package com.eshopping.profile.exception;

public class CsrfViolationException extends RuntimeException {
    public CsrfViolationException(String message) {
        super(message);
    }
}

