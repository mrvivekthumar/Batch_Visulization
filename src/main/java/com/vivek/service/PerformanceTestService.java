package com.vivek.service;

import com.vivek.dto.DatabaseStats;
import com.vivek.dto.PerformanceResult;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 300)
    public PerformanceResult smartInsert(int totalRecords, int batchSize) {
        String operationId = UUID.randomUUID().toString();
        log.info("📝 [{}] Smart Insert started: {} records with batch size {}",
                operationId, totalRecords, batchSize);

        validateInsertOperation(totalRecords, batchSize, operationId);
        checkSystemResources(operationId);

        activeOperations.incrementAndGet();
        totalOperationsCounter.increment();

        try {
            LocalDateTime startTime = LocalDateTime.now();
            long startMemory = getUsedMemory();

            Timer.Sample sample = Timer.start(meterRegistry);
            Timer timerToUse = batchSize == 1 ? singleInsertionTimer : batchInsertionTimer;

            int totalInserted;
            int operationCount;

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

            double recordsPerSecond = totalInserted > 0 && duration.toMillis() > 0
                    ? (double) totalInserted / (duration.toMillis() / 1000.0)
                    : 0.0;

            double avgTimePerRecord = totalInserted > 0
                    ? (double) duration.toMillis() / totalInserted
                    : 0.0;

            return PerformanceResult.builder()
                    .testType(batchSize == 1 ? "SINGLE_INSERTION" : "BATCH_INSERTION")
                    .batchSize(batchSize)
                    .recordsProcessed(totalInserted)
                    .durationMs(duration.toMillis())
                    .averageTimePerRecord(avgTimePerRecord)
                    .memoryUsedMB(Math.abs(endMemory - startMemory) / (1024 * 1024))
                    .recordsPerSecond(recordsPerSecond)
                    .batchCount(operationCount)
                    .startTime(startTime)
                    .endTime(endTime)
                    .operationId(operationId)
                    .build();
        } catch (Exception e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Smart insert failed", operationId, e);
            throw new PerformanceOperationException("Smart insert failed: " + e.getMessage(), e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    public PerformanceResult smartDelete(int totalRecords, int batchSize) {
        String operationId = UUID.randomUUID().toString();
        log.info("🗑️ [{}] Smart Delete started: {} records with batch size {}",
                operationId, totalRecords, batchSize);

        validateDeleteOperation(totalRecords, batchSize, operationId);

        Long availableRecords = repository.getTotalRecordCount();
        if (availableRecords < totalRecords) {
            totalRecords = availableRecords.intValue();
            log.warn("⚠️ [{}] Only {} records available for deletion, adjusting target",
                    operationId, totalRecords);
        }

        if (totalRecords == 0) {
            throw new InsufficientResourcesException("No records available for deletion");
        }

        activeOperations.incrementAndGet();
        totalOperationsCounter.increment();

        try {
            LocalDateTime startTime = LocalDateTime.now();
            long startMemory = getUsedMemory();

            Timer.Sample sample = Timer.start(meterRegistry);
            Timer timerToUse = batchSize == 1 ? singleDeletionTimer : batchDeletionTimer;

            List<Long> idsToDelete = getIdsForDeletion(totalRecords, operationId);
            int totalDeleted;
            int operationCount;

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

            double recordsPerSecond = totalDeleted > 0 && duration.toMillis() > 0
                    ? (double) totalDeleted / (duration.toMillis() / 1000.0)
                    : 0.0;

            double avgTimePerRecord = totalDeleted > 0
                    ? (double) duration.toMillis() / totalDeleted
                    : 0.0;

            return PerformanceResult.builder()
                    .testType(batchSize == 1 ? "SINGLE_DELETION" : "BATCH_DELETION")
                    .batchSize(batchSize)
                    .recordsProcessed(totalDeleted)
                    .durationMs(duration.toMillis())
                    .averageTimePerRecord(avgTimePerRecord)
                    .memoryUsedMB(Math.abs(endMemory - startMemory) / (1024 * 1024))
                    .recordsPerSecond(recordsPerSecond)
                    .batchCount(operationCount)
                    .startTime(startTime)
                    .endTime(endTime)
                    .operationId(operationId)
                    .build();
        } catch (DataAccessException e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Database error during smart delete", operationId, e);
            throw new DatabaseOperationException("Failed to delete records due to database error: %s", e,
                    e.getMessage());
        } catch (Exception e) {
            failedOperationsCounter.increment();
            log.error("❌ [{}] Unexpected error during smart delete", operationId, e);
            throw new PerformanceOperationException("Smart delete operation failed unexpectedly", e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

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
            throw new DatabaseOperationException("Unable to retrieve database statistics", e);
        }
    }

    private void validateInsertOperation(int totalRecords, int batchSize, String operationId) {
        if (totalRecords <= 0 || batchSize <= 0 || totalRecords > maxRecordsPerOperation || batchSize > maxBatchSize
                || batchSize > totalRecords) {
            throw new ValidationException("Invalid arguments for insert operation");
        }
    }

    private void validateDeleteOperation(int totalRecords, int batchSize, String operationId) {
        if (totalRecords <= 0 || batchSize <= 0 || totalRecords > maxRecordsPerOperation || batchSize > maxBatchSize) {
            throw new ValidationException("Invalid arguments for delete operation");
        }
    }

    private void checkSystemResources(String operationId) {
        Runtime runtime = Runtime.getRuntime();
        if (((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) > 0.9) {
            throw new ResourceExhaustedException("High memory usage");
        }
    }

    private int performSingleInserts(int totalRecords, String operationId) {
        int inserted = 0;
        for (int i = 0; i < totalRecords; i++) {
            repository.save(PerformanceTestRecord.createTestRecord(i));
            inserted++;
        }
        return inserted;
    }

    private BatchInsertResult performBatchInserts(int totalRecords, int batchSize, String operationId) {
        int totalInserted = 0;
        int batchCount = 0;
        for (int i = 0; i < totalRecords; i += batchSize) {
            List<PerformanceTestRecord> batch = new ArrayList<>();
            for (int j = i; j < i + batchSize && j < totalRecords; j++) {
                batch.add(PerformanceTestRecord.createTestRecord(j));
            }
            totalInserted += repository.saveAll(batch).size();
            batchCount++;
        }
        return new BatchInsertResult(totalInserted, batchCount);
    }

    private List<Long> getIdsForDeletion(int totalRecords, String operationId) {
        return repository.findIdsPaginated(totalRecords, 0);
    }

    private int performSingleDeletes(List<Long> idsToDelete, String operationId) {
        int deleted = 0;
        for (Long id : idsToDelete) {
            deleted += repository.deleteRecordById(id);
        }
        return deleted;
    }

    private BatchDeleteResult performBatchDeletes(List<Long> idsToDelete, int batchSize, String operationId) {
        int totalDeleted = 0;
        int batchCount = 0;
        for (int i = 0; i < idsToDelete.size(); i += batchSize) {
            List<Long> batchIds = idsToDelete.subList(i, Math.min(i + batchSize, idsToDelete.size()));
            totalDeleted += repository.batchDeleteByIds(batchIds);
            batchCount++;
        }
        return new BatchDeleteResult(totalDeleted, batchCount);
    }

    private long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private record BatchInsertResult(int inserted, int batches) {
    }

    private record BatchDeleteResult(int deleted, int batches) {
    }
}