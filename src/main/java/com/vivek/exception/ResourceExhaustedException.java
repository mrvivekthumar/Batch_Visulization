package com.vivek.exception;

import org.springframework.http.HttpStatus;

public class ResourceExhaustedException extends PerformanceTestException {
    public ResourceExhaustedException(String message, Object... args) {
        super("RESOURCE_EXHAUSTED", String.format(message, args), HttpStatus.SERVICE_UNAVAILABLE, args);
    }

    public ResourceExhaustedException(String message, Throwable cause, Object... args) {
        super("RESOURCE_EXHAUSTED", String.format(message, args), cause, HttpStatus.SERVICE_UNAVAILABLE, args);
    }
}