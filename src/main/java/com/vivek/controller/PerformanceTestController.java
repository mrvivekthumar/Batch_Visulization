package com.vivek.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.vivek.service.PerformanceTestService;
import com.vivek.service.PerformanceTestService.DatabaseStats;
import com.vivek.service.PerformanceTestService.PerformanceResult;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Database Performance Testing Operations
 * 
 * Provides endpoints for:
 * - Batch insertion performance testing
 * - Batch deletion performance testing
 * - System and database statistics
 * - Health monitoring
 * 
 * @author Vivek
 * @version 1.0.0
 * @since 2025-06-14
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/performance")
@CrossOrigin(origins = {
        "http://localhost:8080",
        "http://localhost:3000",
        "http://127.0.0.1:8080",
        "http://127.0.0.1:3000"
}, methods = {
        RequestMethod.GET,
        RequestMethod.POST
}, maxAge = 3600)
@Validated
public class PerformanceTestController {

    private final PerformanceTestService performanceTestService;

    @Autowired
    public PerformanceTestController(PerformanceTestService performanceTestService) {
        this.performanceTestService = performanceTestService;
    }

    /**
     * Smart Initialize - Insert records based on batch size
     * If batchSize = 1: Insert one by one
     * If batchSize > 1: Insert in batches
     * 
     * @param totalRecords Number of records to insert (100-100000)
     * @param batchSize    Size of each batch (1-10000)
     * @return Performance test results
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PerformanceResult>> smartInitialize(
            @RequestParam(defaultValue = "1000") @Min(value = 100, message = "Total records must be at least 100") @Max(value = 100000, message = "Total records cannot exceed 100,000") @NotNull(message = "Total records is required") Integer totalRecords,

            @RequestParam(defaultValue = "100") @Min(value = 1, message = "Batch size must be at least 1") @Max(value = 10000, message = "Batch size cannot exceed 10,000") @NotNull(message = "Batch size is required") Integer batchSize) {

        // Validate business logic
        if (batchSize > totalRecords) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Batch size cannot be greater than total records"));
        }

        log.info("📝 Smart Initialize Request: {} records with batch size {}", totalRecords, batchSize);

        try {
            PerformanceResult result = performanceTestService.smartInsert(totalRecords, batchSize);

            log.info("✅ Smart initialize completed successfully: {} records in {}ms",
                    result.getRecordsProcessed(), result.getDurationMs());

            return ResponseEntity.ok(ApiResponse.success(result, "Records inserted successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid request for smart initialize: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid input: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Smart initialize failed for {} records", totalRecords, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Performance test failed. Please try again."));
        }
    }

    /**
     * Smart Delete - Delete records based on batch size
     * If batchSize = 1: Delete one by one
     * If batchSize > 1: Delete in batches
     * 
     * @param totalRecords Number of records to delete (1-50000)
     * @param batchSize    Size of each batch (1-5000)
     * @return Performance test results
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<PerformanceResult>> smartDelete(
            @RequestParam(defaultValue = "1000") @Min(value = 1, message = "Total records must be at least 1") @Max(value = 50000, message = "Total records cannot exceed 50,000 for deletion") @NotNull(message = "Total records is required") Integer totalRecords,

            @RequestParam(defaultValue = "100") @Min(value = 1, message = "Batch size must be at least 1") @Max(value = 5000, message = "Batch size cannot exceed 5,000 for deletion") @NotNull(message = "Batch size is required") Integer batchSize) {

        // Validate business logic
        if (batchSize > totalRecords) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Batch size cannot be greater than total records"));
        }

        log.info("🗑️ Smart Delete Request: {} records with batch size {}", totalRecords, batchSize);

        try {
            PerformanceResult result = performanceTestService.smartDelete(totalRecords, batchSize);

            log.info("✅ Smart delete completed successfully: {} records in {}ms",
                    result.getRecordsProcessed(), result.getDurationMs());

            return ResponseEntity.ok(ApiResponse.success(result, "Records deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid request for smart delete: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid input: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Smart delete failed for {} records", totalRecords, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Delete operation failed. Please try again."));
        }
    }

    /**
     * Get comprehensive database statistics
     * 
     * @return Database statistics including record count and table size
     */
    @GetMapping("/stats/database")
    public ResponseEntity<ApiResponse<DatabaseStats>> getDatabaseStats() {
        log.debug("📊 Database stats requested");

        try {
            DatabaseStats stats = performanceTestService.getDatabaseStats();
            return ResponseEntity.ok(ApiResponse.success(stats, "Database statistics retrieved"));

        } catch (Exception e) {
            log.error("❌ Failed to get database stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to retrieve database statistics"));
        }
    }

    /**
     * Get comprehensive system information and JVM statistics
     * 
     * @return System performance metrics
     */
    @GetMapping("/stats/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        log.debug("🖥️ System stats requested");

        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemStats = new HashMap<>();

            // JVM Information
            systemStats.put("jvm", Map.of(
                    "version", System.getProperty("java.version"),
                    "vendor", System.getProperty("java.vendor"),
                    "availableProcessors", runtime.availableProcessors(),
                    "totalMemoryMB", runtime.totalMemory() / (1024 * 1024),
                    "freeMemoryMB", runtime.freeMemory() / (1024 * 1024),
                    "usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
                    "maxMemoryMB", runtime.maxMemory() / (1024 * 1024),
                    "memoryUsagePercent",
                    Math.round(((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100)));

            // Operating System Information
            systemStats.put("os", Map.of(
                    "name", System.getProperty("os.name"),
                    "version", System.getProperty("os.version"),
                    "architecture", System.getProperty("os.arch")));

            // Application Information
            systemStats.put("application", Map.of(
                    "name", "Database Batch Performance Analyzer",
                    "version", "1.0.0",
                    "uptime", getApplicationUptime(),
                    "timestamp", LocalDateTime.now()));

            return ResponseEntity.ok(ApiResponse.success(systemStats, "System statistics retrieved"));

        } catch (Exception e) {
            log.error("❌ Failed to get system stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to retrieve system statistics"));
        }
    }

    /**
     * Enhanced health check endpoint with detailed status
     * 
     * @return Application health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();

            // Basic health information
            health.put("status", "UP");
            health.put("service", "Database Batch Performance Analyzer");
            health.put("version", "1.0.0");
            health.put("timestamp", LocalDateTime.now());

            // Check database connectivity
            try {
                DatabaseStats dbStats = performanceTestService.getDatabaseStats();
                health.put("database", Map.of(
                        "status", "UP",
                        "totalRecords", dbStats.getTotalRecords()));
            } catch (Exception e) {
                health.put("database", Map.of(
                        "status", "DOWN",
                        "error", "Database connection failed"));
                log.warn("Database health check failed: {}", e.getMessage());
            }

            // Memory health check
            Runtime runtime = Runtime.getRuntime();
            double memoryUsagePercent = ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory())
                    * 100;
            health.put("memory", Map.of(
                    "status", memoryUsagePercent < 90 ? "UP" : "WARNING",
                    "usagePercent", Math.round(memoryUsagePercent)));

            return ResponseEntity.ok(ApiResponse.success(health, "Health check completed"));

        } catch (Exception e) {
            log.error("❌ Health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Health check failed"));
        }
    }

    /**
     * Get application uptime in a human-readable format
     */
    private String getApplicationUptime() {
        long uptimeMs = System.currentTimeMillis() -
                java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();

        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d days, %d hours", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }

    /**
     * Standardized API Response wrapper
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private LocalDateTime timestamp;
        private String error;

        private ApiResponse(boolean success, String message, T data, String error) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.error = error;
            this.timestamp = LocalDateTime.now();
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data, null);
        }

        public static <T> ApiResponse<T> error(String error) {
            return new ApiResponse<>(false, null, null, error);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getError() {
            return error;
        }
    }
}