package com.eshopping.cart.exception;

public class UnauthorizedCartAccessException extends RuntimeException {
    public UnauthorizedCartAccessException(String message) {
        super(message);
    }
}

