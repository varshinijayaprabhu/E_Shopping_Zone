package com.eshopping.order.dto.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionClientResponseDTO {

    private boolean success;
    private String message;
    private WalletTransactionData data;

    public WalletTransactionClientResponseDTO() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WalletTransactionData getData() {
        return data;
    }

    public void setData(WalletTransactionData data) {
        this.data = data;
    }

    public static class WalletTransactionData {
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

        public WalletTransactionData() {
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
            this.amount = amount;
        }

        public BigDecimal getBalanceBefore() {
            return balanceBefore;
        }

        public void setBalanceBefore(BigDecimal balanceBefore) {
            this.balanceBefore = balanceBefore;
        }

        public BigDecimal getBalanceAfter() {
            return balanceAfter;
        }

        public void setBalanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
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
}

