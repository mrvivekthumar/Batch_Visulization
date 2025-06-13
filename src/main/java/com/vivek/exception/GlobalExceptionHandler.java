package com.vivek.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for comprehensive error management
 * 
 * Provides:
 * - Structured error responses
 * - Security-safe error messages
 * - Proper HTTP status codes
 * - Request correlation tracking
 * - Detailed logging for debugging
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle custom performance test exceptions
     */
    @ExceptionHandler(PerformanceTestException.class)
    public ResponseEntity<ErrorResponse> handlePerformanceTestException(
            PerformanceTestException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        log.error("🚨 [{}] Performance test exception: {} - {}",
                correlationId, ex.getErrorCode(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /**
     * Handle validation errors from request parameters
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        log.warn("⚠️ [{}] Validation error: {}", correlationId, fieldErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("VALIDATION_FAILED")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        String violations = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("⚠️ [{}] Constraint violation: {}", correlationId, violations);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("CONSTRAINT_VIOLATION")
                .message("Request constraints violated: " + violations)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        log.warn("⚠️ [{}] Type mismatch: {}", correlationId, message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("INVALID_PARAMETER_TYPE")
                .message(message)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle security exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        log.warn("🔒 [{}] Access denied: {}", correlationId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("ACCESS_DENIED")
                .message("Access denied. Insufficient privileges for this operation.")
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        log.warn("🔐 [{}] Authentication failed: {}", correlationId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("AUTHENTICATION_FAILED")
                .message("Invalid credentials provided.")
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle transaction timeout exceptions
     */
    @ExceptionHandler(TransactionTimedOutException.class)
    public ResponseEntity<ErrorResponse> handleTransactionTimeoutException(
            TransactionTimedOutException ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        log.error("⏰ [{}] Transaction timeout: {}", correlationId, ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("TRANSACTION_TIMEOUT")
                .message("Operation timed out. Please try with smaller batch size or contact support.")
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    /**
     * Handle out of memory errors
     */
    @ExceptionHandler(OutOfMemoryError.class)
    public ResponseEntity<ErrorResponse> handleOutOfMemoryError(
            OutOfMemoryError ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        log.error("💾 [{}] Out of memory error: {}", correlationId, ex.getMessage(), ex);

        // Force garbage collection
        System.gc();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("OUT_OF_MEMORY")
                .message("Insufficient memory to complete operation. Please reduce batch size and try again.")
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        String correlationId = generateCorrelationId();

        log.error("💥 [{}] Unexpected error: {}", correlationId, ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please try again or contact support.")
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Generate correlation ID for request tracking
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extract request path safely
     */
    private String extractPath(WebRequest request) {
        try {
            return request.getDescription(false).replace("uri=", "");
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Standardized error response structure
     */
    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private boolean success;
        private String errorCode;
        private String message;
        private Map<String, String> fieldErrors;
        private LocalDateTime timestamp;
        private String correlationId;
        private String path;

        // Additional debugging info (only in development)
        private Object debugInfo;
    }
}