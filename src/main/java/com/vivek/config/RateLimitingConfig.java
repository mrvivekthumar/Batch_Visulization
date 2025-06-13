package com.vivek.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Configuration using Token Bucket Algorithm
 * 
 * Provides protection against:
 * - API abuse and DoS attacks
 * - Resource exhaustion
 * - Excessive database operations
 * 
 * Different limits for different user types and endpoints
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Slf4j
@Component
public class RateLimitingConfig implements HandlerInterceptor {

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.burst-capacity:10}")
    private int burstCapacity;

    @Value("${app.rate-limit.performance-operations-per-hour:20}")
    private int performanceOperationsPerHour;

    // Cache for rate limiting buckets per IP
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> performanceBuckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!rateLimitEnabled) {
            return true;
        }

        String clientIp = getClientIpAddress(request);
        String requestURI = request.getRequestURI();

        // Apply different rate limits based on endpoint type
        if (isPerformanceEndpoint(requestURI)) {
            return handlePerformanceEndpointRateLimit(clientIp, response);
        } else {
            return handleGeneralRateLimit(clientIp, response);
        }
    }

    /**
     * Handle rate limiting for performance testing endpoints
     * These are more resource-intensive, so we apply stricter limits
     */
    private boolean handlePerformanceEndpointRateLimit(String clientIp, HttpServletResponse response)
            throws Exception {

        Bucket bucket = performanceBuckets.computeIfAbsent(clientIp, this::createPerformanceBucket);

        if (bucket.tryConsume(1)) {
            // Add remaining requests header
            response.addHeader("X-Rate-Limit-Remaining-Performance",
                    String.valueOf(bucket.getAvailableTokens()));
            return true;
        } else {
            log.warn("🚫 Performance endpoint rate limit exceeded for IP: {}", clientIp);
            sendRateLimitExceededResponse(response, "Performance operations rate limit exceeded. " +
                    "Maximum " + performanceOperationsPerHour + " operations per hour allowed.");
            return false;
        }
    }

    /**
     * Handle rate limiting for general API endpoints
     */
    private boolean handleGeneralRateLimit(String clientIp, HttpServletResponse response)
            throws Exception {

        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createGeneralBucket);

        if (bucket.tryConsume(1)) {
            // Add remaining requests header
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(bucket.getAvailableTokens()));
            return true;
        } else {
            log.warn("🚫 General rate limit exceeded for IP: {}", clientIp);
            sendRateLimitExceededResponse(response, "Rate limit exceeded. " +
                    "Maximum " + requestsPerMinute + " requests per minute allowed.");
            return false;
        }
    }

    /**
     * Create bucket for general API endpoints
     * 60 requests per minute with burst capacity of 10
     */
    private Bucket createGeneralBucket(String key) {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        Bandwidth burstLimit = Bandwidth.classic(burstCapacity,
                Refill.intervally(burstCapacity, Duration.ofSeconds(10)));

        return Bucket4j.builder()
                .addLimit(limit)
                .addLimit(burstLimit)
                .build();
    }

    /**
     * Create bucket for performance testing endpoints
     * 20 operations per hour (more restrictive due to resource intensity)
     */
    private Bucket createPerformanceBucket(String key) {
        Bandwidth limit = Bandwidth.classic(performanceOperationsPerHour,
                Refill.intervally(performanceOperationsPerHour, Duration.ofHours(1)));

        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Check if the request is for a performance testing endpoint
     */
    private boolean isPerformanceEndpoint(String requestURI) {
        return requestURI != null && (requestURI.contains("/initialize") ||
                requestURI.contains("/delete") ||
                requestURI.contains("/test/"));
    }

    /**
     * Extract client IP address from request
     * Handles proxy headers for accurate identification
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }

        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded;
        }

        return request.getRemoteAddr();
    }

    /**
     * Send rate limit exceeded response
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, String message)
            throws Exception {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.addHeader("Retry-After", "60"); // Suggest retry after 60 seconds

        String jsonResponse = String.format(
                "{\"success\": false, \"error\": \"%s\", \"timestamp\": \"%s\", \"retryAfter\": 60}",
                message,
                java.time.LocalDateTime.now());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * Cleanup old buckets periodically (implement as scheduled task if needed)
     */
    public void cleanupOldBuckets() {
        // This could be called by a scheduled task to prevent memory leaks
        // For now, ConcurrentHashMap will handle concurrent access safely
        log.debug("Rate limiting cache size - General: {}, Performance: {}",
                buckets.size(), performanceBuckets.size());
    }
}