package com.eshopping.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletDTO {

    private Long walletId;
    private Long customerId;
    private BigDecimal currentBalance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WalletDTO() {
    }

    public WalletDTO(Long walletId, Long customerId, BigDecimal currentBalance, String currency, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.walletId = walletId;
        this.customerId = customerId;
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

