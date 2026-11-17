package com.eshopping.wallet.service;

import com.eshopping.wallet.dto.DepositRequestDTO;
import com.eshopping.wallet.dto.PaymentRequestDTO;
import com.eshopping.wallet.dto.PaymentStatusResponseDTO;
import com.eshopping.wallet.dto.RefundRequestDTO;
import com.eshopping.wallet.dto.WalletDTO;
import com.eshopping.wallet.dto.WalletTransactionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    /**
     * Retrieve the customer's wallet and balance.
     * Auto-creates a new active wallet if one does not exist yet.
     */
    WalletDTO getWallet(Long customerId);

    /**
     * Retrieve paginated immutable ledger transactions for a customer's wallet.
     */
    Page<WalletTransactionDTO> getTransactions(Long customerId, Pageable pageable);

    /**
     * Simulated deposit to top-up customer's wallet balance.
     * Idempotent and concurrency-safe.
     */
    WalletTransactionDTO deposit(Long customerId, DepositRequestDTO request);

    /**
     * Internal checkout payment deduction under pessimistic write locking.
     * Guarantees idempotency and zero double-deduction.
     */
    WalletTransactionDTO pay(PaymentRequestDTO request);

    /**
     * Internal Saga compensating refund under pessimistic write locking.
     * Guarantees idempotency and zero double-credit.
     */
    WalletTransactionDTO refund(RefundRequestDTO request);

    /**
     * Look up payment status by order reference for Order Service reconciliation.
     */
    PaymentStatusResponseDTO getPaymentStatusByOrderReference(String orderReference);
}

