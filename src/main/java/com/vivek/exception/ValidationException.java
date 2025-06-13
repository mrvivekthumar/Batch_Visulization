package com.vivek.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for validation errors
 */
public class ValidationException extends PerformanceTestException {

    public ValidationException(String message, Object... args) {
        super("VALIDATION_ERROR", String.format(message, args), HttpStatus.BAD_REQUEST, args);
    }

    public ValidationException(String message, Throwable cause, Object... args) {
        super("VALIDATION_ERROR", String.format(message, args), cause, HttpStatus.BAD_REQUEST, args);
    }
}