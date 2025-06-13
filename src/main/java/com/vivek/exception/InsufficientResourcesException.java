package com.vivek.exception;

import org.springframework.http.HttpStatus;

public class InsufficientResourcesException extends PerformanceTestException {
    public InsufficientResourcesException(String message, Object... args) {
        super("INSUFFICIENT_RESOURCES", String.format(message, args), HttpStatus.CONFLICT, args);
    }
}