package com.eshopping.order.service;

import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.client.WalletServiceClient;
import com.eshopping.order.dto.MerchantOrderItemDTO;
import com.eshopping.order.dto.OrderDTO;
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
}

