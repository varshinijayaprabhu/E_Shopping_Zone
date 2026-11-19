package com.eshopping.order.service;

import com.eshopping.order.client.CartServiceClient;
import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.dto.*;
import com.eshopping.order.dto.client.*;
import com.eshopping.order.entity.*;
import com.eshopping.order.exception.BadRequestException;
import com.eshopping.order.exception.InsufficientStockException;
import com.eshopping.order.exception.PaymentFailedException;
import com.eshopping.order.exception.PaymentGatewayException;
import com.eshopping.order.repository.OrderRepository;
import com.eshopping.order.repository.PaymentRecordRepository;
import com.eshopping.order.service.impl.OrderServiceImpl;
import com.eshopping.order.service.impl.RazorpayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RazorpayServiceTest {

    private static final String TEST_KEY_ID = "rzp_test_orderKey123";
    private static final String TEST_KEY_SECRET = "secret_order_test_mockKeySecret789";

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private RestTemplate restTemplate;

    private RazorpayServiceImpl razorpayService;

    @BeforeEach
    void setUp() {
        razorpayService = new RazorpayServiceImpl(
                cartServiceClient,
                productServiceClient,
                orderRepository,
                paymentRecordRepository,
                orderService
        );
        razorpayService.setKeyId(TEST_KEY_ID);
        razorpayService.setKeySecret(TEST_KEY_SECRET);
        razorpayService.setCurrency("INR");
        ReflectionTestUtils.setField(razorpayService, "restTemplate", restTemplate);
    }

    private CartItemClientDTO createCartItem(Long productId, String name, int quantity, BigDecimal unitPrice) {
        CartItemClientDTO item = new CartItemClientDTO();
        item.setItemId(100L);
        item.setProductId(productId);
        item.setProductName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setItemSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setInStock(true);
        item.setAvailableStock(100);
        return item;
    }

    private CartClientDTO createCart(Long customerId, List<CartItemClientDTO> items) {
        CartClientDTO cart = new CartClientDTO();
        cart.setCartId(1L);
        cart.setCustomerId(customerId);
        cart.setItems(items);
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQty = 0;
        for (CartItemClientDTO item : items) {
            subtotal = subtotal.add(item.getItemSubtotal());
            totalQty += item.getQuantity();
        }
        cart.setCartSubtotal(subtotal);
        cart.setTotalItems(totalQty);
        return cart;
    }

    private ProductClientDTO createProductResponse(Long productId, BigDecimal price, int stock, Long merchantId) {
        ProductClientDTO.ProductData data = new ProductClientDTO.ProductData();
        data.setId(productId);
        data.setPrice(price);
        data.setAvailableStock(stock);
        data.setMerchantId(merchantId);
        ProductClientDTO response = new ProductClientDTO();
        response.setSuccess(true);
        response.setMessage("Found");
        response.setData(data);
        return response;
    }

    @Test
    @DisplayName("Create checkout Razorpay order calculates cart totals and paise")
    void testCreateOrder_Success() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayCreateOrderRequestDTO request = new RazorpayCreateOrderRequestDTO(
                "Alice", "9876543210", "123 Street", "IDEMP-001"
        );

        CartItemClientDTO item = createCartItem(10L, "Smart Watch", 1, new BigDecimal("1200.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("1200.00"), 15, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        RazorpayCreateOrderResponseDTO response = razorpayService.createOrder(customerId, request, authHeader);

        assertNotNull(response);
        assertNotNull(response.getRazorpayOrderId());
        assertNotNull(response.getOrderNumber());
        assertTrue(response.getOrderNumber().startsWith("ORD-"));
        assertEquals(new BigDecimal("1200.00"), response.getAmount());
        assertEquals(120000L, response.getAmountInPaise());
        assertEquals("INR", response.getCurrency());
        assertEquals(TEST_KEY_ID, response.getKeyId());
    }

    @Test
    @DisplayName("Create order with empty cart throws BadRequestException")
    void testCreateOrder_EmptyCart_ThrowsBadRequestException() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayCreateOrderRequestDTO request = new RazorpayCreateOrderRequestDTO(
                "Alice", "9876543210", "123 Street", "IDEMP-001"
        );

        CartClientDTO emptyCart = createCart(customerId, Collections.emptyList());
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(emptyCart);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                razorpayService.createOrder(customerId, request, authHeader));
        assertTrue(ex.getMessage().contains("Cannot create payment order with an empty cart"));
    }

    @Test
    @DisplayName("Create order with insufficient stock throws InsufficientStockException")
    void testCreateOrder_InsufficientStock_ThrowsException() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayCreateOrderRequestDTO request = new RazorpayCreateOrderRequestDTO(
                "Alice", "9876543210", "123 Street", "IDEMP-001"
        );

        CartItemClientDTO item = createCartItem(10L, "Smart Watch", 5, new BigDecimal("1200.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("1200.00"), 2, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () ->
                razorpayService.createOrder(customerId, request, authHeader));
        assertTrue(ex.getMessage().contains("Insufficient stock for product 'Smart Watch'"));
    }

    @Test
    @DisplayName("Verify valid HMAC-SHA256 signature completes checkout saga")
    void testVerifyAndCompletePayment_ValidSignature_CompletesSaga() throws Exception {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        String orderId = "order_live_checkout999";
        String paymentId = "pay_live_txn888";
        String payload = orderId + "|" + paymentId;
        String validSignature = calculateHmacSha256(payload, TEST_KEY_SECRET);

        RazorpayVerifyPaymentRequestDTO request = new RazorpayVerifyPaymentRequestDTO(
                orderId, paymentId, validSignature, "ORD-TEST-12345",
                "Alice", "9876543210", "123 Street", "IDEMP-KEY-999"
        );

        when(orderRepository.findByIdempotencyKey("IDEMP-KEY-999")).thenReturn(Optional.empty());
        when(paymentRecordRepository.findByPaymentReference(paymentId)).thenReturn(Optional.empty());

        CartItemClientDTO item = createCartItem(10L, "Smart Watch", 1, new BigDecimal("500.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("500.00"), 10, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        InventoryReserveClientResponseDTO reserveResponse = new InventoryReserveClientResponseDTO();
        reserveResponse.setSuccess(true);
        reserveResponse.setMessage("Reserved");
        when(productServiceClient.reserveStock(any(InventoryReserveClientRequestDTO.class))).thenReturn(reserveResponse);

        Order savedOrder = new Order(
                "ORD-TEST-12345", "IDEMP-KEY-999", customerId, PaymentMethod.RAZORPAY,
                new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("550.00"),
                "Alice", "9876543210", "123 Street"
        );
        savedOrder.setId(100L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDTO expectedDto = new OrderDTO();
        expectedDto.setId(100L);
        expectedDto.setOrderNumber("ORD-TEST-12345");
        expectedDto.setOrderStatus(OrderStatus.PLACED);
        when(orderService.mapToOrderDTO(any(Order.class))).thenReturn(expectedDto);

        OrderDTO result = razorpayService.verifyAndCompletePayment(customerId, request, authHeader);

        assertNotNull(result);
        assertEquals("ORD-TEST-12345", result.getOrderNumber());
        assertEquals(OrderStatus.PLACED, result.getOrderStatus());

        verify(productServiceClient, times(1)).reserveStock(any(InventoryReserveClientRequestDTO.class));
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartServiceClient, times(1)).clearCart(customerId, authHeader);
    }

    @Test
    @DisplayName("Verify tampered signature throws PaymentGatewayException and does not place order")
    void testVerifyAndCompletePayment_TamperedSignature_ThrowsException() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayVerifyPaymentRequestDTO request = new RazorpayVerifyPaymentRequestDTO(
                "order_live_999", "pay_live_888", "tampered_fake_signature_hash",
                "ORD-TEST-12345", "Alice", "9876543210", "123 Street", "IDEMP-001"
        );

        when(orderRepository.findByIdempotencyKey("IDEMP-001")).thenReturn(Optional.empty());
        when(paymentRecordRepository.findByPaymentReference("pay_live_888")).thenReturn(Optional.empty());

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                razorpayService.verifyAndCompletePayment(customerId, request, authHeader));

        assertEquals("PAYMENT_SIGNATURE_MISMATCH", ex.getErrorCode());

        verify(productServiceClient, never()).reserveStock(any());
        verify(orderRepository, never()).save(any());
        verify(cartServiceClient, never()).clearCart(any(), any());
    }

    @Test
    @DisplayName("Verify stock reservation failure throws PaymentFailedException")
    void testVerifyAndCompletePayment_StockReservationFails_ThrowsException() throws Exception {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        String orderId = "order_live_checkout999";
        String paymentId = "pay_live_txn888";
        String validSignature = calculateHmacSha256(orderId + "|" + paymentId, TEST_KEY_SECRET);

        RazorpayVerifyPaymentRequestDTO request = new RazorpayVerifyPaymentRequestDTO(
                orderId, paymentId, validSignature, "ORD-TEST-12345",
                "Alice", "9876543210", "123 Street", "IDEMP-KEY-999"
        );

        when(orderRepository.findByIdempotencyKey("IDEMP-KEY-999")).thenReturn(Optional.empty());
        when(paymentRecordRepository.findByPaymentReference(paymentId)).thenReturn(Optional.empty());

        CartItemClientDTO item = createCartItem(10L, "Smart Watch", 1, new BigDecimal("500.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("500.00"), 10, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        InventoryReserveClientResponseDTO rejectResponse = new InventoryReserveClientResponseDTO();
        rejectResponse.setSuccess(false);
        rejectResponse.setMessage("Failed to reserve");
        when(productServiceClient.reserveStock(any(InventoryReserveClientRequestDTO.class))).thenReturn(rejectResponse);

        PaymentFailedException ex = assertThrows(PaymentFailedException.class, () ->
                razorpayService.verifyAndCompletePayment(customerId, request, authHeader));

        assertTrue(ex.getMessage().contains("Stock reservation rejected"));
        verify(orderRepository, never()).save(any());
        verify(cartServiceClient, never()).clearCart(any(), any());
    }

    @Test
    @DisplayName("Verify idempotent replay returns existing order without re-processing")
    void testVerifyAndCompletePayment_IdempotentReplay_ReturnsExistingOrder() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayVerifyPaymentRequestDTO request = new RazorpayVerifyPaymentRequestDTO(
                "order_live_999", "pay_live_888", "any_sig",
                "ORD-EXISTING-001", "Alice", "9876543210", "123 Street", "IDEMP-EXISTING"
        );

        Order existingOrder = new Order();
        existingOrder.setId(42L);
        existingOrder.setOrderNumber("ORD-EXISTING-001");
        when(orderRepository.findByIdempotencyKey("IDEMP-EXISTING")).thenReturn(Optional.of(existingOrder));

        OrderDTO expectedDto = new OrderDTO();
        expectedDto.setId(42L);
        expectedDto.setOrderNumber("ORD-EXISTING-001");
        when(orderService.mapToOrderDTO(existingOrder)).thenReturn(expectedDto);

        OrderDTO result = razorpayService.verifyAndCompletePayment(customerId, request, authHeader);

        assertNotNull(result);
        assertEquals("ORD-EXISTING-001", result.getOrderNumber());

        verify(productServiceClient, never()).reserveStock(any());
        verify(orderRepository, never()).save(any());
        verify(cartServiceClient, never()).clearCart(any(), any());
    }

    @Test
    @DisplayName("Create checkout order with live Razorpay 200 response parses remote order ID")
    void testCreateOrder_LiveRazorpayApiSuccess() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayCreateOrderRequestDTO request = new RazorpayCreateOrderRequestDTO("Bob", "9876543210", "123 Street", "IDEMP-002");

        CartItemClientDTO item = createCartItem(10L, "Keyboard", 1, new BigDecimal("1500.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("1500.00"), 10, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        String jsonResponse = "{\"id\":\"order_rzp_live_9999\"}";
        when(restTemplate.postForEntity(eq("https://api.razorpay.com/v1/orders"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        RazorpayCreateOrderResponseDTO response = razorpayService.createOrder(customerId, request, authHeader);

        assertNotNull(response);
        assertEquals("order_rzp_live_9999", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("1500.00"), response.getAmount());
    }

    @Test
    @DisplayName("Create checkout order with subtotal under 1000 applies 50 shipping fee")
    void testCreateOrder_SubtotalUnder1000_AppliesShippingFee() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayCreateOrderRequestDTO request = new RazorpayCreateOrderRequestDTO("Bob", "9876543210", "123 Street", "IDEMP-003");

        CartItemClientDTO item = createCartItem(10L, "Cable", 1, new BigDecimal("300.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("300.00"), 10, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        razorpayService.setKeyId("rzp_test_placeholder");

        RazorpayCreateOrderResponseDTO response = razorpayService.createOrder(customerId, request, authHeader);

        assertNotNull(response);
        assertEquals(new BigDecimal("350.00"), response.getAmount());
        assertEquals(35000L, response.getAmountInPaise());
        assertTrue(response.getRazorpayOrderId().startsWith("order_sim_"));
    }

    @Test
    @DisplayName("Create order when product is not found in catalog throws BadRequestException")
    void testCreateOrder_ProductNotFound_ThrowsBadRequest() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayCreateOrderRequestDTO request = new RazorpayCreateOrderRequestDTO("Bob", "9876543210", "123 Street", "IDEMP-004");

        CartItemClientDTO item = createCartItem(99L, "Unknown Item", 1, new BigDecimal("50.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);
        when(productServiceClient.getProductById(99L)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> razorpayService.createOrder(customerId, request, authHeader));
    }

    @Test
    @DisplayName("Verify and complete payment with simulated order and signature prefixes succeeds")
    void testVerifyAndCompletePayment_MockSignatures() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(paymentRecordRepository.findByPaymentReference(any())).thenReturn(Optional.empty());

        CartItemClientDTO item = createCartItem(10L, "Mouse", 1, new BigDecimal("800.00"));
        CartClientDTO cart = createCart(customerId, Collections.singletonList(item));
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(cart);

        ProductClientDTO productResponse = createProductResponse(10L, new BigDecimal("800.00"), 10, 11L);
        when(productServiceClient.getProductById(10L)).thenReturn(productResponse);

        InventoryReserveClientResponseDTO reserveResponse = new InventoryReserveClientResponseDTO();
        reserveResponse.setSuccess(true);
        when(productServiceClient.reserveStock(any())).thenReturn(reserveResponse);

        Order savedOrder = new Order("ORD-SIM-1", "IDEMP-SIM-1", customerId, PaymentMethod.RAZORPAY,
                new BigDecimal("800.00"), new BigDecimal("50.00"), new BigDecimal("850.00"),
                "User", "9876543210", "Address");
        savedOrder.setId(201L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(201L);
        orderDTO.setOrderNumber("ORD-SIM-1");
        orderDTO.setOrderStatus(OrderStatus.PLACED);
        when(orderService.mapToOrderDTO(any(Order.class))).thenReturn(orderDTO);

        // 1. orderId starts with order_sim_
        RazorpayVerifyPaymentRequestDTO req1 = new RazorpayVerifyPaymentRequestDTO(
                "order_sim_123", "pay_live_123", "any_signature",
                "ORD-SIM-1", "User", "9876543210", "Address", "IDEMP-SIM-1"
        );
        assertNotNull(razorpayService.verifyAndCompletePayment(customerId, req1, authHeader));

        // 2. paymentId starts with pay_sim_
        RazorpayVerifyPaymentRequestDTO req2 = new RazorpayVerifyPaymentRequestDTO(
                "order_live_123", "pay_sim_123", "any_signature",
                "ORD-SIM-1", "User", "9876543210", "Address", "IDEMP-SIM-2"
        );
        assertNotNull(razorpayService.verifyAndCompletePayment(customerId, req2, authHeader));

        // 3. signature starts with sig_sim_
        RazorpayVerifyPaymentRequestDTO req3 = new RazorpayVerifyPaymentRequestDTO(
                "order_live_123", "pay_live_123", "sig_sim_123",
                "ORD-SIM-1", "User", "9876543210", "Address", "IDEMP-SIM-3"
        );
        assertNotNull(razorpayService.verifyAndCompletePayment(customerId, req3, authHeader));

        // 4. signature equals mock_valid_signature
        RazorpayVerifyPaymentRequestDTO req4 = new RazorpayVerifyPaymentRequestDTO(
                "order_live_123", "pay_live_123", "mock_valid_signature",
                "ORD-SIM-1", "User", "9876543210", "Address", "IDEMP-SIM-4"
        );
        assertNotNull(razorpayService.verifyAndCompletePayment(customerId, req4, authHeader));
    }

    @Test
    @DisplayName("Verify and complete payment returns existing order when payment reference was already recorded")
    void testVerifyAndCompletePayment_ExistingPaymentReference() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayVerifyPaymentRequestDTO request = new RazorpayVerifyPaymentRequestDTO(
                "order_live_123", "pay_already_processed", "mock_valid_signature",
                "ORD-OLD-1", "Alice", "9876543210", "Address", "IDEMP-OLD"
        );

        Order existingOrder = new Order();
        existingOrder.setId(50L);
        existingOrder.setOrderNumber("ORD-OLD-1");

        PaymentRecord existingRecord = new PaymentRecord(
                existingOrder, "pay_already_processed", PaymentMethod.RAZORPAY,
                new BigDecimal("500.00"), "COMPLETED", "Done"
        );
        when(orderRepository.findByIdempotencyKey("IDEMP-OLD")).thenReturn(Optional.empty());
        when(paymentRecordRepository.findByPaymentReference("pay_already_processed")).thenReturn(Optional.of(existingRecord));

        OrderDTO expectedDto = new OrderDTO();
        expectedDto.setId(50L);
        expectedDto.setOrderNumber("ORD-OLD-1");
        when(orderService.mapToOrderDTO(existingOrder)).thenReturn(expectedDto);

        OrderDTO result = razorpayService.verifyAndCompletePayment(customerId, request, authHeader);

        assertNotNull(result);
        assertEquals("ORD-OLD-1", result.getOrderNumber());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify and complete payment payload validation errors")
    void testVerifyAndCompletePayment_ValidationErrors() {
        assertThrows(PaymentGatewayException.class, () -> razorpayService.verifyAndCompletePayment(1L, null, "tok"));

        RazorpayVerifyPaymentRequestDTO emptyOrderId = new RazorpayVerifyPaymentRequestDTO(
                "", "pay_1", "sig_1", "ORD-1", "Name", "123", "Addr", "IDEMP"
        );
        assertThrows(PaymentGatewayException.class, () -> razorpayService.verifyAndCompletePayment(1L, emptyOrderId, "tok"));

        RazorpayVerifyPaymentRequestDTO emptyPaymentId = new RazorpayVerifyPaymentRequestDTO(
                "order_1", "", "sig_1", "ORD-1", "Name", "123", "Addr", "IDEMP"
        );
        assertThrows(PaymentGatewayException.class, () -> razorpayService.verifyAndCompletePayment(1L, emptyPaymentId, "tok"));

        RazorpayVerifyPaymentRequestDTO emptySig = new RazorpayVerifyPaymentRequestDTO(
                "order_1", "pay_1", "", "ORD-1", "Name", "123", "Addr", "IDEMP"
        );
        assertThrows(PaymentGatewayException.class, () -> razorpayService.verifyAndCompletePayment(1L, emptySig, "tok"));
    }

    @Test
    @DisplayName("Verify and complete payment throws BadRequestException when cart is empty during finalize")
    void testVerifyAndCompletePayment_CartEmpty_ThrowsBadRequest() {
        Long customerId = 1L;
        String authHeader = "Bearer mock-token";
        RazorpayVerifyPaymentRequestDTO request = new RazorpayVerifyPaymentRequestDTO(
                "order_live_1", "pay_live_1", "mock_valid_signature",
                "ORD-EMPTY", "Alice", "9876543210", "Address", "IDEMP-EMPTY"
        );

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(paymentRecordRepository.findByPaymentReference(any())).thenReturn(Optional.empty());
        when(cartServiceClient.getCart(customerId, authHeader)).thenReturn(createCart(customerId, Collections.emptyList()));

        assertThrows(BadRequestException.class, () -> razorpayService.verifyAndCompletePayment(customerId, request, authHeader));
    }

    @Test
    @DisplayName("Refund payment handles null order, non-razorpay methods, and missing payment ID")
    void testRefundPayment_GuardClauses() {
        // Null order
        assertFalse(razorpayService.refundPayment(null, new BigDecimal("100.00"), "reason"));

        // COD order
        Order codOrder = new Order();
        codOrder.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        assertFalse(razorpayService.refundPayment(codOrder, new BigDecimal("100.00"), "reason"));

        // Order with no payment records
        Order noRecordsOrder = new Order();
        noRecordsOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        assertFalse(razorpayService.refundPayment(noRecordsOrder, new BigDecimal("100.00"), "reason"));
    }

    @Test
    @DisplayName("Refund payment with live gateway payment ID and simulated fallback")
    void testRefundPayment_SuccessBranches() {
        Order razorpayOrder = new Order();
        razorpayOrder.setOrderNumber("ORD-RZP-REFUND");
        razorpayOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        razorpayOrder.setTotalAmount(new BigDecimal("999.00"));

        PaymentRecord record = new PaymentRecord(
                razorpayOrder, "TXN-REF-123", PaymentMethod.RAZORPAY,
                new BigDecimal("999.00"), "COMPLETED", "Paid",
                "order_live_999", "pay_live_888", "sig_live_777"
        );
        razorpayOrder.addPaymentRecord(record);

        // 1. Live refund API success (200 OK)
        when(restTemplate.postForEntity(contains("/refund"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"status\":\"processed\"}", HttpStatus.OK));

        boolean liveResult = razorpayService.refundPayment(razorpayOrder, new BigDecimal("999.00"), "Customer requested refund");
        assertTrue(liveResult);

        // 2. Live refund API error throws exception -> simulated refund returns true
        when(restTemplate.postForEntity(contains("/refund"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        boolean fallbackResult = razorpayService.refundPayment(razorpayOrder, null, null);
        assertTrue(fallbackResult);
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
