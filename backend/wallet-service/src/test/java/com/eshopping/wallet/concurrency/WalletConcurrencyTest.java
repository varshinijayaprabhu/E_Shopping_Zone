package com.eshopping.wallet.concurrency;

import com.eshopping.wallet.dto.PaymentRequestDTO;
import com.eshopping.wallet.entity.Wallet;
import com.eshopping.wallet.entity.WalletStatus;
import com.eshopping.wallet.exception.InsufficientBalanceException;
import com.eshopping.wallet.repository.WalletRepository;
import com.eshopping.wallet.repository.WalletTransactionRepository;
import com.eshopping.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class WalletConcurrencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent Deductions: 10 threads concurrently deducting ₹1,000 from ₹10,000 results in exactly ₹0.00")
    void testConcurrentDeductions_ExactExhaustion() throws Exception {
        Long customerId = 501L;
        Wallet wallet = new Wallet(customerId, new BigDecimal("10000.00"));
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Ensure all threads start simultaneously
                    PaymentRequestDTO req = new PaymentRequestDTO(
                            customerId,
                            "ORD-CONCUR-A-" + index,
                            new BigDecimal("1000.00"),
                            "IDEMP-CONCUR-A-" + index,
                            "Concurrent payment " + index
                    );
                    walletService.pay(req);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire simultaneous requests
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within 15 seconds");
        assertEquals(10, successCount.get(), "All 10 concurrent deductions should succeed");
        assertEquals(0, failCount.get(), "Zero failures expected");

        Wallet finalWallet = walletRepository.findByCustomerId(customerId).orElseThrow(IllegalStateException::new);
        assertEquals(new BigDecimal("0.00"), finalWallet.getCurrentBalance(), "Balance should be exactly 0.00");
    }

    @Test
    @DisplayName("Concurrent Over-Deductions: 10 threads attempting ₹1,000 deduction on ₹5,000; exactly 5 succeed, 5 fail, balance never goes negative")
    void testConcurrentDeductions_OverDraftProtection() throws Exception {
        Long customerId = 502L;
        Wallet wallet = new Wallet(customerId, new BigDecimal("5000.00"));
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize fire
                    PaymentRequestDTO req = new PaymentRequestDTO(
                            customerId,
                            "ORD-CONCUR-B-" + index,
                            new BigDecimal("1000.00"),
                            "IDEMP-CONCUR-B-" + index,
                            "Concurrent payment " + index
                    );
                    walletService.pay(req);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException ex) {
                    insufficientCount.incrementAndGet();
                } catch (Exception ex) {
                    // Any other transient exception
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within 15 seconds");
        assertEquals(5, successCount.get(), "Exactly 5 deductions should succeed");
        assertEquals(5, insufficientCount.get(), "Exactly 5 deductions should fail due to insufficient funds");

        Wallet finalWallet = walletRepository.findByCustomerId(customerId).orElseThrow(IllegalStateException::new);
        assertEquals(new BigDecimal("0.00"), finalWallet.getCurrentBalance(), "Balance should never be negative, exactly 0.00");
    }
}

