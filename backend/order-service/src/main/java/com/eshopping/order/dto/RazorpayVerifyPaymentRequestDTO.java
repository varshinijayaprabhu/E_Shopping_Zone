package com.eshopping.order.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RazorpayVerifyPaymentRequestDTO {

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotBlank(message = "Shipping name is required")
    @Size(max = 100, message = "Shipping name cannot exceed 100 characters")
    private String shippingName;

    @NotBlank(message = "Contact phone number is required")
    @Size(min = 10, max = 20, message = "Contact phone must be between 10 and 20 digits")
    private String shippingPhone;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String shippingAddress;

    private String idempotencyKey;

    public RazorpayVerifyPaymentRequestDTO() {
    }

    public RazorpayVerifyPaymentRequestDTO(String razorpayOrderId, String razorpayPaymentId,
                                           String razorpaySignature, String orderNumber,
                                           String shippingName, String shippingPhone,
                                           String shippingAddress, String idempotencyKey) {
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpaySignature = razorpaySignature;
        this.orderNumber = orderNumber;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.idempotencyKey = idempotencyKey;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
