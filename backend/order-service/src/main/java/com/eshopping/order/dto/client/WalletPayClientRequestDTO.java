package com.eshopping.order.dto.client;

import java.math.BigDecimal;

public class WalletPayClientRequestDTO {

    private Long customerId;
    private String orderReference;
    private BigDecimal amount;
    private String idempotencyKey;
    private String description;

    public WalletPayClientRequestDTO() {
    }

    public WalletPayClientRequestDTO(Long customerId, String orderReference, BigDecimal amount, String idempotencyKey, String description) {
        this.customerId = customerId;
        this.orderReference = orderReference;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

