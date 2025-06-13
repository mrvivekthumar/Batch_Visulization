package com.vivek.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all performance testing related errors
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Getter
public abstract class PerformanceTestException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] args;

    protected PerformanceTestException(String errorCode, String message, HttpStatus httpStatus, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }

    protected PerformanceTestException(String errorCode, String message, Throwable cause, HttpStatus httpStatus,
            Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }
}