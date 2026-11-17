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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CheckoutSagaOrchestratorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @InjectMocks
    private CheckoutSagaOrchestrator orchestrator;

    private CheckoutRequestDTO sampleRequest;
    private CartClientDTO sampleCart;

    @BeforeEach
    public void setUp() {
        sampleRequest = new CheckoutRequestDTO(
                "IDEMP-TEST-001",
                PaymentMethod.WALLET,
                "Alice Customer",
                "+91-9876543210",
                "123 Tech Park, Bangalore"
        );

        CartItemClientDTO cartItem = new CartItemClientDTO(
                1L,
                10L,
                "Wireless Headphones",
                "WH-1000",
                new BigDecimal("600.00"),
                "http://img.com/1.png",
                2,
                new BigDecimal("1200.00"),
                true,
                15
        );
        sampleCart = new CartClientDTO(1L, 1L, Collections.singletonList(cartItem), 2, new BigDecimal("1200.00"));
    }

    @Test
    @DisplayName("Should successfully execute checkout saga with wallet payment")
    public void testSuccessfulWalletCheckout() {
        when(orderRepository.findByIdempotencyKey("IDEMP-TEST-001")).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(sampleCart);

        ProductClientDTO productClientDTO = new ProductClientDTO();
        ProductClientDTO.ProductData productData = new ProductClientDTO.ProductData();
        productData.setId(10L);
        productData.setMerchantId(2L);
        productData.setName("Wireless Headphones");
        productData.setPrice(new BigDecimal("600.00"));
        productClientDTO.setData(productData);
        when(productServiceClient.getProductById(10L)).thenReturn(productClientDTO);

        InventoryReserveClientResponseDTO reserveResponse = new InventoryReserveClientResponseDTO();
        reserveResponse.setSuccess(true);
        when(productServiceClient.reserveStock(any(InventoryReserveClientRequestDTO.class))).thenReturn(reserveResponse);

        WalletTransactionClientResponseDTO payResponse = new WalletTransactionClientResponseDTO();
        payResponse.setSuccess(true);
        WalletTransactionClientResponseDTO.WalletTransactionData payData = new WalletTransactionClientResponseDTO.WalletTransactionData();
        payData.setTransactionReference("TXN-PAY-001");
        payResponse.setData(payData);
        when(walletServiceClient.pay(any(WalletPayClientRequestDTO.class))).thenReturn(payResponse);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token");

        assertNotNull(result);
        assertEquals(OrderStatus.PLACED, result.getOrderStatus());
        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());
        assertEquals(SagaState.COMPLETED, result.getSagaState());
        assertEquals(new BigDecimal("1200.00"), result.getSubtotalAmount());
        assertEquals(new BigDecimal("0.00"), result.getShippingFee()); // Free shipping >= 1000
        assertEquals(new BigDecimal("1200.00"), result.getTotalAmount());
        assertEquals(1, result.getOrderItems().size());
        assertEquals(1, result.getPaymentRecords().size());

        verify(cartServiceClient).clearCart(eq(1L), eq("Bearer token"));
    }

    @Test
    @DisplayName("Should successfully execute checkout saga with Cash On Delivery")
    public void testSuccessfulCodCheckout() {
        sampleRequest.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(sampleCart);

        InventoryReserveClientResponseDTO reserveResponse = new InventoryReserveClientResponseDTO();
        reserveResponse.setSuccess(true);
        when(productServiceClient.reserveStock(any(InventoryReserveClientRequestDTO.class))).thenReturn(reserveResponse);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token");

        assertNotNull(result);
        assertEquals(OrderStatus.PLACED, result.getOrderStatus());
        assertEquals(PaymentStatus.COD_PENDING, result.getPaymentStatus());
        assertEquals(SagaState.COMPLETED, result.getSagaState());
        verify(walletServiceClient, never()).pay(any());
    }

    @Test
    @DisplayName("Should return existing order on idempotent replay")
    public void testIdempotentReplayReturnsExistingOrder() {
        Order existing = new Order(
                "ORD-EXISTING-001",
                "IDEMP-TEST-001",
                1L,
                PaymentMethod.WALLET,
                new BigDecimal("1200.00"),
                BigDecimal.ZERO,
                new BigDecimal("1200.00"),
                "Alice Customer",
                "+91-9876543210",
                "123 Tech Park, Bangalore"
        );
        when(orderRepository.findByIdempotencyKey("IDEMP-TEST-001")).thenReturn(Optional.of(existing));

        Order result = orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token");

        assertSame(existing, result);
        verify(cartServiceClient, never()).getCart(any(), any());
        verify(productServiceClient, never()).reserveStock(any());
        verify(walletServiceClient, never()).pay(any());
    }

    @Test
    @DisplayName("Should reject idempotent replay with different customer ID")
    public void testIdempotencyConflictDifferentCustomer() {
        Order existing = new Order(
                "ORD-EXISTING-001",
                "IDEMP-TEST-001",
                99L, // different customer
                PaymentMethod.WALLET,
                new BigDecimal("1200.00"),
                BigDecimal.ZERO,
                new BigDecimal("1200.00"),
                "Alice Customer",
                "+91-9876543210",
                "123 Tech Park, Bangalore"
        );
        when(orderRepository.findByIdempotencyKey("IDEMP-TEST-001")).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () ->
                orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token")
        );
    }

    @Test
    @DisplayName("Should reject idempotent replay with different payload")
    public void testIdempotencyConflictDifferentPayload() {
        Order existing = new Order(
                "ORD-EXISTING-001",
                "IDEMP-TEST-001",
                1L,
                PaymentMethod.WALLET,
                new BigDecimal("1200.00"),
                BigDecimal.ZERO,
                new BigDecimal("1200.00"),
                "Alice Customer",
                "+91-9876543210",
                "999 Different Address" // mismatch
        );
        when(orderRepository.findByIdempotencyKey("IDEMP-TEST-001")).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () ->
                orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token")
        );
    }

    @Test
    @DisplayName("Should reject checkout with empty cart")
    public void testEmptyCartThrowsBadRequest() {
        sampleCart.setItems(Collections.emptyList());
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(sampleCart);

        assertThrows(BadRequestException.class, () ->
                orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token")
        );
    }

    @Test
    @DisplayName("Should reject checkout when item has insufficient stock")
    public void testInsufficientStockAbortsSagaWithoutCompensation() {
        sampleCart.getItems().get(0).setAvailableStock(1); // Requested is 2
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(sampleCart);

        assertThrows(InsufficientStockException.class, () ->
                orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token")
        );

        verify(productServiceClient, never()).reserveStock(any());
        verify(walletServiceClient, never()).pay(any());
    }

    @Test
    @DisplayName("Should trigger compensating stock release when wallet payment fails")
    public void testWalletPaymentFailureTriggersCompensatingStockRelease() {
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(sampleCart);

        InventoryReserveClientResponseDTO reserveResponse = new InventoryReserveClientResponseDTO();
        reserveResponse.setSuccess(true);
        when(productServiceClient.reserveStock(any(InventoryReserveClientRequestDTO.class))).thenReturn(reserveResponse);

        when(walletServiceClient.pay(any(WalletPayClientRequestDTO.class)))
                .thenThrow(new PaymentFailedException("Insufficient wallet funds"));
        when(walletServiceClient.getPaymentStatusByOrder(anyString())).thenReturn(null);

        assertThrows(PaymentFailedException.class, () ->
                orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token")
        );

        // Verify compensating stock release was called
        verify(productServiceClient).releaseStock(any(InventoryReleaseClientRequestDTO.class));
        // Verify order was NOT saved
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should recover and complete if reconciliation confirms payment on timeout")
    public void testNetworkTimeoutReconcilesWithWalletService() {
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(sampleCart);

        InventoryReserveClientResponseDTO reserveResponse = new InventoryReserveClientResponseDTO();
        reserveResponse.setSuccess(true);
        when(productServiceClient.reserveStock(any(InventoryReserveClientRequestDTO.class))).thenReturn(reserveResponse);

        // Simulate network exception during payment call
        when(walletServiceClient.pay(any(WalletPayClientRequestDTO.class)))
                .thenThrow(new RuntimeException("SocketTimeoutException"));

        // Reconciliation check confirms payment actually succeeded
        WalletPaymentStatusClientResponseDTO statusCheck = new WalletPaymentStatusClientResponseDTO();
        WalletPaymentStatusClientResponseDTO.PaymentStatusData data = new WalletPaymentStatusClientResponseDTO.PaymentStatusData();
        data.setFound(true);
        data.setTransactionReference("TXN-RECONCILED-001");
        statusCheck.setData(data);
        when(walletServiceClient.getPaymentStatusByOrder(anyString())).thenReturn(statusCheck);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orchestrator.executeCheckoutSaga(sampleRequest, 1L, "Bearer token");

        assertNotNull(result);
        assertEquals(OrderStatus.PLACED, result.getOrderStatus());
        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());
        verify(productServiceClient, never()).releaseStock(any());
    }
}

