package com.vivek.metrics;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vivek.repository.PerformanceTestRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Metrics Configuration for Database Batch Performance Analyzer
 * 
 * Provides application-specific metrics including:
 * - Database performance metrics
 * - System resource metrics
 * - Custom gauges and counters
 * 
 * NOTE: ApplicationMetrics is already a @Component, so we don't create a bean
 * for it here
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class CustomMetricsConfig {

    /**
     * Database Performance Metrics
     */
    @Bean
    public MeterBinder databaseMetrics(DataSource dataSource, PerformanceTestRepository repository) {
        return new DatabaseMetrics(dataSource, repository);
    }

    /**
     * NOTE: ApplicationMetrics bean removed - it's already a @Component
     * Removing this prevents the duplicate bean conflict
     */

    /**
     * System Resource Metrics
     * NOTE: Removed SystemResourceMetrics as it's already defined in
     * ApplicationMetrics.java
     * to prevent duplicate class compilation error
     */
}

/**
 * Database-specific metrics
 */
@Slf4j
class DatabaseMetrics implements MeterBinder {

    private final DataSource dataSource;
    private final PerformanceTestRepository repository;
    private final AtomicLong lastRecordCount = new AtomicLong(0);
    private final AtomicLong connectionTestTime = new AtomicLong(0);

    public DatabaseMetrics(DataSource dataSource, PerformanceTestRepository repository) {
        this.dataSource = dataSource;
        this.repository = repository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Database record count gauge
        Gauge.builder("database.records.total", this::getRecordCount)
                .description("Total number of records in the database")
                .tag("table", "performance_test_records")
                .register(registry);

        Gauge.builder("database.connection.test.time", this::getConnectionTestTime)
                .description("Time taken to test database connection")
                .baseUnit("milliseconds")
                .register(registry);

        Gauge.builder("database.table.size.bytes", this::getTableSizeBytes)
                .description("Size of the main table in bytes")
                .tag("table", "performance_test_records")
                .register(registry);

        Gauge.builder("database.connections.active", this::getActiveConnectionCount)
                .description("Number of active database connections")
                .register(registry);

        Gauge.builder("database.health.status", this::getDatabaseHealthStatus)
                .description("Database health status (1=healthy, 0=unhealthy)")
                .register(registry);
    }

    private double getRecordCount() {
        try {
            Long count = repository.getTotalRecordCount();
            if (count != null) {
                lastRecordCount.set(count);
                return count.doubleValue();
            }
        } catch (Exception e) {
            log.warn("Failed to get record count for metrics: {}", e.getMessage());
        }
        return lastRecordCount.get();
    }

    private double getConnectionTestTime() {
        try {
            long startTime = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                connection.isValid(1);
            }
            long testTime = System.currentTimeMillis() - startTime;
            connectionTestTime.set(testTime);
            return testTime;
        } catch (Exception e) {
            log.warn("Failed to test database connection for metrics: {}", e.getMessage());
            return -1.0;
        }
    }

    private double getTableSizeBytes() {
        try {
            String sizeStr = repository.getTableSize();
            if (sizeStr != null && !sizeStr.equals("N/A")) {
                return parseSizeToBytes(sizeStr);
            }
        } catch (Exception e) {
            log.warn("Failed to get table size for metrics: {}", e.getMessage());
        }
        return 0.0;
    }

    private double getActiveConnectionCount() {
        try {
            Integer count = repository.getActiveConnectionCount();
            return count != null ? count.doubleValue() : 0.0;
        } catch (Exception e) {
            log.warn("Failed to get active connection count for metrics: {}", e.getMessage());
            return 0.0;
        }
    }

    private double getDatabaseHealthStatus() {
        try {
            repository.testConnection();
            return 1.0; // Healthy
        } catch (Exception e) {
            return 0.0; // Unhealthy
        }
    }

    private double parseSizeToBytes(String sizeStr) {
        try {
            String[] parts = sizeStr.trim().split("\\s+");
            if (parts.length == 2) {
                double value = Double.parseDouble(parts[0]);
                String unit = parts[1].toLowerCase();

                return switch (unit) {
                    case "bytes", "b" -> value;
                    case "kb" -> value * 1024;
                    case "mb" -> value * 1024 * 1024;
                    case "gb" -> value * 1024 * 1024 * 1024;
                    default -> 0.0;
                };
            }
        } catch (Exception e) {
            log.warn("Failed to parse size string '{}': {}", sizeStr, e.getMessage());
        }
        return 0.0;
    }
}