package com.eshopping.order.dto.client;

import java.math.BigDecimal;

public class WalletRefundClientRequestDTO {

    private Long customerId;
    private String orderReference;
    private BigDecimal amount;
    private String idempotencyKey;
    private String reason;
    private String paymentTransactionReference;

    public WalletRefundClientRequestDTO() {
    }

    public WalletRefundClientRequestDTO(Long customerId, String orderReference, BigDecimal amount,
                                        String idempotencyKey, String reason, String paymentTransactionReference) {
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

