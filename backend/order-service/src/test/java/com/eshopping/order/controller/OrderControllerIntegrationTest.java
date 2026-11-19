package com.eshopping.order.controller;

import com.eshopping.order.client.CartServiceClient;
import com.eshopping.order.client.ProductServiceClient;
import com.eshopping.order.client.WalletServiceClient;
import com.eshopping.order.dto.CancelOrderRequestDTO;
import com.eshopping.order.dto.CheckoutRequestDTO;
import com.eshopping.order.dto.client.*;
import com.eshopping.order.entity.PaymentMethod;
import com.eshopping.order.repository.OrderItemRepository;
import com.eshopping.order.repository.OrderRepository;
import com.eshopping.order.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockBean
    private CartServiceClient cartServiceClient;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private WalletServiceClient walletServiceClient;

    private String customerToken;
    private String merchantToken;
    private String otherCustomerToken;

    @BeforeEach
    public void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        customerToken = "Bearer " + jwtTokenProvider.generateTokenForTest(
                1L, "alice@eshoppingzone.com", Collections.singleton("ROLE_CUSTOMER")
        );
        merchantToken = "Bearer " + jwtTokenProvider.generateTokenForTest(
                2L, "merchant@eshoppingzone.com", Collections.singleton("ROLE_MERCHANT")
        );
        otherCustomerToken = "Bearer " + jwtTokenProvider.generateTokenForTest(
                99L, "mallory@eshoppingzone.com", Collections.singleton("ROLE_CUSTOMER")
        );

        // Setup default mocks for downstream microservices
        CartItemClientDTO item = new CartItemClientDTO(
                1L, 10L, "Premium Headphones", "HEAD-001",
                new BigDecimal("500.00"), "http://img.png", 2,
                new BigDecimal("1000.00"), true, 20
        );
        CartClientDTO cart = new CartClientDTO(1L, 1L, Collections.singletonList(item), 2, new BigDecimal("1000.00"));
        when(cartServiceClient.getCart(eq(1L), any())).thenReturn(cart);

        ProductClientDTO productClient = new ProductClientDTO();
        ProductClientDTO.ProductData productData = new ProductClientDTO.ProductData();
        productData.setId(10L);
        productData.setMerchantId(2L);
        productData.setName("Premium Headphones");
        productData.setPrice(new BigDecimal("500.00"));
        productClient.setData(productData);
        when(productServiceClient.getProductById(10L)).thenReturn(productClient);

        InventoryReserveClientResponseDTO reserveResp = new InventoryReserveClientResponseDTO();
        reserveResp.setSuccess(true);
        when(productServiceClient.reserveStock(any())).thenReturn(reserveResp);

        WalletTransactionClientResponseDTO walletResp = new WalletTransactionClientResponseDTO();
        walletResp.setSuccess(true);
        WalletTransactionClientResponseDTO.WalletTransactionData txnData = new WalletTransactionClientResponseDTO.WalletTransactionData();
        txnData.setTransactionReference("TXN-INTEG-001");
        walletResp.setData(txnData);
        when(walletServiceClient.pay(any())).thenReturn(walletResp);
    }

    @Test
    @DisplayName("POST /api/orders/checkout - Wallet: Should successfully complete checkout saga")
    public void testCheckoutWithWalletSuccess() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-INTEG-001",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park, Bangalore"
        );

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderStatus", is("PLACED")))
                .andExpect(jsonPath("$.data.sagaState", is("COMPLETED")))
                .andExpect(jsonPath("$.data.totalAmount", is(1000.0)))
                .andExpect(jsonPath("$.data.orderNumber", startsWith("ORD-")))
                .andExpect(jsonPath("$.data.payment.paymentMethod", is("WALLET")))
                .andExpect(jsonPath("$.data.payment.paymentStatus", is("PAID")));
    }

    @Test
    @DisplayName("POST /api/orders/checkout - COD: Should successfully complete checkout with Cash On Delivery")
    public void testCheckoutWithCodSuccess() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-INTEG-002",
                PaymentMethod.CASH_ON_DELIVERY,
                "Alice",
                "+91-9876543210",
                "123 Tech Park, Bangalore"
        );

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderStatus", is("PLACED")))
                .andExpect(jsonPath("$.data.payment.paymentMethod", is("CASH_ON_DELIVERY")))
                .andExpect(jsonPath("$.data.payment.paymentStatus", is("COD_PENDING")));
    }

    @Test
    @DisplayName("POST /api/orders/checkout - Should replay idempotently without duplicate charges")
    public void testCheckoutIdempotentReplay() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-INTEG-003",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park, Bangalore"
        );

        // First attempt: creates order
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second attempt with exact same idempotency key: returns same order
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderStatus", is("PLACED")))
                .andExpect(jsonPath("$.data.totalAmount", is(1000.0)));
    }

    @Test
    @DisplayName("POST /api/orders/checkout - Should reject unauthenticated requests with 401")
    public void testCheckoutUnauthenticated() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-NOAUTH",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park"
        );

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/orders - Should return authenticated customer order history")
    public void testGetCustomerOrders() throws Exception {
        // Place an order first
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-HISTORY-001",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park"
        );
        mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].shippingName", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should enforce Anti-IDOR customer isolation")
    public void testGetOrderAntiIdorIsolation() throws Exception {
        // Alice places order
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-IDOR-001",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park"
        );
        String responseStr = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(responseStr).path("data").path("id").asLong();

        // Alice accesses her own order -> 200 OK
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(orderId.intValue())));

        // Mallory (customer 99) attempts to access Alice's order -> 404 NOT_FOUND
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", otherCustomerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/orders/merchant - Should return merchant order items for ROLE_MERCHANT")
    public void testGetMerchantOrders() throws Exception {
        // Place an order first (item is assigned merchantId = 2)
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-MERCH-001",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park"
        );
        mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Merchant (ID 2) queries items
        mockMvc.perform(get("/api/orders/merchant")
                        .header("Authorization", merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].productNameSnapshot", is("Premium Headphones")));
    }

    @Test
    @DisplayName("GET /api/orders/merchant - Should reject ROLE_CUSTOMER with 403 Forbidden")
    public void testGetMerchantOrdersCustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/orders/merchant")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel - Should cancel order and update status")
    public void testCancelOrder() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-CANCEL-001",
                PaymentMethod.WALLET,
                "Alice",
                "+91-9876543210",
                "123 Tech Park"
        );
        String responseStr = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(responseStr).path("data").path("id").asLong();

        CancelOrderRequestDTO cancelReq = new CancelOrderRequestDTO("Bought somewhere else");
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("CANCELLED")))
                .andExpect(jsonPath("$.data.payment.paymentStatus", is("REFUNDED")));
    }

    @Test
    @DisplayName("POST /api/orders/reconcile - Should succeed with valid internal secret")
    public void testReconcileWithInternalSecret() throws Exception {
        mockMvc.perform(post("/api/orders/reconcile")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(0)));
    }

    @Test
    @DisplayName("POST /api/orders/reconcile - Should reject missing or invalid secret with 403")
    public void testReconcileMissingSecretForbidden() throws Exception {
        mockMvc.perform(post("/api/orders/reconcile"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/reconcile")
                        .header("X-Internal-Secret", "WRONG_SECRET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/orders/internal/metrics/summary - Returns order metrics summary")
    public void testGetMetricsSummary() throws Exception {
        mockMvc.perform(get("/api/orders/internal/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalOrders").exists())
                .andExpect(jsonPath("$.data.totalRevenue").exists());
    }

    @Test
    @DisplayName("PUT /api/orders/internal/{id}/status - Updates status and returns 200 OK")
    public void testUpdateOrderStatusInternal() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-STATUS-001", PaymentMethod.WALLET, "Alice", "+91-9876543210", "123 Tech Park"
        );
        String res = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).path("data").path("id").asLong();

        mockMvc.perform(put("/api/orders/internal/" + orderId + "/status")
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("SHIPPED")));
    }

    @Test
    @DisplayName("PUT /api/orders/status/{orderNumber} - Updates status by number and returns 200 OK")
    public void testUpdateOrderStatusByNumber() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-STATUS-002", PaymentMethod.WALLET, "Alice", "+91-9876543210", "123 Tech Park"
        );
        String res = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(res).path("data").path("orderNumber").asText();

        mockMvc.perform(put("/api/orders/status/" + orderNumber)
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("DELIVERED")));

        // Also test GET /api/orders/status/{orderNumber}
        mockMvc.perform(get("/api/orders/status/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber", is(orderNumber)))
                .andExpect(jsonPath("$.data.orderStatus", is("DELIVERED")));
    }

    @Test
    @DisplayName("POST /api/orders/internal/{id}/dispute-refund - Processes admin dispute refund")
    public void testAdminDisputeRefundInternal() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO(
                "IDEMP-DISPUTE-001", PaymentMethod.WALLET, "Alice", "+91-9876543210", "123 Tech Park"
        );
        String res = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).path("data").path("id").asLong();

        mockMvc.perform(post("/api/orders/internal/" + orderId + "/dispute-refund")
                        .param("reason", "Defective goods verified by admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("REFUNDED_BY_ADMIN")));
    }
}

