package com.eshopping.wallet.dto;

import java.math.BigDecimal;

public class RazorpayTopupCreateOrderResponseDTO {

    private String razorpayOrderId;
    private String receipt;
    private BigDecimal amount;
    private long amountInPaise;
    private String currency;
    private String keyId;

    public RazorpayTopupCreateOrderResponseDTO() {
    }

    public RazorpayTopupCreateOrderResponseDTO(String razorpayOrderId, String receipt, BigDecimal amount,
                                             long amountInPaise, String currency, String keyId) {
        this.razorpayOrderId = razorpayOrderId;
        this.receipt = receipt;
        this.amount = amount;
        this.amountInPaise = amountInPaise;
        this.currency = currency;
        this.keyId = keyId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public long getAmountInPaise() {
        return amountInPaise;
    }

    public void setAmountInPaise(long amountInPaise) {
        this.amountInPaise = amountInPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}
