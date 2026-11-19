package com.eshopping.order.exception;

public class PaymentGatewayException extends RuntimeException {

    private final String errorCode;

    public PaymentGatewayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "PAYMENT_GATEWAY_ERROR";
    }

    public PaymentGatewayException(String message) {
        super(message);
        this.errorCode = "PAYMENT_GATEWAY_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
