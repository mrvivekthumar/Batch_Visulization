package com.vivek.controller;

import com.vivek.dto.DatabaseStats;
import com.vivek.dto.PerformanceResult;
import com.vivek.service.PerformanceTestService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/performance")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:3000", "http://127.0.0.1:8080",
        "http://127.0.0.1:3000" }, methods = { RequestMethod.GET, RequestMethod.POST }, maxAge = 3600)
@Validated
public class PerformanceTestController {

    private final PerformanceTestService performanceTestService;

    @Autowired
    public PerformanceTestController(PerformanceTestService performanceTestService) {
        this.performanceTestService = performanceTestService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PerformanceResult>> smartInitialize(
            @RequestParam(defaultValue = "1000") @Min(value = 100, message = "Total records must be at least 100") @Max(value = 100000, message = "Total records cannot exceed 100,000") @NotNull(message = "Total records is required") Integer totalRecords,
            @RequestParam(defaultValue = "100") @Min(value = 1, message = "Batch size must be at least 1") @Max(value = 10000, message = "Batch size cannot exceed 10,000") @NotNull(message = "Batch size is required") Integer batchSize) {

        if (batchSize > totalRecords) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Batch size cannot be greater than total records"));
        }

        try {
            PerformanceResult result = performanceTestService.smartInsert(totalRecords, batchSize);
            return ResponseEntity.ok(ApiResponse.success(result, "Records inserted successfully"));
        } catch (Exception e) {
            log.error("❌ Smart initialize failed for {} records", totalRecords, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Performance test failed. Please try again."));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<PerformanceResult>> smartDelete(
            @RequestParam(defaultValue = "1000") @Min(value = 1, message = "Total records must be at least 1") @Max(value = 50000, message = "Total records cannot exceed 50,000 for deletion") @NotNull(message = "Total records is required") Integer totalRecords,
            @RequestParam(defaultValue = "100") @Min(value = 1, message = "Batch size must be at least 1") @Max(value = 5000, message = "Batch size cannot exceed 5,000 for deletion") @NotNull(message = "Batch size is required") Integer batchSize) {

        if (batchSize > totalRecords) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Batch size cannot be greater than total records"));
        }

        try {
            PerformanceResult result = performanceTestService.smartDelete(totalRecords, batchSize);
            return ResponseEntity.ok(ApiResponse.success(result, "Records deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Smart delete failed for {} records", totalRecords, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Delete operation failed. Please try again."));
        }
    }

    @GetMapping("/stats/database")
    public ResponseEntity<ApiResponse<DatabaseStats>> getDatabaseStats() {
        try {
            DatabaseStats stats = performanceTestService.getDatabaseStats();
            return ResponseEntity.ok(ApiResponse.success(stats, "Database statistics retrieved"));
        } catch (Exception e) {
            log.error("❌ Failed to get database stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to retrieve database statistics"));
        }
    }

    // Other methods like getSystemStats and healthCheck remain the same...

    @GetMapping("/stats/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        log.debug("🖥️ System stats requested");

        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemStats = new HashMap<>();

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

            systemStats.put("os", Map.of(
                    "name", System.getProperty("os.name"),
                    "version", System.getProperty("os.version"),
                    "architecture", System.getProperty("os.arch")));

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