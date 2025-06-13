package com.vivek.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.vivek.repository.PerformanceTestRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Health Indicators for Database Batch Performance Analyzer
 * 
 * Provides detailed health checks for:
 * - Database connectivity and performance
 * - Application-specific functionality
 * - System resource utilization
 * - External service dependencies
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Slf4j
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PerformanceTestRepository repository;

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        }
    }

    private Health checkDatabaseHealth() {
        Map<String, Object> details = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            boolean isValid = connection.isValid(5);
            long connectionTime = System.currentTimeMillis() - startTime;

            details.put("connectionTime", connectionTime + "ms");
            details.put("connectionValid", isValid);
            details.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
            details.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            details.put("jdbcUrl", connection.getMetaData().getURL());

            if (!isValid || connectionTime > 1000) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "Database connection slow or invalid")
                        .build();
            }

            // Test application-specific functionality
            try {
                Long recordCount = repository.getTotalRecordCount();
                details.put("totalRecords", recordCount != null ? recordCount : 0);

                // Test a simple query performance
                long queryStart = System.currentTimeMillis();
                repository.testConnection();
                long queryTime = System.currentTimeMillis() - queryStart;

                details.put("queryTime", queryTime + "ms");

                if (queryTime > 500) {
                    return Health.down()
                            .status("DEGRADED")
                            .withDetails(details)
                            .withDetail("reason", "Database queries are slow")
                            .build();
                }

            } catch (DataAccessException e) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("queryError", e.getMessage())
                        .withDetail("reason", "Database query failed")
                        .build();
            }

            return Health.up()
                    .withDetails(details)
                    .withDetail("status", "Database is healthy")
                    .build();

        } catch (SQLException e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("sqlState", e.getSQLState())
                    .withDetail("errorCode", e.getErrorCode())
                    .withDetail("reason", "Database connection failed")
                    .build();
        }
    }
}

/**
 * System Resource Health Indicator
 */
@Slf4j
@Component
class SystemResourceHealthIndicator implements HealthIndicator {

    private static final double MEMORY_THRESHOLD = 0.9; // 90%
    private static final double DISK_THRESHOLD = 0.9; // 90%

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        try {
            // Memory check
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            double memoryUsageRatio = (double) usedMemory / maxMemory;

            details.put("memory", Map.of(
                    "max", formatBytes(maxMemory),
                    "total", formatBytes(totalMemory),
                    "used", formatBytes(usedMemory),
                    "free", formatBytes(freeMemory),
                    "usagePercent", Math.round(memoryUsageRatio * 100)));

            // Disk space check
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            double diskUsageRatio = (double) usedSpace / totalSpace;

            details.put("disk", Map.of(
                    "total", formatBytes(totalSpace),
                    "used", formatBytes(usedSpace),
                    "free", formatBytes(freeSpace),
                    "usagePercent", Math.round(diskUsageRatio * 100)));

            // CPU information
            details.put("cpu", Map.of(
                    "availableProcessors", runtime.availableProcessors(),
                    "systemLoadAverage", getSystemLoadAverage()));

            // Determine health status
            if (memoryUsageRatio > MEMORY_THRESHOLD) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "High memory usage: " + Math.round(memoryUsageRatio * 100) + "%")
                        .build();
            }

            if (diskUsageRatio > DISK_THRESHOLD) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "High disk usage: " + Math.round(diskUsageRatio * 100) + "%")
                        .build();
            }

            if (memoryUsageRatio > 0.8 || diskUsageRatio > 0.8) {
                return Health.up()
                        .status("WARNING")
                        .withDetails(details)
                        .withDetail("reason", "Resource usage is high but acceptable")
                        .build();
            }

            return Health.up()
                    .withDetails(details)
                    .withDetail("status", "System resources are healthy")
                    .build();

        } catch (Exception e) {
            log.error("System resource health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("reason", "System resource check failed")
                    .build();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private double getSystemLoadAverage() {
        try {
            return java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception e) {
            return -1.0; // Not available
        }
    }
}

/**
 * Application Performance Health Indicator
 */
@Slf4j
@Component
class PerformanceHealthIndicator implements HealthIndicator {

    @Autowired
    private PerformanceTestRepository repository;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        try {
            // Test database performance
            long startTime = System.currentTimeMillis();

            // Simple count query
            Long totalRecords = repository.getTotalRecordCount();
            long countQueryTime = System.currentTimeMillis() - startTime;

            details.put("database", Map.of(
                    "totalRecords", totalRecords != null ? totalRecords : 0,
                    "countQueryTime", countQueryTime + "ms"));

            // Test more complex query
            startTime = System.currentTimeMillis();
            try {
                String tableSize = repository.getTableSize();
                long complexQueryTime = System.currentTimeMillis() - startTime;

                details.put("performance", Map.of(
                        "tableSize", tableSize,
                        "complexQueryTime", complexQueryTime + "ms"));

                // Performance thresholds
                if (countQueryTime > 1000 || complexQueryTime > 2000) {
                    return Health.down()
                            .status("DEGRADED")
                            .withDetails(details)
                            .withDetail("reason", "Database queries are performing slowly")
                            .build();
                }

            } catch (Exception e) {
                details.put("tableSize", "Unable to determine");
                log.warn("Could not determine table size: {}", e.getMessage());
            }

            // Active operations check
            try {
                Integer activeConnections = repository.getActiveConnectionCount();
                details.put("connections", Map.of(
                        "active", activeConnections != null ? activeConnections : 0));

                if (activeConnections != null && activeConnections > 50) {
                    return Health.up()
                            .status("WARNING")
                            .withDetails(details)
                            .withDetail("reason", "High number of active connections")
                            .build();
                }

            } catch (Exception e) {
                log.warn("Could not check active connections: {}", e.getMessage());
            }

            return Health.up()
                    .withDetails(details)
                    .withDetail("status", "Application performance is healthy")
                    .build();

        } catch (Exception e) {
            log.error("Performance health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("reason", "Performance check failed")
                    .build();
        }
    }
}

/**
 * External Dependencies Health Indicator
 */
@Slf4j
@Component
class ExternalDependenciesHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        // Check Redis (if configured)
        try {
            // This would check Redis connectivity if you have Redis client configured
            details.put("redis", Map.of(
                    "status", "not_configured",
                    "message", "Redis dependency check skipped - not configured"));
        } catch (Exception e) {
            allHealthy = false;
            details.put("redis", Map.of(
                    "status", "down",
                    "error", e.getMessage()));
        }

        // Check external APIs (if any)
        details.put("externalAPIs", Map.of(
                "status", "none_configured",
                "message", "No external APIs configured"));

        // Check file system access
        try {
            java.io.File tempFile = java.io.File.createTempFile("health_check", ".tmp");
            tempFile.delete();

            details.put("filesystem", Map.of(
                    "status", "healthy",
                    "writable", true));
        } catch (Exception e) {
            allHealthy = false;
            details.put("filesystem", Map.of(
                    "status", "error",
                    "writable", false,
                    "error", e.getMessage()));
        }

        if (allHealthy) {
            return Health.up()
                    .withDetails(details)
                    .withDetail("status", "All external dependencies are healthy")
                    .build();
        } else {
            return Health.down()
                    .withDetails(details)
                    .withDetail("reason", "One or more external dependencies are unhealthy")
                    .build();
        }
    }
}