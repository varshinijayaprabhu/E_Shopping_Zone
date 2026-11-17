package com.eshopping.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionDTO {

    private Long transactionId;
    private String transactionReference;
    private String orderReference;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String status;
    private String description;
    private LocalDateTime createdAt;

    public WalletTransactionDTO() {
    }

    public WalletTransactionDTO(Long transactionId, String transactionReference, String orderReference,
                                String transactionType, BigDecimal amount, BigDecimal balanceBefore,
                                BigDecimal balanceAfter, String status, String description,
                                LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.transactionReference = transactionReference;
        this.orderReference = orderReference;
        this.transactionType = transactionType;
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.balanceBefore = balanceBefore != null ? balanceBefore.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

