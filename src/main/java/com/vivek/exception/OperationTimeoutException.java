package com.vivek.exception;

import org.springframework.http.HttpStatus;

public class OperationTimeoutException extends PerformanceTestException {
    public OperationTimeoutException(String message, Object... args) {
        super("OPERATION_TIMEOUT", String.format(message, args), HttpStatus.REQUEST_TIMEOUT, args);
    }

    public OperationTimeoutException(String message, Throwable cause, Object... args) {
        super("OPERATION_TIMEOUT", String.format(message, args), cause, HttpStatus.REQUEST_TIMEOUT, args);
    }
}