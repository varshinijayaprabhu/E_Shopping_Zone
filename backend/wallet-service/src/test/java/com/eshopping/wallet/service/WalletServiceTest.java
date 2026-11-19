package com.eshopping.wallet.service;

import com.eshopping.wallet.dto.DepositRequestDTO;
import com.eshopping.wallet.dto.PaymentRequestDTO;
import com.eshopping.wallet.dto.PaymentStatusResponseDTO;
import com.eshopping.wallet.dto.RefundRequestDTO;
import com.eshopping.wallet.dto.WalletDTO;
import com.eshopping.wallet.dto.WalletTransactionDTO;
import com.eshopping.wallet.entity.TransactionStatus;
import com.eshopping.wallet.entity.TransactionType;
import com.eshopping.wallet.entity.Wallet;
import com.eshopping.wallet.entity.WalletStatus;
import com.eshopping.wallet.entity.WalletTransaction;
import com.eshopping.wallet.exception.BadRequestException;
import com.eshopping.wallet.exception.InsufficientBalanceException;
import com.eshopping.wallet.exception.ResourceNotFoundException;
import com.eshopping.wallet.repository.WalletRepository;
import com.eshopping.wallet.repository.WalletTransactionRepository;
import com.eshopping.wallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = new Wallet(1L, new BigDecimal("10000.00"));
        testWallet.setId(10L);
        testWallet.setStatus(WalletStatus.ACTIVE);
    }

    @Test
    @DisplayName("getWallet: Returns existing wallet details")
    void testGetWallet_Existing() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(testWallet));

        WalletDTO result = walletService.getWallet(1L);

        assertNotNull(result);
        assertEquals(10L, result.getWalletId());
        assertEquals(1L, result.getCustomerId());
        assertEquals(new BigDecimal("10000.00"), result.getCurrentBalance());
        assertEquals("INR", result.getCurrency());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    @DisplayName("getWallet: Auto-provisions new wallet if none exists")
    void testGetWallet_AutoProvision() {
        when(walletRepository.findByCustomerId(2L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(20L);
            return w;
        });

        WalletDTO result = walletService.getWallet(2L);

        assertNotNull(result);
        assertEquals(20L, result.getWalletId());
        assertEquals(2L, result.getCustomerId());
        assertEquals(new BigDecimal("0.00"), result.getCurrentBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("getTransactions: Returns paginated transactions for customer wallet")
    void testGetTransactions() {
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(testWallet));

        WalletTransaction txn = new WalletTransaction(
                testWallet, "TXN-DEP-001", "IDEMP-001", null,
                TransactionType.DEPOSIT, new BigDecimal("500.00"),
                new BigDecimal("9500.00"), new BigDecimal("10000.00"),
                TransactionStatus.SUCCESS, "Top-up"
        );
        txn.setId(100L);
        txn.setCreatedAt(LocalDateTime.now());

        Page<WalletTransaction> page = new PageImpl<WalletTransaction>(Collections.singletonList(txn));
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class))).thenReturn(page);

        Page<WalletTransactionDTO> result = walletService.getTransactions(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("TXN-DEP-001", result.getContent().get(0).getTransactionReference());
        assertEquals(new BigDecimal("500.00"), result.getContent().get(0).getAmount());
    }

    @Test
    @DisplayName("deposit: Successfully credits balance and records before/after amounts")
    void testDeposit_Success() {
        DepositRequestDTO request = new DepositRequestDTO(
                new BigDecimal("2500.00"), "IDEMP-DEP-101", "UPI", "Demo deposit");

        when(transactionRepository.findByIdempotencyKey("IDEMP-DEP-101")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> {
            WalletTransaction t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });

        WalletTransactionDTO result = walletService.deposit(1L, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("2500.00"), result.getAmount());
        assertEquals(new BigDecimal("10000.00"), result.getBalanceBefore());
        assertEquals(new BigDecimal("12500.00"), result.getBalanceAfter());
        assertEquals(new BigDecimal("12500.00"), testWallet.getCurrentBalance());
        assertEquals("DEPOSIT", result.getTransactionType());
        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getTransactionReference().startsWith("TXN-DEP-"));
    }

    @Test
    @DisplayName("deposit: Duplicate idempotency key returns existing transaction without double credit")
    void testDeposit_Idempotency() {
        DepositRequestDTO request = new DepositRequestDTO(
                new BigDecimal("2500.00"), "IDEMP-DEP-101", "UPI", "Demo deposit");

        WalletTransaction existingTxn = new WalletTransaction(
                testWallet, "TXN-DEP-EXISTING", "IDEMP-DEP-101", null,
                TransactionType.DEPOSIT, new BigDecimal("2500.00"),
                new BigDecimal("10000.00"), new BigDecimal("12500.00"),
                TransactionStatus.SUCCESS, "Demo deposit"
        );
        existingTxn.setId(101L);

        when(transactionRepository.findByIdempotencyKey("IDEMP-DEP-101")).thenReturn(Optional.of(existingTxn));

        WalletTransactionDTO result = walletService.deposit(1L, request);

        assertNotNull(result);
        assertEquals("TXN-DEP-EXISTING", result.getTransactionReference());
        assertEquals(new BigDecimal("12500.00"), result.getBalanceAfter());
        // Verify balance was NOT mutated on wallet
        verify(walletRepository, never()).findByCustomerIdForUpdate(anyLong());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("pay: Successfully debits balance under pessimistic write lock")
    void testPay_Success() {
        PaymentRequestDTO request = new PaymentRequestDTO(
                1L, "ORD-20261107-1001", new BigDecimal("3499.00"), "IDEMP-PAY-001", "Order payment");

        when(transactionRepository.findByIdempotencyKey("IDEMP-PAY-001")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> {
            WalletTransaction t = inv.getArgument(0);
            t.setId(102L);
            return t;
        });

        WalletTransactionDTO result = walletService.pay(request);

        assertNotNull(result);
        assertEquals("ORD-20261107-1001", result.getOrderReference());
        assertEquals(new BigDecimal("3499.00"), result.getAmount());
        assertEquals(new BigDecimal("10000.00"), result.getBalanceBefore());
        assertEquals(new BigDecimal("6501.00"), result.getBalanceAfter());
        assertEquals(new BigDecimal("6501.00"), testWallet.getCurrentBalance());
        assertEquals("PAYMENT", result.getTransactionType());
        assertTrue(result.getTransactionReference().startsWith("TXN-PAY-"));
    }

    @Test
    @DisplayName("pay: Duplicate idempotency key returns existing payment without double deduction")
    void testPay_Idempotency() {
        PaymentRequestDTO request = new PaymentRequestDTO(
                1L, "ORD-20261107-1001", new BigDecimal("3499.00"), "IDEMP-PAY-001", "Order payment");

        WalletTransaction existingTxn = new WalletTransaction(
                testWallet, "TXN-PAY-EXISTING", "IDEMP-PAY-001", "ORD-20261107-1001",
                TransactionType.PAYMENT, new BigDecimal("3499.00"),
                new BigDecimal("10000.00"), new BigDecimal("6501.00"),
                TransactionStatus.SUCCESS, "Order payment"
        );
        existingTxn.setId(102L);

        when(transactionRepository.findByIdempotencyKey("IDEMP-PAY-001")).thenReturn(Optional.of(existingTxn));

        WalletTransactionDTO result = walletService.pay(request);

        assertNotNull(result);
        assertEquals("TXN-PAY-EXISTING", result.getTransactionReference());
        verify(walletRepository, never()).findByCustomerIdForUpdate(anyLong());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("pay: Throws InsufficientBalanceException when requested amount exceeds current balance")
    void testPay_InsufficientBalance() {
        PaymentRequestDTO request = new PaymentRequestDTO(
                1L, "ORD-20261107-9999", new BigDecimal("15000.00"), "IDEMP-PAY-FAIL", "Large payment");

        when(transactionRepository.findByIdempotencyKey("IDEMP-PAY-FAIL")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(testWallet));

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class, () -> {
            walletService.pay(request);
        });

        assertTrue(ex.getMessage().contains("Insufficient wallet balance"));
        assertEquals(new BigDecimal("10000.00"), testWallet.getCurrentBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("refund: Successfully credits balance and records order reference")
    void testRefund_Success() {
        RefundRequestDTO request = new RefundRequestDTO(
                1L, "ORD-20261107-1001", new BigDecimal("3499.00"), "IDEMP-REF-001", "Saga rollback", "TXN-PAY-001");

        when(transactionRepository.findByIdempotencyKey("IDEMP-REF-001")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> {
            WalletTransaction t = inv.getArgument(0);
            t.setId(103L);
            return t;
        });

        WalletTransactionDTO result = walletService.refund(request);

        assertNotNull(result);
        assertEquals("ORD-20261107-1001", result.getOrderReference());
        assertEquals(new BigDecimal("3499.00"), result.getAmount());
        assertEquals(new BigDecimal("10000.00"), result.getBalanceBefore());
        assertEquals(new BigDecimal("13499.00"), result.getBalanceAfter());
        assertEquals(new BigDecimal("13499.00"), testWallet.getCurrentBalance());
        assertEquals("REFUND", result.getTransactionType());
        assertTrue(result.getTransactionReference().startsWith("TXN-REF-"));
    }

    @Test
    @DisplayName("refund: Duplicate idempotency key returns existing refund without double credit")
    void testRefund_Idempotency() {
        RefundRequestDTO request = new RefundRequestDTO(
                1L, "ORD-20261107-1001", new BigDecimal("3499.00"), "IDEMP-REF-001", "Saga rollback", "TXN-PAY-001");

        WalletTransaction existingTxn = new WalletTransaction(
                testWallet, "TXN-REF-EXISTING", "IDEMP-REF-001", "ORD-20261107-1001",
                TransactionType.REFUND, new BigDecimal("3499.00"),
                new BigDecimal("10000.00"), new BigDecimal("13499.00"),
                TransactionStatus.SUCCESS, "Saga rollback"
        );
        existingTxn.setId(103L);

        when(transactionRepository.findByIdempotencyKey("IDEMP-REF-001")).thenReturn(Optional.of(existingTxn));

        WalletTransactionDTO result = walletService.refund(request);

        assertNotNull(result);
        assertEquals("TXN-REF-EXISTING", result.getTransactionReference());
        verify(walletRepository, never()).findByCustomerIdForUpdate(anyLong());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("getPaymentStatusByOrderReference: Returns payment details when found")
    void testGetPaymentStatus_Found() {
        WalletTransaction paymentTxn = new WalletTransaction(
                testWallet, "TXN-PAY-999", "IDEMP-999", "ORD-RECON-100",
                TransactionType.PAYMENT, new BigDecimal("14999.00"),
                new BigDecimal("20000.00"), new BigDecimal("5001.00"),
                TransactionStatus.SUCCESS, "Payment for ORD-RECON-100"
        );
        paymentTxn.setId(999L);
        paymentTxn.setCreatedAt(LocalDateTime.now());

        when(transactionRepository.findByOrderReferenceAndTransactionType("ORD-RECON-100", TransactionType.PAYMENT))
                .thenReturn(Optional.of(paymentTxn));

        PaymentStatusResponseDTO result = walletService.getPaymentStatusByOrderReference("ORD-RECON-100");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("TXN-PAY-999", result.getTransactionReference());
        assertEquals("ORD-RECON-100", result.getOrderReference());
        assertEquals(new BigDecimal("14999.00"), result.getAmount());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("PAYMENT", result.getTransactionType());
    }

    @Test
    @DisplayName("getPaymentStatusByOrderReference: Returns not found for non-existent order")
    void testGetPaymentStatus_NotFound() {
        when(transactionRepository.findByOrderReferenceAndTransactionType("ORD-NON-EXISTENT", TransactionType.PAYMENT))
                .thenReturn(Optional.empty());

        PaymentStatusResponseDTO result = walletService.getPaymentStatusByOrderReference("ORD-NON-EXISTENT");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertEquals("ORD-NON-EXISTENT", result.getOrderReference());
        assertEquals("NOT_FOUND", result.getStatus());
    }

    @Test
    @DisplayName("getPaymentStatusByOrderReference: Empty or null order reference throws BadRequestException")
    void testGetPaymentStatus_NullOrEmptyThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> walletService.getPaymentStatusByOrderReference(null));
        assertThrows(BadRequestException.class, () -> walletService.getPaymentStatusByOrderReference("   "));
    }

    @Test
    @DisplayName("getWallet: Null customer ID throws BadRequestException")
    void testGetWallet_NullCustomerIdThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> walletService.getWallet(null));
    }

    @Test
    @DisplayName("getWallet: Auto-provisions wallet if not found")
    void testGetWallet_AutoProvisionsWallet() {
        when(walletRepository.findByCustomerId(999L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> {
            Wallet w = i.getArgument(0);
            w.setId(99L);
            return w;
        });

        WalletDTO dto = walletService.getWallet(999L);

        assertNotNull(dto);
        assertEquals(999L, dto.getCustomerId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("getTransactions: Null customer ID throws BadRequestException")
    void testGetTransactions_NullCustomerId() {
        assertThrows(BadRequestException.class, () -> walletService.getTransactions(null, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("getTransactions: Non-existent wallet throws ResourceNotFoundException")
    void testGetTransactions_WalletNotFound() {
        when(walletRepository.findByCustomerId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                walletService.getTransactions(999L, PageRequest.of(0, 10))
        );
    }

    @Test
    @DisplayName("deposit: Idempotency conflict with different amount throws BadRequestException")
    void testDeposit_IdempotencyConflictThrowsBadRequest() {
        WalletTransaction existing = new WalletTransaction();
        existing.setAmount(new BigDecimal("100.00"));

        when(transactionRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(existing));

        DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("200.00"), "KEY-123", "UPI", "deposit");

        assertThrows(BadRequestException.class, () -> walletService.deposit(1L, request));
    }

    @Test
    @DisplayName("deposit: Negative or null amount throws BadRequestException")
    void testDeposit_InvalidAmountThrowsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                walletService.deposit(1L, new DepositRequestDTO(BigDecimal.ZERO, "K1", "UPI", "deposit"))
        );
        assertThrows(BadRequestException.class, () ->
                walletService.deposit(null, new DepositRequestDTO(new BigDecimal("10.00"), "K1", "UPI", "deposit"))
        );
    }

    @Test
    @DisplayName("refund: Idempotency conflict throws BadRequestException")
    void testRefund_IdempotencyConflict() {
        WalletTransaction existing = new WalletTransaction();
        existing.setAmount(new BigDecimal("100.00"));
        existing.setOrderReference("ORD-1");

        when(transactionRepository.findByIdempotencyKey("REF-KEY")).thenReturn(Optional.of(existing));

        RefundRequestDTO request = new RefundRequestDTO(1L, "ORD-2", new BigDecimal("100.00"), "REF-KEY", "reason", null);

        assertThrows(BadRequestException.class, () -> walletService.refund(request));
    }

    @Test
    @DisplayName("refund: Amount exceeding original payment throws BadRequestException")
    void testRefund_ExceedsOriginalPaymentThrowsBadRequest() {
        WalletTransaction originalPayment = new WalletTransaction();
        originalPayment.setAmount(new BigDecimal("500.00"));

        when(transactionRepository.findByIdempotencyKey("REF-KEY")).thenReturn(Optional.empty());
        when(transactionRepository.findByOrderReferenceAndTransactionType("ORD-1", TransactionType.PAYMENT))
                .thenReturn(Optional.of(originalPayment));

        RefundRequestDTO request = new RefundRequestDTO(1L, "ORD-1", new BigDecimal("600.00"), "REF-KEY", "reason", null);

        assertThrows(BadRequestException.class, () -> walletService.refund(request));
    }

    @Test
    @DisplayName("pay: Null request, customerId, or amount throws BadRequestException")
    void testPay_NullValidationThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> walletService.pay(null));
        assertThrows(BadRequestException.class, () -> walletService.pay(new PaymentRequestDTO(null, "ORD-1", new BigDecimal("100.00"), "K", "desc")));
        assertThrows(BadRequestException.class, () -> walletService.pay(new PaymentRequestDTO(1L, "ORD-1", null, "K", "desc")));
    }

    @Test
    @DisplayName("pay: Non-positive amount throws BadRequestException")
    void testPay_NonPositiveAmountThrowsBadRequest() {
        PaymentRequestDTO request = new PaymentRequestDTO(1L, "ORD-1", BigDecimal.ZERO, "K", "desc");
        assertThrows(BadRequestException.class, () -> walletService.pay(request));
    }

    @Test
    @DisplayName("pay: Idempotency conflict with different amount or order throws BadRequestException")
    void testPay_IdempotencyConflictThrowsBadRequest() {
        WalletTransaction existing = new WalletTransaction();
        existing.setAmount(new BigDecimal("100.00"));
        existing.setOrderReference("ORD-100");

        when(transactionRepository.findByIdempotencyKey("PAY-KEY")).thenReturn(Optional.of(existing));

        PaymentRequestDTO conflictAmount = new PaymentRequestDTO(1L, "ORD-100", new BigDecimal("200.00"), "PAY-KEY", "desc");
        PaymentRequestDTO conflictOrder = new PaymentRequestDTO(1L, "ORD-200", new BigDecimal("100.00"), "PAY-KEY", "desc");

        assertThrows(BadRequestException.class, () -> walletService.pay(conflictAmount));
        assertThrows(BadRequestException.class, () -> walletService.pay(conflictOrder));
    }

    @Test
    @DisplayName("pay: Inactive wallet status throws IllegalStateException")
    void testPay_InactiveWalletThrowsIllegalState() {
        testWallet.setStatus(WalletStatus.FROZEN);

        when(transactionRepository.findByIdempotencyKey("KEY-FROZEN")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(testWallet));

        PaymentRequestDTO request = new PaymentRequestDTO(1L, "ORD-1", new BigDecimal("50.00"), "KEY-FROZEN", "desc");

        assertThrows(IllegalStateException.class, () -> walletService.pay(request));
    }

    @Test
    @DisplayName("refund: Null or empty reason uses default description format without parenthesis")
    void testRefund_NullReasonDescription() {
        RefundRequestDTO request = new RefundRequestDTO(
                1L, "ORD-NO-REASON", new BigDecimal("50.00"), "IDEMP-REF-NO-REASON", null, null
        );

        when(transactionRepository.findByIdempotencyKey("IDEMP-REF-NO-REASON")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletTransactionDTO res = walletService.refund(request);

        assertNotNull(res);
        assertEquals("Refund for order ORD-NO-REASON", res.getDescription());
    }

    @Test
    @DisplayName("refund: Auto-creates wallet if wallet does not exist during refund credit")
    void testRefund_AutoCreatesWalletWhenNotFound() {
        RefundRequestDTO request = new RefundRequestDTO(
                888L, "ORD-NEW-WALLET", new BigDecimal("100.00"), "IDEMP-REF-NEW-W", "Refund return", null
        );

        when(transactionRepository.findByIdempotencyKey("IDEMP-REF-NEW-W")).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdForUpdate(888L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(88L);
            return w;
        });
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletTransactionDTO res = walletService.refund(request);

        assertNotNull(res);
        assertEquals(new BigDecimal("100.00"), res.getAmount());
        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("refund: Null request or customerId or amount throws BadRequestException")
    void testRefund_NullValidationThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> walletService.refund(null));
        assertThrows(BadRequestException.class, () -> walletService.refund(new RefundRequestDTO(null, "ORD-1", new BigDecimal("50.00"), "K", "r", null)));
        assertThrows(BadRequestException.class, () -> walletService.refund(new RefundRequestDTO(1L, "ORD-1", null, "K", "r", null)));
    }

    @Test
    @DisplayName("refund: Non-positive refund amount throws BadRequestException")
    void testRefund_NonPositiveAmountThrowsBadRequest() {
        RefundRequestDTO req = new RefundRequestDTO(1L, "ORD-1", BigDecimal.ZERO, "K", "r", null);
        assertThrows(BadRequestException.class, () -> walletService.refund(req));
    }

    @Test
    @DisplayName("deposit: Null deposit request throws BadRequestException")
    void testDeposit_NullRequestThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> walletService.deposit(1L, null));
    }
}

