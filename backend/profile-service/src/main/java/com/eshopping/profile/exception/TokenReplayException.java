package com.eshopping.profile.exception;

public class TokenReplayException extends RuntimeException {
    public TokenReplayException(String message) {
        super(message);
    }
}

