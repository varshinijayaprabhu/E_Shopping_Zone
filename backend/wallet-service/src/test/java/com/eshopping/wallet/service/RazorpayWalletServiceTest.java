package com.eshopping.wallet.service;

import com.eshopping.wallet.dto.DepositRequestDTO;
import com.eshopping.wallet.dto.RazorpayTopupCreateOrderRequestDTO;
import com.eshopping.wallet.dto.RazorpayTopupCreateOrderResponseDTO;
import com.eshopping.wallet.dto.RazorpayTopupVerifyRequestDTO;
import com.eshopping.wallet.dto.WalletTransactionDTO;
import com.eshopping.wallet.entity.TransactionStatus;
import com.eshopping.wallet.entity.TransactionType;
import com.eshopping.wallet.exception.PaymentGatewayException;
import com.eshopping.wallet.service.impl.RazorpayWalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RazorpayWalletServiceTest {

    private static final String TEST_KEY_ID = "rzp_test_mockKey123";
    private static final String TEST_KEY_SECRET = "secret_test_mockKeySecret456";

    @Mock
    private WalletService walletService;

    @Mock
    private RestTemplate restTemplate;

    private RazorpayWalletServiceImpl razorpayWalletService;

    @BeforeEach
    void setUp() {
        razorpayWalletService = new RazorpayWalletServiceImpl(walletService);
        razorpayWalletService.setKeyId(TEST_KEY_ID);
        razorpayWalletService.setKeySecret(TEST_KEY_SECRET);
        razorpayWalletService.setCurrency("INR");
        ReflectionTestUtils.setField(razorpayWalletService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Create topup order calculates paise and creates order intent")
    void testCreateTopupOrder_Success() {
        RazorpayTopupCreateOrderRequestDTO request = new RazorpayTopupCreateOrderRequestDTO(new BigDecimal("1000.00"));

        RazorpayTopupCreateOrderResponseDTO response = razorpayWalletService.createTopupOrder(1L, request);

        assertNotNull(response);
        assertNotNull(response.getRazorpayOrderId());
        assertTrue(response.getRazorpayOrderId().startsWith("order_"));
        assertNotNull(response.getReceipt());
        assertTrue(response.getReceipt().startsWith("REC-TOPUP-"));
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals(100000L, response.getAmountInPaise());
        assertEquals("INR", response.getCurrency());
        assertEquals(TEST_KEY_ID, response.getKeyId());
    }

    @Test
    @DisplayName("Create topup order with invalid amount throws PaymentGatewayException")
    void testCreateTopupOrder_InvalidAmount_ThrowsException() {
        RazorpayTopupCreateOrderRequestDTO zeroRequest = new RazorpayTopupCreateOrderRequestDTO(BigDecimal.ZERO);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                razorpayWalletService.createTopupOrder(1L, zeroRequest));
        assertEquals("INVALID_AMOUNT", ex.getErrorCode());

        PaymentGatewayException nullEx = assertThrows(PaymentGatewayException.class, () ->
                razorpayWalletService.createTopupOrder(1L, null));
        assertEquals("INVALID_AMOUNT", nullEx.getErrorCode());
    }

    @Test
    @DisplayName("Verify valid cryptographic HMAC-SHA256 signature credits wallet balance")
    void testVerifyAndCreditWallet_ValidSignature_CreditsWallet() throws Exception {
        String orderId = "order_live_ABC1234567";
        String paymentId = "pay_live_XYZ9876543";
        String payload = orderId + "|" + paymentId;
        String validSignature = calculateHmacSha256(payload, TEST_KEY_SECRET);

        BigDecimal topupAmount = new BigDecimal("1500.00");
        RazorpayTopupVerifyRequestDTO request = new RazorpayTopupVerifyRequestDTO(
                paymentId, orderId, validSignature, topupAmount
        );

        WalletTransactionDTO mockTxn = new WalletTransactionDTO(
                100L, "TXN-TOPUP-100", "TOPUP-" + orderId + "-" + paymentId,
                "DEPOSIT", topupAmount,
                new BigDecimal("500.00"), new BigDecimal("2000.00"),
                "SUCCESS", "Wallet Top-up via Razorpay Gateway", LocalDateTime.now()
        );

        when(walletService.deposit(eq(1L), any(DepositRequestDTO.class))).thenReturn(mockTxn);

        WalletTransactionDTO result = razorpayWalletService.verifyAndCreditWallet(1L, request);

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(topupAmount, result.getAmount());

        ArgumentCaptor<DepositRequestDTO> captor = ArgumentCaptor.forClass(DepositRequestDTO.class);
        verify(walletService, times(1)).deposit(eq(1L), captor.capture());

        DepositRequestDTO capturedRequest = captor.getValue();
        assertEquals(topupAmount, capturedRequest.getAmount());
        assertEquals("RAZORPAY", capturedRequest.getFundingSource());
        assertEquals("TOPUP-" + orderId + "-" + paymentId, capturedRequest.getIdempotencyKey());
    }

    @Test
    @DisplayName("Verify tampered HMAC-SHA256 signature throws PaymentGatewayException and rejects credit")
    void testVerifyAndCreditWallet_TamperedSignature_ThrowsException() {
        String orderId = "order_live_ABC1234567";
        String paymentId = "pay_live_XYZ9876543";
        String tamperedSignature = "invalid_tampered_signature_hash_000000";

        BigDecimal topupAmount = new BigDecimal("1000.00");
        RazorpayTopupVerifyRequestDTO request = new RazorpayTopupVerifyRequestDTO(
                paymentId, orderId, tamperedSignature, topupAmount
        );

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                razorpayWalletService.verifyAndCreditWallet(1L, request));

        assertEquals("PAYMENT_SIGNATURE_MISMATCH", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Cryptographic signature verification failed"));

        // Ensure walletService.deposit is NEVER called
        verify(walletService, never()).deposit(any(), any());
    }

    @Test
    @DisplayName("Verify missing payload fields throws PaymentGatewayException")
    void testVerifyAndCreditWallet_MissingFields_ThrowsException() {
        RazorpayTopupVerifyRequestDTO missingSignature = new RazorpayTopupVerifyRequestDTO(
                "order_123", "pay_456", "", new BigDecimal("100.00")
        );

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                razorpayWalletService.verifyAndCreditWallet(1L, missingSignature));
        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());

        RazorpayTopupVerifyRequestDTO nullRequest = null;
        PaymentGatewayException nullEx = assertThrows(PaymentGatewayException.class, () ->
                razorpayWalletService.verifyAndCreditWallet(1L, nullRequest));
        assertEquals("INVALID_REQUEST", nullEx.getErrorCode());

        verify(walletService, never()).deposit(any(), any());
    }

    @Test
    @DisplayName("Verify zero or negative amount in verification throws PaymentGatewayException")
    void testVerifyAndCreditWallet_InvalidAmount_ThrowsException() {
        RazorpayTopupVerifyRequestDTO zeroAmount = new RazorpayTopupVerifyRequestDTO(
                "order_123", "pay_456", "sig_123", BigDecimal.ZERO
        );

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                razorpayWalletService.verifyAndCreditWallet(1L, zeroAmount));
        assertEquals("INVALID_AMOUNT", ex.getErrorCode());

        verify(walletService, never()).deposit(any(), any());
    }

    @Test
    @DisplayName("Create topup order with live Razorpay API 200 response parses remote order id")
    void testCreateTopupOrder_LiveRazorpayApiSuccess() {
        String jsonResponse = "{\"id\":\"order_live_topup_9988\"}";
        when(restTemplate.postForEntity(eq("https://api.razorpay.com/v1/orders"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        RazorpayTopupCreateOrderRequestDTO request = new RazorpayTopupCreateOrderRequestDTO(new BigDecimal("500.00"));
        RazorpayTopupCreateOrderResponseDTO response = razorpayWalletService.createTopupOrder(1L, request);

        assertNotNull(response);
        assertEquals("order_live_topup_9988", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("500.00"), response.getAmount());
    }

    @Test
    @DisplayName("Create topup order when keyId contains placeholder falls back to simulated ID")
    void testCreateTopupOrder_PlaceholderKeyFallback() {
        razorpayWalletService.setKeyId("rzp_test_placeholder_key");
        RazorpayTopupCreateOrderRequestDTO request = new RazorpayTopupCreateOrderRequestDTO(new BigDecimal("200.00"));

        RazorpayTopupCreateOrderResponseDTO response = razorpayWalletService.createTopupOrder(1L, request);

        assertNotNull(response);
        assertTrue(response.getRazorpayOrderId().startsWith("order_topup_"));
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("Verify and credit wallet with simulated order and signature prefixes succeeds")
    void testVerifyAndCreditWallet_MockSignatures() {
        WalletTransactionDTO mockTxn = new WalletTransactionDTO(
                101L, "TXN-TOPUP-101", "TOPUP-order_topup_1-pay_sim_1",
                "DEPOSIT", new BigDecimal("300.00"),
                BigDecimal.ZERO, new BigDecimal("300.00"),
                "SUCCESS", "Topup", LocalDateTime.now()
        );
        when(walletService.deposit(eq(1L), any(DepositRequestDTO.class))).thenReturn(mockTxn);

        // 1. orderId starts with order_topup_
        RazorpayTopupVerifyRequestDTO req1 = new RazorpayTopupVerifyRequestDTO("pay_any", "order_topup_123", "any_sig", new BigDecimal("300.00"));
        assertNotNull(razorpayWalletService.verifyAndCreditWallet(1L, req1));

        // 2. orderId starts with order_sim_
        RazorpayTopupVerifyRequestDTO req2 = new RazorpayTopupVerifyRequestDTO("pay_any", "order_sim_456", "any_sig", new BigDecimal("300.00"));
        assertNotNull(razorpayWalletService.verifyAndCreditWallet(1L, req2));

        // 3. paymentId starts with pay_sim_
        RazorpayTopupVerifyRequestDTO req3 = new RazorpayTopupVerifyRequestDTO("pay_sim_789", "order_other", "any_sig", new BigDecimal("300.00"));
        assertNotNull(razorpayWalletService.verifyAndCreditWallet(1L, req3));

        // 4. signature starts with sig_sim_
        RazorpayTopupVerifyRequestDTO req4 = new RazorpayTopupVerifyRequestDTO("pay_other", "order_other", "sig_sim_000", new BigDecimal("300.00"));
        assertNotNull(razorpayWalletService.verifyAndCreditWallet(1L, req4));

        // 5. signature equals mock_valid_signature
        RazorpayTopupVerifyRequestDTO req5 = new RazorpayTopupVerifyRequestDTO("pay_other", "order_other", "mock_valid_signature", new BigDecimal("300.00"));
        assertNotNull(razorpayWalletService.verifyAndCreditWallet(1L, req5));
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
