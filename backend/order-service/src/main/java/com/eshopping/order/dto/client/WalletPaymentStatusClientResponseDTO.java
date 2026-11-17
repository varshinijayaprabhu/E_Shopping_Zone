package com.eshopping.order.dto.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletPaymentStatusClientResponseDTO {

    private boolean success;
    private String message;
    private PaymentStatusData data;

    public WalletPaymentStatusClientResponseDTO() {
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

    public PaymentStatusData getData() {
        return data;
    }

    public void setData(PaymentStatusData data) {
        this.data = data;
    }

    public static class PaymentStatusData {
        private boolean found;
        private String transactionReference;
        private String orderReference;
        private Long customerId;
        private BigDecimal amount;
        private String status;
        private String transactionType;
        private LocalDateTime createdAt;

        public PaymentStatusData() {
        }

        public boolean isFound() {
            return found;
        }

        public void setFound(boolean found) {
            this.found = found;
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

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTransactionType() {
            return transactionType;
        }

        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}

