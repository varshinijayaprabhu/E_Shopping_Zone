package com.eshopping.wallet.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class RefundRequestDTO {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Order reference is required")
    @Size(max = 64, message = "Order reference must not exceed 64 characters")
    private String orderReference;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    @Size(max = 64, message = "Payment transaction reference must not exceed 64 characters")
    private String paymentTransactionReference;

    public RefundRequestDTO() {
    }

    public RefundRequestDTO(Long customerId, String orderReference, BigDecimal amount, String idempotencyKey, String reason, String paymentTransactionReference) {
        this.customerId = customerId;
        this.orderReference = orderReference;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.reason = reason;
        this.paymentTransactionReference = paymentTransactionReference;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPaymentTransactionReference() {
        return paymentTransactionReference;
    }

    public void setPaymentTransactionReference(String paymentTransactionReference) {
        this.paymentTransactionReference = paymentTransactionReference;
    }
}

