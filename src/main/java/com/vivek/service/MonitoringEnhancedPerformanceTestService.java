package com.vivek.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vivek.metrics.ApplicationMetrics;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Enhanced Performance Test Service with comprehensive monitoring integration
 * 
 * This is an update to your existing PerformanceTestService to add monitoring
 */
@Slf4j
@Service
public class MonitoringEnhancedPerformanceTestService {

        @Autowired
        private ApplicationMetrics applicationMetrics;

        @Autowired
        private MeterRegistry meterRegistry;

        /**
         * Enhanced smartInsert method with monitoring
         * Add this monitoring code to your existing smartInsert method
         */
        @Timed(value = "performance.operation.duration", description = "Duration of performance operations")
        public PerformanceResult smartInsertWithMonitoring(int totalRecords, int batchSize) {
                String operationId = java.util.UUID.randomUUID().toString();
                LocalDateTime startTime = LocalDateTime.now();

                // Record operation start
                applicationMetrics.incrementOperationsStarted();
                meterRegistry.counter("performance.operations.started",
                                "type", "insert",
                                "batch_size", String.valueOf(batchSize))
                                .increment();

                log.info("📝 [{}] Smart Insert started: {} records with batch size {} - MONITORING",
                                operationId, totalRecords, batchSize);

                try {
                        // Your existing smartInsert logic goes here...
                        // For demonstration, I'll show the monitoring wrapper

                        Timer.Sample sample = Timer.start(meterRegistry);

                        // Simulate your existing insert logic
                        PerformanceResult result = performInsertWithMonitoring(totalRecords, batchSize, operationId);

                        sample.stop(Timer.builder("performance.insert.duration")
                                        .tag("batch_size", String.valueOf(batchSize))
                                        .tag("operation_id", operationId)
                                        .register(meterRegistry));

                        // Record successful completion
                        LocalDateTime endTime = LocalDateTime.now();
                        Duration duration = Duration.between(startTime, endTime);

                        applicationMetrics.incrementOperationsCompleted(result.getRecordsProcessed(), duration);

                        // Record specific metrics
                        meterRegistry.counter("performance.records.inserted",
                                        "batch_size", String.valueOf(batchSize))
                                        .increment(result.getRecordsProcessed());

                        meterRegistry.timer("performance.throughput",
                                        "operation", "insert")
                                        .record(duration);

                        // Custom gauge for tracking last operation performance
                        meterRegistry.gauge("performance.last.operation.records.per.second",
                                        result.getRecordsPerSecond());

                        log.info("✅ [{}] Smart insert completed successfully: {} records in {} ms - MONITORING",
                                        operationId, result.getRecordsProcessed(), result.getDurationMs());

                        return result;

                } catch (Exception e) {
                        // Record failure
                        applicationMetrics.incrementOperationsFailed();

                        meterRegistry.counter("performance.operations.failed",
                                        "type", "insert",
                                        "batch_size", String.valueOf(batchSize),
                                        "error_type", e.getClass().getSimpleName())
                                        .increment();

                        log.error("❌ [{}] Smart insert failed - MONITORING", operationId, e);
                        throw e;
                }
        }

