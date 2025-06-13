package com.vivek.exception;

import org.springframework.http.HttpStatus;

public class PerformanceOperationException extends PerformanceTestException {
    public PerformanceOperationException(String message, Object... args) {
        super("PERFORMANCE_OPERATION_ERROR", String.format(message, args), HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    public PerformanceOperationException(String message, Throwable cause, Object... args) {
        super("PERFORMANCE_OPERATION_ERROR", String.format(message, args), cause, HttpStatus.INTERNAL_SERVER_ERROR,
                args);
    }
}