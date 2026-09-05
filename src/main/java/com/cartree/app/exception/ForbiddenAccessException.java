package com.cartree.app.exception;

public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
