package com.eshopping.wallet.entity;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable financial transaction ledger record for Digital Wallet operations.
 * Records the exact state before and after each financial transaction.
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_tx_wallet", columnList = "wallet_id"),
        @Index(name = "idx_wallet_tx_ref", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_wallet_tx_idemp", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_wallet_tx_order", columnList = "order_reference")
})
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "order_reference", length = 64)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.SUCCESS;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WalletTransaction() {
    }

    public WalletTransaction(Wallet wallet, String transactionReference, String idempotencyKey,
                             String orderReference, TransactionType transactionType, BigDecimal amount,
                             BigDecimal balanceBefore, BigDecimal balanceAfter, TransactionStatus status,
                             String description) {
        this.wallet = wallet;
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.orderReference = orderReference;
        this.transactionType = transactionType;
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.balanceBefore = balanceBefore != null ? balanceBefore.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.status = status != null ? status : TransactionStatus.SUCCESS;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore != null ? balanceBefore.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

