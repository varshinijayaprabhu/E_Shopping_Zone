package com.eshopping.order.service.impl;

import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.client.WalletServiceClient;
import com.eshopping.order.dto.*;
import com.eshopping.order.dto.client.InventoryReleaseClientRequestDTO;
import com.eshopping.order.dto.client.WalletRefundClientRequestDTO;
import com.eshopping.order.entity.*;
import com.eshopping.order.exception.BadRequestException;
import com.eshopping.order.exception.ResourceNotFoundException;
import com.eshopping.order.repository.OrderItemRepository;
import com.eshopping.order.repository.OrderRepository;
import com.eshopping.order.saga.CheckoutSagaOrchestrator;
import com.eshopping.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CheckoutSagaOrchestrator checkoutSagaOrchestrator;
    private final ProductServiceClient productServiceClient;
    private final WalletServiceClient walletServiceClient;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            CheckoutSagaOrchestrator checkoutSagaOrchestrator,
                            ProductServiceClient productServiceClient,
                            WalletServiceClient walletServiceClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.checkoutSagaOrchestrator = checkoutSagaOrchestrator;
        this.productServiceClient = productServiceClient;
        this.walletServiceClient = walletServiceClient;
    }

    @Override
    @Transactional
    public OrderDTO checkout(CheckoutRequestDTO request, Long customerId, String authHeader) {
        Order savedOrder = checkoutSagaOrchestrator.executeCheckoutSaga(request, customerId, authHeader);
        return mapToOrderDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToOrderDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " not found for authenticated customer"));
        return mapToOrderDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order with number " + orderNumber + " not found"));
        return mapToOrderDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MerchantOrderItemDTO> getMerchantOrders(Long merchantId, Pageable pageable) {
        return orderItemRepository.findByMerchantIdWithOrder(merchantId, pageable)
                .map(this::mapToMerchantOrderItemDTO);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long orderId, Long customerId, String reason) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " not found for customer"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.info("Order {} is already cancelled", order.getOrderNumber());
            return mapToOrderDTO(order);
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order in status '" + order.getOrderStatus() + "' cannot be cancelled. Please contact customer support for returns.");
        }

        log.info("Initiating cancellation saga for order {} (customer: {})", order.getOrderNumber(), customerId);

        // Step 1: Refund payment if paid through Wallet
        if (order.getPaymentMethod() == PaymentMethod.WALLET && order.getPaymentStatus() == PaymentStatus.PAID) {
            String refIdempotencyKey = "ref-" + order.getOrderNumber();
            String originalTxnRef = null;
            if (!order.getPaymentRecords().isEmpty()) {
                originalTxnRef = order.getPaymentRecords().iterator().next().getGatewayResponse();
            }

            WalletRefundClientRequestDTO refundRequest = new WalletRefundClientRequestDTO(
                    customerId,
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    refIdempotencyKey,
                    reason != null ? reason : "Order cancelled by customer",
                    originalTxnRef
            );

            try {
                walletServiceClient.refund(refundRequest);
                order.setPaymentStatus(PaymentStatus.REFUNDED);

                PaymentRecord refundRecord = new PaymentRecord(
                        order,
                        "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                        PaymentMethod.WALLET,
                        order.getTotalAmount(),
                        "REFUNDED",
                        "REF-" + order.getOrderNumber()
                );
                order.addPaymentRecord(refundRecord);
                log.info("Compensating refund issued for cancelled order {}", order.getOrderNumber());
            } catch (Exception ex) {
                log.error("Failed to refund wallet for cancelled order {}: {}", order.getOrderNumber(), ex.getMessage());
            }
        }

        // Step 2: Release stock in Product Service
        try {
            List<InventoryReleaseClientRequestDTO.ItemRelease> releases = new ArrayList<>();
            for (OrderItem item : order.getOrderItems()) {
                releases.add(new InventoryReleaseClientRequestDTO.ItemRelease(item.getProductId(), item.getQuantity()));
            }
            InventoryReleaseClientRequestDTO releaseRequest = new InventoryReleaseClientRequestDTO(order.getOrderNumber(), releases);
            productServiceClient.releaseStock(releaseRequest);
            log.info("Stock release executed for cancelled order {}", order.getOrderNumber());
        } catch (Exception ex) {
            log.error("Failed to release stock for cancelled order {}: {}", order.getOrderNumber(), ex.getMessage());
        }

        // Step 3: Update Order Status
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setSagaFailureReason("Cancelled by customer: " + (reason != null ? reason : "No reason provided"));
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} successfully cancelled", order.getOrderNumber());
        return mapToOrderDTO(updatedOrder);
    }

    @Override
    @Transactional
    public int reconcilePendingSagas() {
        List<SagaState> pendingStates = Arrays.asList(
                SagaState.STARTED,
                SagaState.STOCK_RESERVED,
                SagaState.COMPENSATING_STOCK,
                SagaState.COMPENSATING_PAYMENT
        );

        List<Order> stuckOrders = orderRepository.findBySagaStateIn(pendingStates);
        int reconciledCount = 0;

        for (Order order : stuckOrders) {
            log.info("Reconciling stuck order {} with state {}", order.getOrderNumber(), order.getSagaState());
            try {
                if (order.getSagaState() == SagaState.STOCK_RESERVED) {
                    // Check if payment was deducted
                    com.eshopping.order.dto.client.WalletPaymentStatusClientResponseDTO statusCheck =
                            walletServiceClient.getPaymentStatusByOrder(order.getOrderNumber());

                    if (statusCheck != null && statusCheck.getData() != null && statusCheck.getData().isFound()) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        order.setOrderStatus(OrderStatus.PLACED);
                        order.setSagaState(SagaState.COMPLETED);
                        orderRepository.save(order);
                        log.info("Reconciled order {} forward to COMPLETED", order.getOrderNumber());
                    } else {
                        // Compensate stock
                        List<InventoryReleaseClientRequestDTO.ItemRelease> releases = new ArrayList<>();
                        for (OrderItem item : order.getOrderItems()) {
                            releases.add(new InventoryReleaseClientRequestDTO.ItemRelease(item.getProductId(), item.getQuantity()));
                        }
                        productServiceClient.releaseStock(new InventoryReleaseClientRequestDTO(order.getOrderNumber(), releases));
                        order.setOrderStatus(OrderStatus.CANCELLED);
                        order.setSagaState(SagaState.COMPENSATION_COMPLETED);
                        order.setSagaFailureReason("Reconciled after payment timeout");
                        orderRepository.save(order);
                        log.info("Reconciled order {} backward to COMPENSATION_COMPLETED", order.getOrderNumber());
                    }
                    reconciledCount++;
                } else if (order.getSagaState() == SagaState.COMPENSATING_STOCK) {
                    List<InventoryReleaseClientRequestDTO.ItemRelease> releases = new ArrayList<>();
                    for (OrderItem item : order.getOrderItems()) {
                        releases.add(new InventoryReleaseClientRequestDTO.ItemRelease(item.getProductId(), item.getQuantity()));
                    }
                    productServiceClient.releaseStock(new InventoryReleaseClientRequestDTO(order.getOrderNumber(), releases));
                    order.setSagaState(SagaState.COMPENSATION_COMPLETED);
                    orderRepository.save(order);
                    reconciledCount++;
                }
            } catch (Exception ex) {
                log.error("Failed to reconcile order {}: {}", order.getOrderNumber(), ex.getMessage());
            }
        }

        return reconciledCount;
    }

    private OrderDTO mapToOrderDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProductId(),
                        item.getProductNameSnapshot(),
                        null,
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getSubtotal(),
                        item.getMerchantId()
                ))
                .collect(Collectors.toList());

        PaymentRecordDTO paymentDTO = null;
        if (!order.getPaymentRecords().isEmpty()) {
            PaymentRecord pr = order.getPaymentRecords().iterator().next();
            paymentDTO = new PaymentRecordDTO(
                    pr.getId(),
                    pr.getPaymentReference(),
                    pr.getPaymentMethod(),
                    order.getPaymentStatus(),
                    pr.getAmount(),
                    null,
                    pr.getCreatedAt(),
                    null
            );
        }

        return new OrderDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getOrderStatus(),
                order.getSagaState(),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getSagaFailureReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDTOs,
                paymentDTO
        );
    }

    private MerchantOrderItemDTO mapToMerchantOrderItemDTO(OrderItem item) {
        Order o = item.getOrder();
        MerchantOrderItemDTO dto = new MerchantOrderItemDTO(
                item.getId(),
                o.getOrderNumber(),
                o.getOrderStatus(),
                o.getCreatedAt(),
                item.getProductId(),
                item.getProductNameSnapshot(),
                null,
                item.getUnitPriceSnapshot(),
                item.getQuantity(),
                item.getSubtotal(),
                o.getShippingName(),
                o.getShippingPhone(),
                o.getShippingAddress()
        );
        dto.setOrderId(o.getId());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderMetricsSummaryDTO getMetricsSummary() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        return new OrderMetricsSummaryDTO(totalOrders, totalRevenue);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setOrderStatus(OrderStatus.valueOf(status.toUpperCase()));
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderNumber(), status);
        return mapToOrderDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatusByNumber(String orderNumber, String status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        order.setOrderStatus(OrderStatus.valueOf(status.toUpperCase()));
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderNumber, status);
        return mapToOrderDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO adminDisputeRefund(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.REFUNDED_BY_ADMIN || order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already refunded or cancelled");
        }

        String refundReason = (reason != null && !reason.trim().isEmpty())
                ? reason
                : "Dispute refund issued by Platform Administrator";

        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            String refIdempotencyKey = "ADM-REF-IDEM-" + UUID.randomUUID().toString();
            String originalTxnRef = null;
            if (!order.getPaymentRecords().isEmpty()) {
                originalTxnRef = order.getPaymentRecords().iterator().next().getPaymentReference();
            }

            WalletRefundClientRequestDTO refundRequest = new WalletRefundClientRequestDTO(
                    order.getCustomerId(),
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    refIdempotencyKey,
                    refundReason,
                    originalTxnRef
            );

            try {
                walletServiceClient.refund(refundRequest);
                order.setPaymentStatus(PaymentStatus.REFUNDED);

                PaymentRecord refundRecord = new PaymentRecord(
                        order,
                        "ADM-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                        PaymentMethod.WALLET,
                        order.getTotalAmount(),
                        "REFUNDED",
                        "Admin dispute refund: " + refundReason
                );
                order.addPaymentRecord(refundRecord);
                log.info("Dispute refund issued to customer {} for order {}", order.getCustomerId(), order.getOrderNumber());
            } catch (Exception ex) {
                log.error("Failed to execute wallet refund for dispute on order {}: {}", order.getOrderNumber(), ex.getMessage());
                throw new BadRequestException("Wallet refund failed: " + ex.getMessage());
            }
        }

        try {
            List<InventoryReleaseClientRequestDTO.ItemRelease> releases = new ArrayList<>();
            for (OrderItem item : order.getOrderItems()) {
                releases.add(new InventoryReleaseClientRequestDTO.ItemRelease(item.getProductId(), item.getQuantity()));
            }
            InventoryReleaseClientRequestDTO releaseRequest = new InventoryReleaseClientRequestDTO(order.getOrderNumber(), releases);
            productServiceClient.releaseStock(releaseRequest);
            log.info("Stock released for admin dispute refund on order {}", order.getOrderNumber());
        } catch (Exception ex) {
            log.warn("Failed to release stock during dispute refund on order {}: {}", order.getOrderNumber(), ex.getMessage());
        }

        order.setOrderStatus(OrderStatus.REFUNDED_BY_ADMIN);
        order.setSagaState(SagaState.COMPENSATION_COMPLETED);
        order.setSagaFailureReason(refundReason);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} transitioned to REFUNDED_BY_ADMIN by administrator", order.getOrderNumber());
        return mapToOrderDTO(updatedOrder);
    }
}

