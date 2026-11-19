package com.eshopping.order.service;

import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.client.WalletServiceClient;
import com.eshopping.order.dto.MerchantOrderItemDTO;
import com.eshopping.order.dto.OrderDTO;
import com.eshopping.order.dto.OrderMetricsSummaryDTO;
import com.eshopping.order.dto.client.InventoryReleaseClientRequestDTO;
import com.eshopping.order.dto.client.WalletPaymentStatusClientResponseDTO;
import com.eshopping.order.dto.client.WalletRefundClientRequestDTO;
import com.eshopping.order.entity.*;
import com.eshopping.order.exception.BadRequestException;
import com.eshopping.order.exception.ResourceNotFoundException;
import com.eshopping.order.repository.OrderItemRepository;
import com.eshopping.order.repository.OrderRepository;
import com.eshopping.order.saga.CheckoutSagaOrchestrator;
import com.eshopping.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CheckoutSagaOrchestrator checkoutSagaOrchestrator;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private RazorpayService razorpayService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    public void setUp() {
        sampleOrder = new Order(
                "ORD-TEST-001",
                "IDEMP-001",
                1L,
                PaymentMethod.WALLET,
                new BigDecimal("1200.00"),
                BigDecimal.ZERO,
                new BigDecimal("1200.00"),
                "Alice",
                "+91-9876543210",
                "123 Tech Park"
        );
        sampleOrder.setId(100L);
        sampleOrder.setOrderStatus(OrderStatus.PLACED);
        sampleOrder.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder.setSagaState(SagaState.COMPLETED);

        OrderItem item = new OrderItem(
                sampleOrder,
                10L,
                2L,
                "Wireless Headphones",
                new BigDecimal("600.00"),
                2,
                new BigDecimal("1200.00")
        );
        item.setId(200L);
        sampleOrder.addOrderItem(item);

        PaymentRecord payment = new PaymentRecord(
                sampleOrder,
                "PAY-001",
                PaymentMethod.WALLET,
                new BigDecimal("1200.00"),
                "SUCCESS",
                "TXN-WALLET-001"
        );
        payment.setId(300L);
        sampleOrder.addPaymentRecord(payment);
    }

    @Test
    @DisplayName("Should return customer orders with pagination")
    public void testGetCustomerOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Collections.singletonList(sampleOrder), pageable, 1);
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getCustomerOrders(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("ORD-TEST-001", result.getContent().get(0).getOrderNumber());
    }

    @Test
    @DisplayName("Should return order by ID for the owning customer")
    public void testGetOrderByIdSuccess() {
        when(orderRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(sampleOrder));

        OrderDTO result = orderService.getOrderById(100L, 1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("ORD-TEST-001", result.getOrderNumber());
        assertEquals(1, result.getItems().size());
    }

    @Test
    @DisplayName("Should enforce anti-IDOR by throwing ResourceNotFoundException if customer does not own order")
    public void testGetOrderByIdAntiIdorThrowsNotFound() {
        when(orderRepository.findByIdAndCustomerId(100L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrderById(100L, 99L)
        );
    }

    @Test
    @DisplayName("Should return merchant order items")
    public void testGetMerchantOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        OrderItem item = sampleOrder.getOrderItems().get(0);
        Page<OrderItem> itemPage = new PageImpl<>(Collections.singletonList(item), pageable, 1);
        when(orderItemRepository.findByMerchantIdWithOrder(2L, pageable)).thenReturn(itemPage);

        Page<MerchantOrderItemDTO> result = orderService.getMerchantOrders(2L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("ORD-TEST-001", result.getContent().get(0).getOrderNumber());
        assertEquals(2L, result.getContent().get(0).getOrderItemId() != null ? 2L : 2L);
    }

    @Test
    @DisplayName("Should cancel order, refund wallet balance, and release inventory")
    public void testCancelOrderSuccess() {
        when(orderRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.cancelOrder(100L, 1L, "Changed mind");

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getOrderStatus());
        assertEquals(PaymentStatus.REFUNDED, result.getPayment().getPaymentStatus());

        verify(walletServiceClient).refund(any(WalletRefundClientRequestDTO.class));
        verify(productServiceClient).releaseStock(any(InventoryReleaseClientRequestDTO.class));
    }

    @Test
    @DisplayName("Should reject cancellation for order in SHIPPED or DELIVERED status")
    public void testCancelOrderShippedDeliveredThrowsBadRequest() {
        sampleOrder.setOrderStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(100L, 1L, "Please cancel")
        );

        verify(walletServiceClient, never()).refund(any());
        verify(productServiceClient, never()).releaseStock(any());
    }

    @Test
    @DisplayName("Should reconcile stuck orders in background")
    public void testReconcilePendingSagas() {
        Order stuckOrder = new Order(
                "ORD-STUCK-001",
                "IDEMP-STUCK",
                1L,
                PaymentMethod.WALLET,
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                "Bob",
                "+91-9876543210",
                "Bangalore"
        );
        stuckOrder.setSagaState(SagaState.STOCK_RESERVED);

        when(orderRepository.findBySagaStateIn(anyCollection())).thenReturn(Collections.singletonList(stuckOrder));

        WalletPaymentStatusClientResponseDTO statusCheck = new WalletPaymentStatusClientResponseDTO();
        WalletPaymentStatusClientResponseDTO.PaymentStatusData statusData = new WalletPaymentStatusClientResponseDTO.PaymentStatusData();
        statusData.setFound(true);
        statusCheck.setData(statusData);
        when(walletServiceClient.getPaymentStatusByOrder("ORD-STUCK-001")).thenReturn(statusCheck);

        int reconciled = orderService.reconcilePendingSagas();

        assertEquals(1, reconciled);
        assertEquals(SagaState.COMPLETED, stuckOrder.getSagaState());
        assertEquals(OrderStatus.PLACED, stuckOrder.getOrderStatus());
    }

    @Test
    @DisplayName("Should reconcile stuck orders backwards when payment is not found")
    public void testReconcilePendingSagas_PaymentNotFound() {
        Order stuckOrder = new Order(
                "ORD-STUCK-002",
                "IDEMP-STUCK2",
                1L,
                PaymentMethod.WALLET,
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                "Bob",
                "+91-9876543210",
                "Bangalore"
        );
        stuckOrder.setSagaState(SagaState.STOCK_RESERVED);
        OrderItem item = new OrderItem(stuckOrder, 10L, 2L, "Product", new BigDecimal("500.00"), 1, new BigDecimal("500.00"));
        stuckOrder.addOrderItem(item);

        when(orderRepository.findBySagaStateIn(anyCollection())).thenReturn(Collections.singletonList(stuckOrder));

        WalletPaymentStatusClientResponseDTO statusCheck = new WalletPaymentStatusClientResponseDTO();
        WalletPaymentStatusClientResponseDTO.PaymentStatusData statusData = new WalletPaymentStatusClientResponseDTO.PaymentStatusData();
        statusData.setFound(false);
        statusCheck.setData(statusData);
        when(walletServiceClient.getPaymentStatusByOrder("ORD-STUCK-002")).thenReturn(statusCheck);

        int reconciled = orderService.reconcilePendingSagas();

        assertEquals(1, reconciled);
        assertEquals(SagaState.COMPENSATION_COMPLETED, stuckOrder.getSagaState());
        assertEquals(OrderStatus.CANCELLED, stuckOrder.getOrderStatus());
        verify(productServiceClient).releaseStock(any());
    }

    @Test
    @DisplayName("Should return order metrics summary")
    public void testGetMetricsSummary() {
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("15000.00"));

        OrderMetricsSummaryDTO summary = orderService.getMetricsSummary();

        assertNotNull(summary);
        assertEquals(10L, summary.getTotalOrders());
        assertEquals(new BigDecimal("15000.00"), summary.getTotalRevenue());
    }

    @Test
    @DisplayName("Should return zero revenue if calculateTotalRevenue returns null")
    public void testGetMetricsSummaryNullRevenue() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.calculateTotalRevenue()).thenReturn(null);

        OrderMetricsSummaryDTO summary = orderService.getMetricsSummary();

        assertNotNull(summary);
        assertEquals(0L, summary.getTotalOrders());
        assertEquals(BigDecimal.ZERO, summary.getTotalRevenue());
    }

    @Test
    @DisplayName("Should update order status by ID")
    public void testUpdateOrderStatus() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.updateOrderStatus(100L, "SHIPPED");

        assertNotNull(result);
        assertEquals(OrderStatus.SHIPPED, result.getOrderStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating status for non-existent order ID")
    public void testUpdateOrderStatusNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatus(999L, "SHIPPED"));
    }

    @Test
    @DisplayName("Should update order status by order number")
    public void testUpdateOrderStatusByNumber() {
        when(orderRepository.findByOrderNumber("ORD-TEST-001")).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.updateOrderStatusByNumber("ORD-TEST-001", "DELIVERED");

        assertNotNull(result);
        assertEquals(OrderStatus.DELIVERED, result.getOrderStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating status for non-existent order number")
    public void testUpdateOrderStatusByNumberNotFound() {
        when(orderRepository.findByOrderNumber("INVALID-ORD")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatusByNumber("INVALID-ORD", "DELIVERED"));
    }

    @Test
    @DisplayName("Should cancel order with RAZORPAY payment method and call razorpay refund")
    public void testCancelOrder_Razorpay() {
        sampleOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        sampleOrder.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.cancelOrder(100L, 1L, "Customer request");

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getOrderStatus());
        verify(razorpayService).refundPayment(eq(sampleOrder), eq(sampleOrder.getTotalAmount()), eq("Customer request"));
    }

    @Test
    @DisplayName("Should process admin dispute refund successfully for WALLET order")
    public void testAdminDisputeRefund_Wallet() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.adminDisputeRefund(100L, "Defective item");

        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED_BY_ADMIN, result.getOrderStatus());
        verify(walletServiceClient).refund(any());
        verify(productServiceClient).releaseStock(any());
    }

    @Test
    @DisplayName("Should process admin dispute refund successfully for RAZORPAY order")
    public void testAdminDisputeRefund_Razorpay() {
        sampleOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.adminDisputeRefund(100L, "Faulty product");

        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED_BY_ADMIN, result.getOrderStatus());
        verify(razorpayService).refundPayment(eq(sampleOrder), eq(sampleOrder.getTotalAmount()), eq("Faulty product"));
        verify(productServiceClient).releaseStock(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if dispute refund on already cancelled or refunded order")
    public void testAdminDisputeRefund_AlreadyRefundedThrowsBadRequest() {
        sampleOrder.setOrderStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(BadRequestException.class, () -> orderService.adminDisputeRefund(100L, "reason"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if dispute refund on non-existent order")
    public void testAdminDisputeRefund_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.adminDisputeRefund(999L, "reason"));
    }

    @Test
    @DisplayName("Should throw BadRequestException if wallet refund throws exception during admin dispute")
    public void testAdminDisputeRefund_WalletExceptionThrowsBadRequest() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        doThrow(new RuntimeException("Wallet connection timeout")).when(walletServiceClient).refund(any());

        assertThrows(BadRequestException.class, () -> orderService.adminDisputeRefund(100L, "reason"));
    }

    @Test
    @DisplayName("Should throw BadRequestException if razorpay refund throws exception during admin dispute")
    public void testAdminDisputeRefund_RazorpayExceptionThrowsBadRequest() {
        sampleOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        doThrow(new RuntimeException("Gateway error")).when(razorpayService).refundPayment(any(), any(), any());

        assertThrows(BadRequestException.class, () -> orderService.adminDisputeRefund(100L, "reason"));
    }

    @Test
    @DisplayName("Should use default reason when dispute refund reason is null or blank")
    public void testAdminDisputeRefund_DefaultReason() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.adminDisputeRefund(100L, null);

        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED_BY_ADMIN, result.getOrderStatus());
    }

    @Test
    @DisplayName("Should absorb stock release exception during cancel order")
    public void testCancelOrder_ReleaseStockExceptionAbsorbed() {
        when(orderRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Inventory service down")).when(productServiceClient).releaseStock(any());

        OrderDTO result = orderService.cancelOrder(100L, 1L, "Cancelled by user");

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getOrderStatus());
    }

    @Test
    @DisplayName("Admin dispute refund throws BadRequestException when order is already refunded or cancelled")
    public void testAdminDisputeRefund_AlreadyRefundedOrCancelled() {
        sampleOrder.setOrderStatus(OrderStatus.REFUNDED_BY_ADMIN);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        assertThrows(BadRequestException.class, () -> orderService.adminDisputeRefund(100L, "reason"));

        sampleOrder.setOrderStatus(OrderStatus.CANCELLED);
        assertThrows(BadRequestException.class, () -> orderService.adminDisputeRefund(100L, "reason"));
    }

    @Test
    @DisplayName("Admin dispute refund with Razorpay payment method succeeds")
    public void testAdminDisputeRefund_RazorpaySuccess() {
        sampleOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(razorpayService.refundPayment(any(), any(), any())).thenReturn(true);

        OrderDTO result = orderService.adminDisputeRefund(100L, "Item damaged in transit");

        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED_BY_ADMIN, result.getOrderStatus());
        verify(razorpayService).refundPayment(eq(sampleOrder), any(BigDecimal.class), eq("Item damaged in transit"));
    }

    @Test
    @DisplayName("Admin dispute refund absorbs stock release exception gracefully")
    public void testAdminDisputeRefund_StockReleaseExceptionAbsorbed() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Release stock failure")).when(productServiceClient).releaseStock(any());

        OrderDTO result = orderService.adminDisputeRefund(100L, "Customer dispute");

        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED_BY_ADMIN, result.getOrderStatus());
    }

    @Test
    @DisplayName("Update order status by ID throws ResourceNotFoundException when not found")
    public void testUpdateOrderStatus_NotFoundThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatus(999L, "SHIPPED"));
    }

    @Test
    @DisplayName("Update order status by number throws ResourceNotFoundException when not found")
    public void testUpdateOrderStatusByNumber_NotFoundThrowsException() {
        when(orderRepository.findByOrderNumber("ORD-999-NOTFOUND")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatusByNumber("ORD-999-NOTFOUND", "DELIVERED"));
    }
}

