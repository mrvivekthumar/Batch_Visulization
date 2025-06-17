package com.vivek.service;

import com.vivek.exception.*;
import com.vivek.model.PerformanceTestRecord;
import com.vivek.repository.PerformanceTestRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced Performance Test Service with comprehensive error handling,
 * retry mechanisms, and production-grade service management
 * 
 * @author Vivek
 * @version 2.0.0
 */
@Slf4j
@Service
public class PerformanceTestService {

    private final PerformanceTestRepository repository;
    private final MeterRegistry meterRegistry;

    // Configuration
    @Value("${performance.test.max-records-per-operation:100000}")
    private int maxRecordsPerOperation;

    @Value("${performance.test.max-batch-size:10000}")
    private int maxBatchSize;

    @Value("${performance.test.memory-threshold-mb:1024}")
    private long memoryThresholdMb;

    // Metrics
    private final Counter totalOperationsCounter;
    private final Counter successfulOperationsCounter;
    private final Counter failedOperationsCounter;
    private final Counter deletedRecordsCounter;
    private final Counter insertedRecordsCounter;
    private final Timer batchDeletionTimer;
    private final Timer singleDeletionTimer;
    private final Timer batchInsertionTimer;
    private final Timer singleInsertionTimer;

    // Operation tracking
    private final AtomicInteger activeOperations = new AtomicInteger(0);

    @Autowired
    public PerformanceTestService(PerformanceTestRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.totalOperationsCounter = Counter.builder("performance.operations.total")
                .description("Total number of performance operations")
                .register(meterRegistry);

        this.successfulOperationsCounter = Counter.builder("performance.operations.successful")
                .description("Number of successful operations")
                .register(meterRegistry);

        this.failedOperationsCounter = Counter.builder("performance.operations.failed")
                .description("Number of failed operations")
                .register(meterRegistry);

        this.deletedRecordsCounter = Counter.builder("performance.records.deleted")
                .description("Total number of records deleted")
                .register(meterRegistry);

        this.insertedRecordsCounter = Counter.builder("performance.records.inserted")
                .description("Total number of records inserted")
                .register(meterRegistry);

        this.batchDeletionTimer = Timer.builder("performance.deletion.batch")
                .description("Time taken for batch deletion operations")
                .register(meterRegistry);

        this.singleDeletionTimer = Timer.builder("performance.deletion.single")
                .description("Time taken for single deletion operations")
                .register(meterRegistry);

        this.batchInsertionTimer = Timer.builder("performance.insertion.batch")
                .description("Time taken for batch insertion operations")
                .register(meterRegistry);

        this.singleInsertionTimer = Timer.builder("performance.insertion.single")
                .description("Time taken for single insertion operations")
                .register(meterRegistry);
    }

