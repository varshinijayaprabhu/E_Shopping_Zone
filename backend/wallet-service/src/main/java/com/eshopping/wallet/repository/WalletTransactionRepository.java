package com.eshopping.wallet.repository;

import com.eshopping.wallet.entity.TransactionType;
import com.eshopping.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<WalletTransaction> findByTransactionReference(String transactionReference);

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Optional<WalletTransaction> findByOrderReferenceAndTransactionType(String orderReference, TransactionType transactionType);

    List<WalletTransaction> findByOrderReference(String orderReference);
}

