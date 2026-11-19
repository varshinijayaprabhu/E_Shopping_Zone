package com.eshopping.order.dto;

import java.math.BigDecimal;

public class RazorpayCreateOrderResponseDTO {

    private String razorpayOrderId;
    private String orderNumber;
    private BigDecimal amount;
    private Long amountInPaise;
    private String currency;
    private String keyId;

    public RazorpayCreateOrderResponseDTO() {
    }

    public RazorpayCreateOrderResponseDTO(String razorpayOrderId, String orderNumber, BigDecimal amount,
                                          Long amountInPaise, String currency, String keyId) {
        this.razorpayOrderId = razorpayOrderId;
        this.orderNumber = orderNumber;
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

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getAmountInPaise() {
        return amountInPaise;
    }

    public void setAmountInPaise(Long amountInPaise) {
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