        /**
         * Enhanced smartDelete method with monitoring
         * Add this monitoring code to your existing smartDelete method
         */
        @Timed(value = "performance.operation.duration", description = "Duration of performance operations")
        public PerformanceResult smartDeleteWithMonitoring(int totalRecords, int batchSize) {
                String operationId = java.util.UUID.randomUUID().toString();
                LocalDateTime startTime = LocalDateTime.now();

                // Record operation start
                applicationMetrics.incrementOperationsStarted();
                meterRegistry.counter("performance.operations.started",
                                "type", "delete",
                                "batch_size", String.valueOf(batchSize))
                                .increment();

                log.info("🗑️ [{}] Smart Delete started: {} records with batch size {} - MONITORING",
                                operationId, totalRecords, batchSize);

                try {
                        Timer.Sample sample = Timer.start(meterRegistry);

                        // Simulate your existing delete logic
                        PerformanceResult result = performDeleteWithMonitoring(totalRecords, batchSize, operationId);

                        sample.stop(Timer.builder("performance.delete.duration")
                                        .tag("batch_size", String.valueOf(batchSize))
                                        .tag("operation_id", operationId)
                                        .register(meterRegistry));

                        // Record successful completion
                        LocalDateTime endTime = LocalDateTime.now();
                        Duration duration = Duration.between(startTime, endTime);

                        applicationMetrics.incrementOperationsCompleted(result.getRecordsProcessed(), duration);

                        // Record specific metrics
                        meterRegistry.counter("performance.records.deleted",
                                        "batch_size", String.valueOf(batchSize))
                                        .increment(result.getRecordsProcessed());

                        // Track deletion efficiency
                        meterRegistry.gauge("performance.last.deletion.efficiency",
                                        result.getRecordsProcessed() / (double) result.getDurationMs() * 1000);

                        log.info("✅ [{}] Smart delete completed successfully: {} records in {} ms - MONITORING",
                                        operationId, result.getRecordsProcessed(), result.getDurationMs());

                        return result;

                } catch (Exception e) {
                        // Record failure
                        applicationMetrics.incrementOperationsFailed();

                        meterRegistry.counter("performance.operations.failed",
                                        "type", "delete",
                                        "batch_size", String.valueOf(batchSize),
                                        "error_type", e.getClass().getSimpleName())
                                        .increment();

                        log.error("❌ [{}] Smart delete failed - MONITORING", operationId, e);
                        throw e;
                }
        }

        // Mock methods to represent your existing logic
        private PerformanceResult performInsertWithMonitoring(int totalRecords, int batchSize, String operationId) {
                // This is where your actual insert logic would go
                // I'm just creating a mock result for demonstration

                try {
                        Thread.sleep(100); // Simulate work
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }

                return PerformanceResult.builder()
                                .testType("BATCH_INSERTION")
                                .batchSize(batchSize)
                                .recordsProcessed(totalRecords)
                                .durationMs(100)
                                .recordsPerSecond(totalRecords / 0.1)
                                .operationId(operationId)
                                .build();
        }

        private PerformanceResult performDeleteWithMonitoring(int totalRecords, int batchSize, String operationId) {
                // This is where your actual delete logic would go
                // I'm just creating a mock result for demonstration

                try {
                        Thread.sleep(150); // Simulate work
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }

                return PerformanceResult.builder()
                                .testType("BATCH_DELETION")
                                .batchSize(batchSize)
                                .recordsProcessed(totalRecords)
                                .durationMs(150)
                                .recordsPerSecond(totalRecords / 0.15)
                                .operationId(operationId)
                                .build();
        }

        /**
         * Performance Result with operation ID for monitoring
         */
        @lombok.Data
        @lombok.Builder
        public static class PerformanceResult {
                private String testType;
                private int batchSize;
                private int recordsProcessed;
                private long durationMs;
                private double averageTimePerRecord;
                private long memoryUsedMB;
                private double recordsPerSecond;
                private int batchCount;
                private LocalDateTime startTime;
                private LocalDateTime endTime;
                private String operationId;
        }
}

/**
 * Instructions for integrating with your existing service:
 * 
 * 1. Add these annotations to your existing smartInsert and smartDelete
 * methods:
 * 
 * @Timed(name = "performance.operation.duration", description = "Duration of
 *             performance operations")
 * 
 *             2. Add these at the beginning of your methods:
 *             applicationMetrics.incrementOperationsStarted();
 *             Timer.Sample sample = Timer.start(meterRegistry);
 * 
 *             3. Add these in your success path:
 *             sample.stop(Timer.builder("performance.insert.duration").register(meterRegistry));
 *             applicationMetrics.incrementOperationsCompleted(result.getRecordsProcessed(),
 *             duration);
 * 
 *             4. Add these in your catch blocks:
 *             applicationMetrics.incrementOperationsFailed();
 *             meterRegistry.counter("performance.operations.failed").increment();
 * 
 *             5. Inject these dependencies in your existing service:
 * @Autowired private ApplicationMetrics applicationMetrics;
 * @Autowired private MeterRegistry meterRegistry;
 */