    /**
     * Smart Insert with enhanced error handling and validation
     */
    // @Transactional(propagation = Propagation.REQUIRED, timeout = 300, rollbackFor
    // = Exception.class)
    // @Retryable(value = { DataAccessException.class }, maxAttempts = 3, backoff =
    // @Backoff(delay = 1000, multiplier = 2))
    @Transactional(timeout = 600)
    public PerformanceResult smartInsert(int totalRecords, int batchSize) {
        String operationId = UUID.randomUUID().toString();
        log.info("📝 [{}] Smart Insert started: {} records with batch size {}",
                operationId, totalRecords, batchSize);

        // Pre-operation validation
        validateInsertOperation(totalRecords, batchSize, operationId);

        // Check system resources
        checkSystemResources(operationId);

        // Track active operations
        activeOperations.incrementAndGet();
        totalOperationsCounter.increment();

        try {
            LocalDateTime startTime = LocalDateTime.now();
            long startMemory = getUsedMemory();

            Timer.Sample sample = Timer.start(meterRegistry);
            Timer timerToUse = batchSize == 1 ? singleInsertionTimer : batchInsertionTimer;

            int totalInserted = 0;
            int operationCount = 0;

            if (batchSize == 1) {
                totalInserted = performSingleInserts(totalRecords, operationId);
                operationCount = totalRecords;
            } else {
                var result = performBatchInserts(totalRecords, batchSize, operationId);
                totalInserted = result.inserted;
                operationCount = result.batches;
            }

            sample.stop(timerToUse);
            insertedRecordsCounter.increment(totalInserted);
            successfulOperationsCounter.increment();

            LocalDateTime endTime = LocalDateTime.now();
            long endMemory = getUsedMemory();
            Duration duration = Duration.between(startTime, endTime);

            PerformanceResult result = PerformanceResult.builder()
                    .testType(batchSize == 1 ? "SINGLE_INSERTION" : "BATCH_INSERTION")
                    .batchSize(batchSize)
                    .recordsProcessed(totalInserted)
                    .durationMs(duration.toMillis())
                    .averageTimePerRecord((double) duration.toMillis() / totalInserted)
                    .memoryUsedMB((endMemory - startMemory) / (1024 * 1024))
                    .recordsPerSecond((double) totalInserted / (duration.toMillis() / 1000.0))
                    .batchCount(operationCount)
                    .startTime(startTime)
                    .endTime(endTime)
                    .operationId(operationId)
                    .build();

            log.info("✅ [{}] Smart insert completed successfully: {} records in {} ms",
                    operationId, totalInserted, duration.toMillis());

            return result;

        } catch (DataAccessException e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Database error during smart insert", operationId, e);
            throw new DatabaseOperationException(
                    "Failed to insert records due to database error: %s", e, e.getMessage());
        } catch (OutOfMemoryError e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Out of memory during smart insert", operationId, e);
            throw new ResourceExhaustedException(
                    "Insufficient memory to complete insert operation", e);
        } catch (Exception e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Unexpected error during smart insert", operationId, e);
            throw new PerformanceOperationException(
                    "Smart insert operation failed unexpectedly", e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    /**
     * Smart Delete with enhanced error handling and validation
     */
    // @Transactional(propagation = Propagation.REQUIRED, timeout = 600, rollbackFor
    // = Exception.class)
    // @Retryable(value = { DataAccessException.class }, maxAttempts = 3, backoff =
    // @Backoff(delay = 1000, multiplier = 2))
    @Transactional(timeout = 900)
    public PerformanceResult smartDelete(int totalRecords, int batchSize) {
        String operationId = UUID.randomUUID().toString();
        log.info("🗑️ [{}] Smart Delete started: {} records with batch size {}",
                operationId, totalRecords, batchSize);

        // Pre-operation validation
        validateDeleteOperation(totalRecords, batchSize, operationId);

        // Check available records
        Long availableRecords = repository.getTotalRecordCount();
        if (availableRecords < totalRecords) {
            totalRecords = availableRecords.intValue();
            log.warn("⚠️ [{}] Only {} records available for deletion, adjusting target",
                    operationId, totalRecords);
        }

        if (totalRecords == 0) {
            throw new InsufficientResourcesException(
                    "No records available for deletion");
        }

        // Track active operations
        activeOperations.incrementAndGet();
        totalOperationsCounter.increment();

        try {
            LocalDateTime startTime = LocalDateTime.now();
            long startMemory = getUsedMemory();

            Timer.Sample sample = Timer.start(meterRegistry);
            Timer timerToUse = batchSize == 1 ? singleDeletionTimer : batchDeletionTimer;

            // Get IDs to delete in chunks to manage memory
            List<Long> idsToDelete = getIdsForDeletion(totalRecords, operationId);

            int totalDeleted = 0;
            int operationCount = 0;

            if (batchSize == 1) {
                totalDeleted = performSingleDeletes(idsToDelete, operationId);
                operationCount = totalDeleted;
            } else {
                var result = performBatchDeletes(idsToDelete, batchSize, operationId);
                totalDeleted = result.deleted;
                operationCount = result.batches;
            }

            sample.stop(timerToUse);
            deletedRecordsCounter.increment(totalDeleted);
            successfulOperationsCounter.increment();

            LocalDateTime endTime = LocalDateTime.now();
            long endMemory = getUsedMemory();
            Duration duration = Duration.between(startTime, endTime);

            PerformanceResult result = PerformanceResult.builder()
                    .testType(batchSize == 1 ? "SINGLE_DELETION" : "BATCH_DELETION")
                    .batchSize(batchSize)
                    .recordsProcessed(totalDeleted)
                    .durationMs(duration.toMillis())
                    .averageTimePerRecord((double) duration.toMillis() / totalDeleted)
                    .memoryUsedMB((endMemory - startMemory) / (1024 * 1024))
                    .recordsPerSecond((double) totalDeleted / (duration.toMillis() / 1000.0))
                    .batchCount(operationCount)
                    .startTime(startTime)
                    .endTime(endTime)
                    .operationId(operationId)
                    .build();

            log.info("✅ [{}] Smart delete completed successfully: {} records in {} ms",
                    operationId, totalDeleted, duration.toMillis());

            return result;

        } catch (DataAccessException e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Database error during smart delete", operationId, e);
            throw new DatabaseOperationException(
                    "Failed to delete records due to database error: %s", e, e.getMessage());
        } catch (Exception e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Unexpected error during smart delete", operationId, e);
            throw new PerformanceOperationException(
                    "Smart delete operation failed unexpectedly", e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    /**
     * Get enhanced database statistics with error handling
     */
    @Retryable(value = { DataAccessException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public DatabaseStats getDatabaseStats() {
        try {
            Long totalRecords = repository.getTotalRecordCount();
            String tableSize = "N/A";
            String connectionInfo = "N/A";

            try {
                tableSize = repository.getTableSize();
            } catch (Exception e) {
                log.warn("Could not retrieve table size: {}", e.getMessage());
            }

            try {
                Integer activeConnections = repository.getActiveConnectionCount();
                connectionInfo = String.format("Active connections: %d", activeConnections);
            } catch (Exception e) {
                log.warn("Could not retrieve connection info: {}", e.getMessage());
            }

            return DatabaseStats.builder()
                    .totalRecords(totalRecords != null ? totalRecords : 0L)
                    .tableSize(tableSize)
                    .connectionInfo(connectionInfo)
                    .timestamp(LocalDateTime.now())
                    .activeOperations(activeOperations.get())
                    .build();

        } catch (DataAccessException e) {
            log.error("Failed to retrieve database statistics", e);
            throw new DatabaseOperationException(
                    "Unable to retrieve database statistics", e);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private void validateInsertOperation(int totalRecords, int batchSize, String operationId) {
        if (totalRecords <= 0) {
            throw new ValidationException("Total records must be positive, got: %d", totalRecords);
        }
        if (totalRecords > maxRecordsPerOperation) {
            throw new ValidationException(
                    "Total records (%d) exceeds maximum allowed (%d)",
                    totalRecords, maxRecordsPerOperation);
        }
        if (batchSize <= 0) {
            throw new ValidationException("Batch size must be positive, got: %d", batchSize);
        }
        if (batchSize > maxBatchSize) {
            throw new ValidationException(
                    "Batch size (%d) exceeds maximum allowed (%d)",
                    batchSize, maxBatchSize);
        }
        if (batchSize > totalRecords) {
            throw new ValidationException(
                    "Batch size (%d) cannot be greater than total records (%d)",
                    batchSize, totalRecords);
        }

        log.debug("✅ [{}] Insert operation validation passed", operationId);
    }

    private void validateDeleteOperation(int totalRecords, int batchSize, String operationId) {
        if (totalRecords <= 0) {
            throw new ValidationException("Total records must be positive, got: %d", totalRecords);
        }
        if (totalRecords > maxRecordsPerOperation) {
            throw new ValidationException(
                    "Total records (%d) exceeds maximum allowed (%d)",
                    totalRecords, maxRecordsPerOperation);
        }
        if (batchSize <= 0) {
            throw new ValidationException("Batch size must be positive, got: %d", batchSize);
        }
        if (batchSize > maxBatchSize) {
            throw new ValidationException(
                    "Batch size (%d) exceeds maximum allowed (%d)",
                    batchSize, maxBatchSize);
        }

        log.debug("✅ [{}] Delete operation validation passed", operationId);
    }

    private void checkSystemResources(String operationId) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);
        double memoryUsagePercent = (double) usedMemoryMb / maxMemoryMb * 100;

        if (memoryUsagePercent > 90) {
            throw new ResourceExhaustedException(
                    "System memory usage too high: %.1f%% (used: %dMB, max: %dMB)",
                    memoryUsagePercent, usedMemoryMb, maxMemoryMb);
        }

        if (activeOperations.get() > 5) {
            throw new ResourceExhaustedException(
                    "Too many active operations: %d. Please wait for current operations to complete.",
                    activeOperations.get());
        }

        log.debug("✅ [{}] System resource check passed: Memory: %.1f%%, Active ops: {}",
                operationId, memoryUsagePercent, activeOperations.get());
    }

    private int performSingleInserts(int totalRecords, String operationId) {
        int inserted = 0;
        for (int i = 0; i < totalRecords; i++) {
            try {
                PerformanceTestRecord record = PerformanceTestRecord.createTestRecord(i);

                // Use JPA save() instead of native SQL query
                PerformanceTestRecord savedRecord = repository.save(record);

                if (savedRecord != null && savedRecord.getId() != null) {
                    inserted++;
                }

                if (i % 1000 == 0 && i > 0) {
                    log.debug("📝 [{}] Inserted {} records (single)", operationId, i);
                }
            } catch (Exception e) {
                log.warn("⚠️ [{}] Failed to insert record {}: {}", operationId, i, e.getMessage());
                // Continue with next record
            }
        }
        return inserted;
    }

    private BatchInsertResult performBatchInserts(int totalRecords, int batchSize, String operationId) {
        int totalInserted = 0;
        int batchCount = 0;

        for (int i = 0; i < totalRecords; i += batchSize) {
            List<PerformanceTestRecord> batch = new ArrayList<>();
            int endIndex = Math.min(i + batchSize, totalRecords);

            for (int j = i; j < endIndex; j++) {
                batch.add(PerformanceTestRecord.createTestRecord(j));
            }

            try {
                // Use JPA saveAll() instead of custom batchSave()
                List<PerformanceTestRecord> savedRecords = repository.saveAll(batch);
                totalInserted += savedRecords.size();
                batchCount++;

                if (batchCount % 10 == 0) {
                    log.debug("📝 [{}] Completed {} batches, {} records",
                            operationId, batchCount, totalInserted);
                }
            } catch (Exception e) {
                log.warn("⚠️ [{}] Failed to insert batch {}: {}",
                        operationId, batchCount, e.getMessage());
            }
        }

        return new BatchInsertResult(totalInserted, batchCount);
    }

    private List<Long> getIdsForDeletion(int totalRecords, String operationId) {
        try {
            // Get IDs in chunks to manage memory
            return repository.findIdsPaginated(totalRecords, 0);
        } catch (Exception e) {
            log.error("❌ [{}] Failed to retrieve IDs for deletion", operationId, e);
            throw new DatabaseOperationException(
                    "Failed to retrieve record IDs for deletion", e);
        }
    }

    private int performSingleDeletes(List<Long> idsToDelete, String operationId) {
        int deleted = 0;
        for (int i = 0; i < idsToDelete.size(); i++) {
            try {
                int result = repository.deleteRecordById(idsToDelete.get(i));
                deleted += result;

                if (i % 1000 == 0 && i > 0) {
                    log.debug("🗑️ [{}] Deleted {} records (single)", operationId, i);
                }
            } catch (Exception e) {
                log.warn("⚠️ [{}] Failed to delete record {}: {}",
                        operationId, idsToDelete.get(i), e.getMessage());
                // Continue with next record
            }
        }
        return deleted;
    }

    private BatchDeleteResult performBatchDeletes(List<Long> idsToDelete, int batchSize, String operationId) {
        int totalDeleted = 0;
        int batchCount = 0;

        for (int i = 0; i < idsToDelete.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, idsToDelete.size());
            List<Long> batchIds = idsToDelete.subList(i, endIndex);

            try {
                int deletedInBatch = repository.batchDeleteByIds(batchIds);
                totalDeleted += deletedInBatch;
                batchCount++;

                if (batchCount % 10 == 0) {
                    log.debug("🗑️ [{}] Deleted {} batches, {} total records",
                            operationId, batchCount, totalDeleted);
                }
            } catch (Exception e) {
                log.warn("⚠️ [{}] Failed to delete batch {}: {}", operationId, batchCount, e.getMessage());
                // Continue with next batch
            }
        }

        return new BatchDeleteResult(totalDeleted, batchCount);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    // ===== HELPER CLASSES =====

    private record BatchInsertResult(int inserted, int batches) {
    }

    private record BatchDeleteResult(int deleted, int batches) {
    }

    /**
     * Enhanced Performance Result Data Transfer Object
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

    /**
     * Enhanced Database Statistics Data Transfer Object
     */
    @lombok.Data
    @lombok.Builder
    public static class DatabaseStats {
        private Long totalRecords;
        private String tableSize;
        private String connectionInfo;
        private LocalDateTime timestamp;
        private int activeOperations;
    }
}