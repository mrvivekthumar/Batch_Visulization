package com.vivek.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for database-related errors
 */
public class DatabaseOperationException extends PerformanceTestException {

    public DatabaseOperationException(String message, Object... args) {
        super("DB_OPERATION_ERROR", String.format(message, args), HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    public DatabaseOperationException(String message, Throwable cause, Object... args) {
        super("DB_OPERATION_ERROR", String.format(message, args), cause, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }
}