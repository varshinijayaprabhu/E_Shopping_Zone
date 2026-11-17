package com.eshopping.order.dto;

import com.eshopping.order.entity.PaymentMethod;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CheckoutRequestDTO {

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Shipping recipient name is required")
    @Size(max = 100, message = "Shipping recipient name must not exceed 100 characters")
    private String shippingName;

    @NotBlank(message = "Shipping contact phone is required")
    @Size(max = 20, message = "Shipping contact phone must not exceed 20 characters")
    private String shippingPhone;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;

    public CheckoutRequestDTO() {
    }

    public CheckoutRequestDTO(String idempotencyKey, PaymentMethod paymentMethod,
                              String shippingName, String shippingPhone, String shippingAddress) {
        this.idempotencyKey = idempotencyKey;
        this.paymentMethod = paymentMethod;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getShippingName() {
        return shippingName;
    }

    public void setShippingName(String shippingName) {
        this.shippingName = shippingName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}

