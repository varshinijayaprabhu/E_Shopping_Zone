package com.eshopping.wallet.controller;

import com.eshopping.wallet.dto.DepositRequestDTO;
import com.eshopping.wallet.dto.PaymentRequestDTO;
import com.eshopping.wallet.dto.RefundRequestDTO;
import com.eshopping.wallet.entity.Wallet;
import com.eshopping.wallet.entity.WalletStatus;
import com.eshopping.wallet.repository.WalletRepository;
import com.eshopping.wallet.repository.WalletTransactionRepository;
import com.eshopping.wallet.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private Wallet customerWallet;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        // Customer ID 100 with initial balance of 50000.00
        customerWallet = new Wallet(100L, new BigDecimal("50000.00"));
        customerWallet.setStatus(WalletStatus.ACTIVE);
        customerWallet = walletRepository.save(customerWallet);

        customerToken = "Bearer " + jwtTokenProvider.generateToken(100L, "customer@test.com", Collections.singletonList("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("GET /api/wallet: Authenticated customer retrieves wallet balance")
    void testGetWallet_Authenticated() throws Exception {
        mockMvc.perform(get("/api/wallet")
                        .header("Authorization", customerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.customerId", is(100)))
                .andExpect(jsonPath("$.data.currentBalance", is(50000.00)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/wallet: Unauthenticated request returns 401 Unauthorized")
    void testGetWallet_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/wallet")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/wallet/deposit: Customer can deposit funds")
    void testDeposit_Success() throws Exception {
        DepositRequestDTO request = new DepositRequestDTO(
                new BigDecimal("5000.00"), "IDEMP-TEST-DEP-001", "SIMULATED_UPI", "Top-up deposit");

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.amount", is(5000.00)))
                .andExpect(jsonPath("$.data.balanceBefore", is(50000.00)))
                .andExpect(jsonPath("$.data.balanceAfter", is(55000.00)))
                .andExpect(jsonPath("$.data.transactionType", is("DEPOSIT")));

        Wallet updated = walletRepository.findByCustomerId(100L).orElseThrow(IllegalStateException::new);
        assertEquals(new BigDecimal("55000.00"), updated.getCurrentBalance());
    }

    @Test
    @DisplayName("POST /api/wallet/deposit: Invalid zero/negative amount returns 400 Bad Request")
    void testDeposit_InvalidAmount() throws Exception {
        DepositRequestDTO request = new DepositRequestDTO(
                new BigDecimal("0.00"), "IDEMP-TEST-FAIL", "UPI", "Zero amount");

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/wallet/pay: Internal endpoint rejects call without service credentials")
    void testPay_UnauthorizedAccess() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(
                100L, "ORD-20261107-101", new BigDecimal("1000.00"), "IDEMP-PAY-SEC-01", "Unauthorized call");

        mockMvc.perform(post("/api/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("POST /api/wallet/pay: Authorized with X-Internal-Secret deducts balance successfully")
    void testPay_WithInternalSecret() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(
                100L, "ORD-20261107-101", new BigDecimal("12000.00"), "IDEMP-PAY-SEC-02", "Order checkout payment");

        mockMvc.perform(post("/api/wallet/pay")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.amount", is(12000.00)))
                .andExpect(jsonPath("$.data.balanceBefore", is(50000.00)))
                .andExpect(jsonPath("$.data.balanceAfter", is(38000.00)))
                .andExpect(jsonPath("$.data.orderReference", is("ORD-20261107-101")))
                .andExpect(jsonPath("$.data.transactionType", is("PAYMENT")));

        Wallet updated = walletRepository.findByCustomerId(100L).orElseThrow(IllegalStateException::new);
        assertEquals(new BigDecimal("38000.00"), updated.getCurrentBalance());
    }

    @Test
    @DisplayName("POST /api/wallet/pay: Request with X-Internal-Service alone without secret is rejected with 403 Forbidden")
    void testPay_WithInternalServiceAlone_Rejected() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(
                100L, "ORD-20261107-102", new BigDecimal("5000.00"), "IDEMP-PAY-SEC-03", "Order checkout payment");

        mockMvc.perform(post("/api/wallet/pay")
                        .header("X-Internal-Service", "order-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("POST /api/wallet/pay: Duplicate payment request is idempotent (no double deduction)")
    void testPay_IdempotentDuplicate() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(
                100L, "ORD-20261107-103", new BigDecimal("10000.00"), "IDEMP-PAY-DUPLICATE", "Order checkout payment");

        // First call
        mockMvc.perform(post("/api/wallet/pay")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter", is(40000.00)));

        // Immediate duplicate call with same idempotency key
        mockMvc.perform(post("/api/wallet/pay")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter", is(40000.00)));

        // Balance must remain 40000.00, NOT 30000.00
        Wallet updated = walletRepository.findByCustomerId(100L).orElseThrow(IllegalStateException::new);
        assertEquals(new BigDecimal("40000.00"), updated.getCurrentBalance());
    }

    @Test
    @DisplayName("POST /api/wallet/refund: Compensating refund credits back balance idempotently")
    void testRefund_SuccessAndIdempotency() throws Exception {
        RefundRequestDTO request = new RefundRequestDTO(
                100L, "ORD-20261107-104", new BigDecimal("7500.00"), "IDEMP-REF-TEST-01", "Saga compensation", "TXN-PAY-XYZ");

        // First refund call
        mockMvc.perform(post("/api/wallet/refund")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.balanceBefore", is(50000.00)))
                .andExpect(jsonPath("$.data.balanceAfter", is(57500.00)))
                .andExpect(jsonPath("$.data.transactionType", is("REFUND")));

        // Duplicate refund call
        mockMvc.perform(post("/api/wallet/refund")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter", is(57500.00)));

        // Balance must remain 57500.00, NOT 65000.00
        Wallet updated = walletRepository.findByCustomerId(100L).orElseThrow(IllegalStateException::new);
        assertEquals(new BigDecimal("57500.00"), updated.getCurrentBalance());
    }

    @Test
    @DisplayName("GET /api/wallet/payment/order/{orderReference}: Reconciliation lookup returns payment record")
    void testPaymentStatusReconciliation() throws Exception {
        // First make a payment
        PaymentRequestDTO payReq = new PaymentRequestDTO(
                100L, "ORD-RECON-777", new BigDecimal("4999.00"), "IDEMP-RECON-777", "Reconciliation test");

        mockMvc.perform(post("/api/wallet/pay")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        // Now lookup status for Order Service reconciliation
        mockMvc.perform(get("/api/wallet/payment/order/ORD-RECON-777")
                        .header("X-Internal-Secret", "ESHOPPING_INTERNAL_SECRET_2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.found", is(true)))
                .andExpect(jsonPath("$.data.orderReference", is("ORD-RECON-777")))
                .andExpect(jsonPath("$.data.amount", is(4999.00)))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")));
    }
}

