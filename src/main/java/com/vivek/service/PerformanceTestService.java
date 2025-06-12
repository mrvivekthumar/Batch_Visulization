package com.vivek.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.model.PerformanceTestRecord;
import com.vivek.repository.PerformanceTestRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PerformanceTestService {

    @Autowired
    private PerformanceTestRepository repository;

    @Autowired
    private MeterRegistry meterRegistry;

    // Custom metrics
    private final Counter totalOperationsCounter;
    private final Counter deletedRecordsCounter;
    private final Timer batchDeletionTimer;
    private final Timer singleDeletionTimer;

    public PerformanceTestService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.totalOperationsCounter = Counter.builder("performance.operations.total")
                .description("Total number of performance operations")
                .register(meterRegistry);

        this.deletedRecordsCounter = Counter.builder("performance.records.deleted")
                .description("Total number of records deleted")
                .register(meterRegistry);

        this.batchDeletionTimer = Timer.builder("performance.deletion.batch")
                .description("Time taken for batch deletion operations")
                .register(meterRegistry);

        this.singleDeletionTimer = Timer.builder("performance.deletion.single")
                .description("Time taken for single deletion operations")
                .register(meterRegistry);
    }

    /**
     * Initialize test data with specified number of records
     */
    @Transactional
    public void initializeTestData(int totalRecords) {
        log.info("🚀 Initializing {} test records...", totalRecords);

        Timer.Sample sample = Timer.start(meterRegistry);

        // just add more
        log.info("📊 Current database records: {}", repository.count());

        // Create test records in batches
        int batchSize = 1000;
        for (int i = 0; i < totalRecords; i += batchSize) {
            List<PerformanceTestRecord> batch = new ArrayList<>();
            int endIndex = Math.min(i + batchSize, totalRecords);

            for (int j = i; j < endIndex; j++) {
                batch.add(PerformanceTestRecord.createTestRecord(j));
            }

            repository.saveAll(batch);

            if (i % 10000 == 0) {
                log.info("📝 Inserted {} records...", i);
            }
        }

        sample.stop(Timer.builder("performance.initialization")
                .description("Time to initialize test data")
                .register(meterRegistry));

        long finalCount = repository.count();
        log.info("✅ Test data initialization complete. Total records: {}", finalCount);
    }

    /**
     * Test batch insertion performance
     */
    @Transactional
    public PerformanceResult testBatchInsertion(int totalRecords, int batchSize) {
        log.info("🚀 Testing batch insertion: {} records in batches of {}", totalRecords, batchSize);

        LocalDateTime startTime = LocalDateTime.now();
        long startMemory = getUsedMemory();

        Timer.Sample sample = Timer.start(meterRegistry);

        int totalInserted = 0;
        int batchCount = 0;

        for (int i = 0; i < totalRecords; i += batchSize) {
            List<PerformanceTestRecord> batch = new ArrayList<>();
            int endIndex = Math.min(i + batchSize, totalRecords);

            for (int j = i; j < endIndex; j++) {
                batch.add(PerformanceTestRecord.createTestRecord(j + (int) (System.currentTimeMillis() % 10000)));
            }

            repository.saveAll(batch);
            totalInserted += batch.size();
            batchCount++;

            if (batchCount % 10 == 0) {
                log.info("📝 Inserted {} batches, {} total records", batchCount, totalInserted);
            }
        }

        sample.stop(Timer.builder("performance.insertion.batch")
                .description("Time taken for batch insertion operations")
                .register(meterRegistry));

        LocalDateTime endTime = LocalDateTime.now();
        long endMemory = getUsedMemory();
        Duration duration = Duration.between(startTime, endTime);

        PerformanceResult result = PerformanceResult.builder()
                .testType("BATCH_INSERTION")
                .batchSize(batchSize)
                .recordsProcessed(totalInserted)
                .durationMs(duration.toMillis())
                .averageTimePerRecord((double) duration.toMillis() / totalInserted)
                .memoryUsedMB((endMemory - startMemory) / (1024 * 1024))
                .recordsPerSecond((double) totalInserted / (duration.toMillis() / 1000.0))
                .batchCount(batchCount)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        log.info("✅ Batch insertion completed: {} records in {} batches, took {} ms",
                totalInserted, batchCount, duration.toMillis());

        return result;
    }

    /**
     * Smart Insert - Insert based on batch size
     * If batchSize = 1: Insert one by one
     * If batchSize > 1: Insert in batches
     */
    @Transactional
    public PerformanceResult smartInsert(int totalRecords, int batchSize) {
        log.info("📝 Smart Insert: {} records with batch size {}", totalRecords, batchSize);

        LocalDateTime startTime = LocalDateTime.now();
        long startMemory = getUsedMemory();
        long startDbCount = repository.count();

        Timer.Sample sample = Timer.start(meterRegistry);

        int totalInserted = 0;
        int operationCount = 0;

        if (batchSize == 1) {
            // Insert one by one using native query
            for (int i = 0; i < totalRecords; i++) {
                PerformanceTestRecord record = PerformanceTestRecord.createTestRecord(i);

                // Use the new single insert method
                repository.insertSingleRecord(
                        record.getTestId(),
                        record.getCategory(),
                        record.getDescription(),
                        record.getNumericValue(),
                        record.getStringValue(),
                        record.getJsonData(),
                        record.getIsActive(),
                        record.getPriority(),
                        record.getTags());

                totalInserted++;
                operationCount++;

                if (i % 1000 == 0 && i > 0) {
                    log.info("📝 Inserted {} records one by one", i);
                }
            }
        } else {
            // Insert in batches using optimized batch method
            for (int i = 0; i < totalRecords; i += batchSize) {
                List<PerformanceTestRecord> batch = new ArrayList<>();
                int endIndex = Math.min(i + batchSize, totalRecords);

                for (int j = i; j < endIndex; j++) {
                    batch.add(PerformanceTestRecord.createTestRecord(j));
                }

                // Use the improved batch insert
                repository.trueBatchInsert(batch);

                totalInserted += batch.size();
                operationCount++;

                if (operationCount % 10 == 0) {
                    log.info("📝 Inserted {} batches, {} total records", operationCount, totalInserted);
                }
            }
        }

        sample.stop(Timer.builder("performance.insertion")
                .description("Time taken for insertion operations")
                .register(meterRegistry));

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
                .build();

        log.info("✅ Smart insert completed: {} records, took {} ms", totalInserted, duration.toMillis());
        return result;
    }

    /**
     * Smart Delete - Delete based on batch size
     * If batchSize = 1: Delete one by one
     * If batchSize > 1: Delete in batches
     */
    @Transactional
    public PerformanceResult smartDelete(int totalRecords, int batchSize) {
        log.info("🗑️ Smart Delete: {} records with batch size {}", totalRecords, batchSize);

        List<Long> availableIds = repository.findIdsPaginated(totalRecords, 0);

        if (availableIds.size() < totalRecords) {
            totalRecords = availableIds.size();
            log.warn("⚠️ Only {} records available for deletion", totalRecords);
        }

        LocalDateTime startTime = LocalDateTime.now();
        long startMemory = getUsedMemory();

        Timer.Sample sample = Timer.start(meterRegistry);

        int totalDeleted = 0;
        int operationCount = 0;

        if (batchSize == 1) {
            // Delete one by one
            for (int i = 0; i < totalRecords; i++) {
                repository.deleteById(availableIds.get(i));
                totalDeleted++;
                operationCount++;
                deletedRecordsCounter.increment();

                if (i % 1000 == 0 && i > 0) {
                    log.info("🗑️ Deleted {} records one by one", i);
                }
            }
        } else {
            // Delete in batches
            for (int i = 0; i < totalRecords; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalRecords);
                List<Long> batchIds = availableIds.subList(i, endIndex);

                int deletedInBatch = repository.batchDeleteByIds(batchIds);
                totalDeleted += deletedInBatch;
                operationCount++;
                deletedRecordsCounter.increment(deletedInBatch);

                if (operationCount % 10 == 0) {
                    log.info("🗑️ Deleted {} batches, {} total records", operationCount, totalDeleted);
                }
            }
        }

        sample.stop(Timer.builder("performance.deletion")
                .description("Time taken for deletion operations")
                .register(meterRegistry));

        totalOperationsCounter.increment();

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
                .build();

        log.info("✅ Smart delete completed: {} records, took {} ms", totalDeleted, duration.toMillis());
        return result;
    }

    /**
     * Test single record deletion performance
     */
    public PerformanceResult testSingleRecordDeletion(int numberOfRecords) {
        log.info("🔥 Testing single record deletion for {} records...", numberOfRecords);

        List<Long> recordIds = repository.findIdsPaginated(numberOfRecords, 0);

        if (recordIds.size() < numberOfRecords) {
            throw new RuntimeException("Not enough records available for testing");
        }

        LocalDateTime startTime = LocalDateTime.now();
        long startMemory = getUsedMemory();

        Timer.Sample sample = Timer.start(meterRegistry);

        int deletedCount = 0;
        for (Long id : recordIds) {
            repository.deleteById(id);
            deletedCount++;
            deletedRecordsCounter.increment();
        }

        sample.stop(singleDeletionTimer);
        totalOperationsCounter.increment();

        LocalDateTime endTime = LocalDateTime.now();
        long endMemory = getUsedMemory();
        Duration duration = Duration.between(startTime, endTime);

        PerformanceResult result = PerformanceResult.builder()
                .testType("SINGLE_DELETION")
                .batchSize(1)
                .recordsProcessed(deletedCount)
                .durationMs(duration.toMillis())
                .averageTimePerRecord((double) duration.toMillis() / deletedCount)
                .memoryUsedMB((endMemory - startMemory) / (1024 * 1024))
                .recordsPerSecond((double) deletedCount / (duration.toMillis() / 1000.0))
                .startTime(startTime)
                .endTime(endTime)
                .build();

        log.info("📊 Single deletion completed: {} records in {} ms",
                deletedCount, duration.toMillis());

        return result;
    }

    /**
     * Test batch deletion performance
     */
    @Transactional
    public PerformanceResult testBatchDeletion(int totalRecords, int batchSize) {
        log.info("🚀 Testing batch deletion: {} records in batches of {}", totalRecords, batchSize);

        List<Long> allIds = repository.findIdsPaginated(totalRecords, 0);

        if (allIds.size() < totalRecords) {
            throw new RuntimeException("Not enough records available for testing");
        }

        LocalDateTime startTime = LocalDateTime.now();
        long startMemory = getUsedMemory();

        Timer.Sample sample = Timer.start(meterRegistry);

        int totalDeleted = 0;
        int batchCount = 0;

        for (int i = 0; i < allIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, allIds.size());
            List<Long> batchIds = allIds.subList(i, endIndex);

            int deletedInBatch = repository.batchDeleteByIds(batchIds);
            totalDeleted += deletedInBatch;
            batchCount++;

            deletedRecordsCounter.increment(deletedInBatch);

            if (batchCount % 10 == 0) {
                log.info("🗑️ Processed {} batches, deleted {} records", batchCount, totalDeleted);
            }
        }

        sample.stop(batchDeletionTimer);
        totalOperationsCounter.increment();

        LocalDateTime endTime = LocalDateTime.now();
        long endMemory = getUsedMemory();
        Duration duration = Duration.between(startTime, endTime);

        PerformanceResult result = PerformanceResult.builder()
                .testType("BATCH_DELETION")
                .batchSize(batchSize)
                .recordsProcessed(totalDeleted)
                .durationMs(duration.toMillis())
                .averageTimePerRecord((double) duration.toMillis() / totalDeleted)
                .memoryUsedMB((endMemory - startMemory) / (1024 * 1024))
                .recordsPerSecond((double) totalDeleted / (duration.toMillis() / 1000.0))
                .batchCount(batchCount)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        log.info("✅ Batch deletion completed: {} records in {} batches, took {} ms",
                totalDeleted, batchCount, duration.toMillis());

        return result;
    }

    /**
     * Get current memory usage
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Get database statistics
     */
    public DatabaseStats getDatabaseStats() {
        Long totalRecords = repository.getTotalRecordCount();
        String tableSize = "N/A";

        try {
            tableSize = repository.getTableSize();
        } catch (Exception e) {
            log.warn("Could not retrieve table size: {}", e.getMessage());
        }

        return DatabaseStats.builder()
                .totalRecords(totalRecords != null ? totalRecords : 0L)
                .tableSize(tableSize)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Performance Result Data Transfer Object
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
    }

    /**
     * Database Statistics Data Transfer Object
     */
    @lombok.Data
    @lombok.Builder
    public static class DatabaseStats {
        private Long totalRecords;
        private String tableSize;
        private LocalDateTime timestamp;
    }
}