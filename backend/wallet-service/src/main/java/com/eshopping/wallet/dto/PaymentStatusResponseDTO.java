package com.eshopping.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentStatusResponseDTO {

    private boolean found;
    private String transactionReference;
    private String orderReference;
    private Long customerId;
    private BigDecimal amount;
    private String status;
    private String transactionType;
    private LocalDateTime createdAt;

    public PaymentStatusResponseDTO() {
    }

    public PaymentStatusResponseDTO(boolean found, String transactionReference, String orderReference,
                                    Long customerId, BigDecimal amount, String status,
                                    String transactionType, LocalDateTime createdAt) {
        this.found = found;
        this.transactionReference = transactionReference;
        this.orderReference = orderReference;
        this.customerId = customerId;
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
        this.status = status;
        this.transactionType = transactionType;
        this.createdAt = createdAt;
    }

    public static PaymentStatusResponseDTO notFound(String orderReference) {
        PaymentStatusResponseDTO dto = new PaymentStatusResponseDTO();
        dto.setFound(false);
        dto.setOrderReference(orderReference);
        dto.setStatus("NOT_FOUND");
        return dto;
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
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
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

