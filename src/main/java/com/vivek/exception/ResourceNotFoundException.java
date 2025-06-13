package com.vivek.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends PerformanceTestException {
    public ResourceNotFoundException(String message, Object... args) {
        super("RESOURCE_NOT_FOUND", String.format(message, args), HttpStatus.NOT_FOUND, args);
    }
}