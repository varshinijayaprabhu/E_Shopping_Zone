package com.eshopping.wallet.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class DepositRequestDTO {

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "1.00", message = "Deposit amount must be at least ₹1.00")
    @DecimalMax(value = "100000.00", message = "Deposit amount cannot exceed ₹100,000.00")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    @Size(max = 50, message = "Funding source must not exceed 50 characters")
    private String fundingSource;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public DepositRequestDTO() {
    }

    public DepositRequestDTO(BigDecimal amount, String idempotencyKey, String fundingSource, String description) {
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.fundingSource = fundingSource;
        this.description = description;
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

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

