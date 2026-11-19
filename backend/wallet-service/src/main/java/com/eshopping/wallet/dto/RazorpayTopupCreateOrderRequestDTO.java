package com.eshopping.wallet.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RazorpayTopupCreateOrderRequestDTO {

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "1.00", message = "Deposit amount must be at least ₹1.00")
    @DecimalMax(value = "100000.00", message = "Deposit amount cannot exceed ₹100,000.00")
    private BigDecimal amount;

    public RazorpayTopupCreateOrderRequestDTO() {
    }

    public RazorpayTopupCreateOrderRequestDTO(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
