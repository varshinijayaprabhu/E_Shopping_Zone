package com.eshopping.order.saga;

import com.eshopping.order.client.CartServiceClient;
import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.client.WalletServiceClient;
import com.eshopping.order.dto.CheckoutRequestDTO;
import com.eshopping.order.dto.client.*;
import com.eshopping.order.entity.*;
import com.eshopping.order.exception.BadRequestException;
import com.eshopping.order.exception.InsufficientStockException;
import com.eshopping.order.exception.PaymentFailedException;
import com.eshopping.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Checkout State-Machine Saga Orchestrator.
 * Coordinates inventory reservation, payment deduction, order persistence, and cart clearance
 * across distributed microservices with forward and compensating transactions.
 */
@Component
public class CheckoutSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CheckoutSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;
    private final WalletServiceClient walletServiceClient;

    public CheckoutSagaOrchestrator(OrderRepository orderRepository,
                                    CartServiceClient cartServiceClient,
                                    ProductServiceClient productServiceClient,
                                    WalletServiceClient walletServiceClient) {
        this.orderRepository = orderRepository;
        this.cartServiceClient = cartServiceClient;
        this.productServiceClient = productServiceClient;
        this.walletServiceClient = walletServiceClient;
    }

    /**
     * Executes the checkout state-machine Saga.
     *
     * @param request    customer checkout request payload
     * @param customerId authenticated customer ID
     * @param authHeader customer Authorization header (Bearer token)
     * @return persisted Order entity in COMPLETED state
     */
    public Order executeCheckoutSaga(CheckoutRequestDTO request, Long customerId, String authHeader) {
        // Step 1: Idempotency Check
        Optional<Order> existingOpt = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            Order existing = existingOpt.get();
            if (!existing.getCustomerId().equals(customerId)) {
                throw new BadRequestException("Idempotency conflict: key already belongs to another customer");
            }
            if (existing.getPaymentMethod() != request.getPaymentMethod() ||
                    !existing.getShippingAddress().equals(request.getShippingAddress())) {
                throw new BadRequestException("Idempotency conflict: request already processed with different parameters");
            }
            log.info("Idempotent replay detected for order {}", existing.getOrderNumber());
            return existing;
        }

        // Step 2: Fetch Customer Cart
        CartClientDTO cart = cartServiceClient.getCart(customerId, authHeader);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout with an empty cart");
        }

        // Step 3: Validate Cart Items Stock Availability
        for (CartItemClientDTO item : cart.getItems()) {
            if (!item.isInStock() || item.getAvailableStock() < item.getQuantity()) {
                throw new InsufficientStockException("Product '" + item.getProductName() + "' has insufficient stock (available: " + item.getAvailableStock() + ", requested: " + item.getQuantity() + ")");
            }
        }

        // Step 4: Resolve Merchant IDs and Snapshots from Product Service
        List<OrderItemData> itemDataList = new ArrayList<>();
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        for (CartItemClientDTO item : cart.getItems()) {
            ProductClientDTO productResponse = productServiceClient.getProductById(item.getProductId());
            Long merchantId = 1L; // Fallback
            BigDecimal unitPrice = item.getUnitPrice();
            String productName = item.getProductName();

            if (productResponse != null && productResponse.getData() != null) {
                merchantId = productResponse.getData().getMerchantId();
                if (productResponse.getData().getPrice() != null) {
                    unitPrice = productResponse.getData().getPrice();
                }
                if (productResponse.getData().getName() != null) {
                    productName = productResponse.getData().getName();
                }
            }

            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, BigDecimal.ROUND_HALF_UP);
            calculatedSubtotal = calculatedSubtotal.add(lineSubtotal);

            itemDataList.add(new OrderItemData(item.getProductId(), merchantId, productName, item.getProductSku(), unitPrice, item.getQuantity(), lineSubtotal));
        }

        // Step 5: Calculate Shipping Fee and Total
        // Free shipping on orders over ₹1000, else ₹50
        BigDecimal shippingFee = calculatedSubtotal.compareTo(new BigDecimal("1000.00")) >= 0
                ? BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP)
                : new BigDecimal("50.00").setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalAmount = calculatedSubtotal.add(shippingFee).setScale(2, BigDecimal.ROUND_HALF_UP);

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        log.info("Starting Checkout Saga for order {} (customer {}, total {})", orderNumber, customerId, totalAmount);

        // Step 6: Saga Step A - Reserve Inventory
        List<InventoryReserveClientRequestDTO.ItemReservation> reservations = new ArrayList<>();
        for (OrderItemData itemData : itemDataList) {
            reservations.add(new InventoryReserveClientRequestDTO.ItemReservation(itemData.productId, itemData.quantity));
        }
        InventoryReserveClientRequestDTO reserveRequest = new InventoryReserveClientRequestDTO(orderNumber, reservations);

        InventoryReserveClientResponseDTO reserveResponse;
        try {
            reserveResponse = productServiceClient.reserveStock(reserveRequest);
        } catch (InsufficientStockException ex) {
            log.error("Saga Step A Failed: Inventory reservation failed for order {}: {}", orderNumber, ex.getMessage());
            throw ex;
        }

        boolean stockReserved = (reserveResponse != null && reserveResponse.isSuccess());
        if (!stockReserved) {
            throw new InsufficientStockException("Inventory reservation failed for order " + orderNumber);
        }
        log.info("Saga Step A Succeeded: Stock reserved for order {}", orderNumber);

        // Step 7: Saga Step B - Process Payment
        String paymentGatewayRef = null;
        if (request.getPaymentMethod() == PaymentMethod.WALLET) {
            String payIdempotencyKey = "pay-" + request.getIdempotencyKey();
            WalletPayClientRequestDTO payRequest = new WalletPayClientRequestDTO(
                    customerId,
                    orderNumber,
                    totalAmount,
                    payIdempotencyKey,
                    "Payment for order " + orderNumber
            );

            try {
                WalletTransactionClientResponseDTO payResponse = walletServiceClient.pay(payRequest);
                if (payResponse != null && payResponse.getData() != null) {
                    paymentGatewayRef = payResponse.getData().getTransactionReference();
                } else {
                    paymentGatewayRef = "TXN-WALLET-" + orderNumber;
                }
                log.info("Saga Step B Succeeded: Wallet deducted for order {} (ref: {})", orderNumber, paymentGatewayRef);
            } catch (Exception ex) {
                log.error("Saga Step B Failed: Wallet payment failed for order {}: {}. Initiating stock compensation...",
                        orderNumber, ex.getMessage());

                // Check if payment was actually processed before compensating (network timeout edge case)
                WalletPaymentStatusClientResponseDTO statusCheck = walletServiceClient.getPaymentStatusByOrder(orderNumber);
                if (statusCheck != null && statusCheck.getData() != null && statusCheck.getData().isFound()) {
                    log.info("Reconciliation check found successful payment for order {}", orderNumber);
                    paymentGatewayRef = statusCheck.getData().getTransactionReference();
                } else {
                    // COMPENSATE: Release reserved stock
                    compensateStock(orderNumber, itemDataList);
                    throw new PaymentFailedException("Payment processing failed: " + ex.getMessage());
                }
            }
        } else {
            paymentGatewayRef = "COD-" + orderNumber;
            log.info("Saga Step B: Cash On Delivery selected for order {}", orderNumber);
        }

        // Step 8: Saga Step C - Persist Order & Line Items in Database
        Order order = new Order(
                orderNumber,
                request.getIdempotencyKey(),
                customerId,
                request.getPaymentMethod(),
                calculatedSubtotal,
                shippingFee,
                totalAmount,
                request.getShippingName(),
                request.getShippingPhone(),
                request.getShippingAddress()
        );

        order.setOrderStatus(OrderStatus.PLACED);
        order.setPaymentStatus(request.getPaymentMethod() == PaymentMethod.WALLET ? PaymentStatus.PAID : PaymentStatus.COD_PENDING);
        order.setSagaState(SagaState.ORDER_PERSISTED);

        for (OrderItemData itemData : itemDataList) {
            OrderItem orderItem = new OrderItem(
                    order,
                    itemData.productId,
                    itemData.merchantId,
                    itemData.productName,
                    itemData.unitPrice,
                    itemData.quantity,
                    itemData.subtotal
            );
            order.addOrderItem(orderItem);
        }

        PaymentRecord paymentRecord = new PaymentRecord(
                order,
                "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                request.getPaymentMethod(),
                totalAmount,
                request.getPaymentMethod() == PaymentMethod.WALLET ? "SUCCESS" : "COD_PENDING",
                paymentGatewayRef
        );
        order.addPaymentRecord(paymentRecord);

        Order savedOrder = orderRepository.save(order);
        log.info("Saga Step C Succeeded: Order {} persisted with ID {}", orderNumber, savedOrder.getId());

        // Step 9: Saga Step D - Clear Shopping Cart
        try {
            cartServiceClient.clearCart(customerId, authHeader);
            log.info("Saga Step D Succeeded: Cart cleared for customer {}", customerId);
        } catch (Exception ex) {
            log.warn("Saga Step D Warning: Cart clearance failed for customer {}: {}", customerId, ex.getMessage());
            // Non-critical: do not abort the confirmed order
        }

        // Step 10: Finalize Saga State
        savedOrder.setSagaState(SagaState.COMPLETED);
        savedOrder = orderRepository.save(savedOrder);
        log.info("Checkout Saga COMPLETED for order {}", orderNumber);

        return savedOrder;
    }

    /**
     * Compensates stock reservation by releasing locked inventory.
     */
    private void compensateStock(String orderNumber, List<OrderItemData> itemDataList) {
        try {
            List<InventoryReleaseClientRequestDTO.ItemRelease> releases = new ArrayList<>();
            for (OrderItemData itemData : itemDataList) {
                releases.add(new InventoryReleaseClientRequestDTO.ItemRelease(itemData.productId, itemData.quantity));
            }
            InventoryReleaseClientRequestDTO releaseRequest = new InventoryReleaseClientRequestDTO(orderNumber, releases);
            productServiceClient.releaseStock(releaseRequest);
            log.info("Stock compensation succeeded for order {}", orderNumber);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to compensate stock for order {}: {}", orderNumber, ex.getMessage());
        }
    }

    /**
     * Helper value object for carrying item data across saga stages.
     */
    private static class OrderItemData {
        final Long productId;
        final Long merchantId;
        final String productName;
        final String productSku;
        final BigDecimal unitPrice;
        final int quantity;
        final BigDecimal subtotal;

        OrderItemData(Long productId, Long merchantId, String productName, String productSku,
                      BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
            this.productId = productId;
            this.merchantId = merchantId;
            this.productName = productName;
            this.productSku = productSku;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }
    }
}

