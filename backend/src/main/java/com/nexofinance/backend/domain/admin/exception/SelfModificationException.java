package com.nexofinance.backend.domain.admin.exception;

public class SelfModificationException extends RuntimeException {

    public SelfModificationException(String message) {
        super(message);
    }
}
