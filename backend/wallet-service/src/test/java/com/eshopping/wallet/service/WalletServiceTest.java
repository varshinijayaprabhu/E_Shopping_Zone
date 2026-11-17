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
}

