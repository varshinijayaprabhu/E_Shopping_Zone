package com.eshopping.product.exception;

public class UnauthorizedMerchantException extends RuntimeException {
    public UnauthorizedMerchantException(String message) {
        super(message);
    }
}

