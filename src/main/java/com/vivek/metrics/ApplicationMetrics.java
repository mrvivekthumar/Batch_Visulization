package com.vivek.metrics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationMetrics implements MeterBinder {

    private final AtomicInteger activeOperations = new AtomicInteger(0);
    private final AtomicLong totalOperationsCompleted = new AtomicLong(0);
    private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
    private final AtomicLong totalErrorCount = new AtomicLong(0);

    private Counter operationsStartedCounter;
    private Counter operationsCompletedCounter;
    private Counter operationsFailedCounter;
    private Counter recordsProcessedCounter;
    private Timer operationDurationTimer;

    @Override
    public void bindTo(MeterRegistry registry) {
        // Operation counters
        operationsStartedCounter = Counter.builder("performance.operations.started")
                .description("Total number of performance operations started")
                .register(registry);

        operationsCompletedCounter = Counter.builder("performance.operations.completed")
                .description("Total number of performance operations completed successfully")
                .register(registry);

        operationsFailedCounter = Counter.builder("performance.operations.failed")
                .description("Total number of performance operations that failed")
                .register(registry);

        recordsProcessedCounter = Counter.builder("performance.records.processed")
                .description("Total number of records processed across all operations")
                .register(registry);

        // Operation timing
        operationDurationTimer = Timer.builder("performance.operation.duration")
                .description("Duration of performance operations")
                .register(registry);

        // Active operations gauge
        Gauge.builder("performance.operations.active", activeOperations, AtomicInteger::get)
                .description("Number of currently active performance operations")
                .register(registry);

        // Success rate gauge
        Gauge.builder("performance.operations.success.rate", this::getSuccessRate)
                .description("Success rate of performance operations (0-1)")
                .register(registry);

        // Average records per operation
        Gauge.builder("performance.records.per.operation.average", this::getAverageRecordsPerOperation)
                .description("Average number of records processed per operation")
                .register(registry);

        // Error rate gauge
        Gauge.builder("performance.operations.error.rate", this::getErrorRate)
                .description("Error rate of performance operations (0-1)")
                .register(registry);
    }

    // Public methods for service layer to call
    public void incrementOperationsStarted() {
        operationsStartedCounter.increment();
        activeOperations.incrementAndGet();
    }

    public void incrementOperationsCompleted(long recordsProcessed, Duration duration) {
        operationsCompletedCounter.increment();
        recordsProcessedCounter.increment(recordsProcessed);
        operationDurationTimer.record(duration);
        activeOperations.decrementAndGet();
        totalOperationsCompleted.incrementAndGet();
        totalRecordsProcessed.addAndGet(recordsProcessed);
    }

    public void incrementOperationsFailed() {
        operationsFailedCounter.increment();
        activeOperations.decrementAndGet();
        totalErrorCount.incrementAndGet();
    }

    private double getSuccessRate() {
        long completed = totalOperationsCompleted.get();
        long errors = totalErrorCount.get();
        long total = completed + errors;

        if (total == 0)
            return 1.0; // No operations yet, assume healthy
        return (double) completed / total;
    }

    private double getAverageRecordsPerOperation() {
        long completed = totalOperationsCompleted.get();
        long records = totalRecordsProcessed.get();

        if (completed == 0)
            return 0.0;
        return (double) records / completed;
    }

    private double getErrorRate() {
        return 1.0 - getSuccessRate();
    }
}

/**
 * System resource metrics
 */
@Slf4j
class SystemResourceMetrics implements MeterBinder {

    @Override
    public void bindTo(MeterRegistry registry) {
        // JVM Memory metrics (detailed)
        Gauge.builder("jvm.memory.heap.usage.percent", this::getHeapUsagePercent)
                .description("JVM heap memory usage percentage")
                .register(registry);

        Gauge.builder("jvm.memory.non.heap.usage.percent", this::getNonHeapUsagePercent)
                .description("JVM non-heap memory usage percentage")
                .register(registry);

        // Garbage Collection metrics
        Gauge.builder("jvm.gc.overhead.percent", this::getGcOverheadPercent)
                .description("Percentage of time spent in garbage collection")
                .register(registry);

        // Thread metrics
        Gauge.builder("jvm.threads.active", this::getActiveThreadCount)
                .description("Number of active threads")
                .register(registry);

        Gauge.builder("jvm.threads.daemon", this::getDaemonThreadCount)
                .description("Number of daemon threads")
                .register(registry);

        // System load
        Gauge.builder("system.cpu.load.average", this::getSystemLoadAverage)
                .description("System load average")
                .register(registry);

        // File descriptors (if available)
        Gauge.builder("system.file.descriptors.open", this::getOpenFileDescriptors)
                .description("Number of open file descriptors")
                .register(registry);

        // Disk space
        Gauge.builder("system.disk.usage.percent", this::getDiskUsagePercent)
                .description("Disk usage percentage")
                .tag("path", "/")
                .register(registry);
    }

    private double getHeapUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;

        if (max <= 0)
            return 0.0;
        return (double) used / max * 100.0;
    }

    private double getNonHeapUsagePercent() {
        try {
            java.lang.management.MemoryMXBean memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            java.lang.management.MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

            if (nonHeap.getMax() <= 0)
                return 0.0;
            return (double) nonHeap.getUsed() / nonHeap.getMax() * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getGcOverheadPercent() {
        try {
            long totalGcTime = 0;
            for (java.lang.management.GarbageCollectorMXBean gcBean : java.lang.management.ManagementFactory
                    .getGarbageCollectorMXBeans()) {
                totalGcTime += gcBean.getCollectionTime();
            }

            long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
            if (uptime <= 0)
                return 0.0;

            return (double) totalGcTime / uptime * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getActiveThreadCount() {
        return Thread.activeCount();
    }

    private double getDaemonThreadCount() {
        try {
            java.lang.management.ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
            return threadBean.getDaemonThreadCount();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getSystemLoadAverage() {
        try {
            return java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception e) {
            return -1.0;
        }
    }

    private double getOpenFileDescriptors() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory
                    .getOperatingSystemMXBean();

            if (osBean instanceof com.sun.management.UnixOperatingSystemMXBean unixBean) {
                return unixBean.getOpenFileDescriptorCount();
            }
        } catch (Exception e) {
            // Not available on this platform
        }
        return -1.0;
    }

    private double getDiskUsagePercent() {
        try {
            java.io.File root = new java.io.File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();

            if (total <= 0)
                return 0.0;
            return (double) (total - free) / total * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}

/**
 * Metrics collection scheduler
 */
@Slf4j
@Component
class MetricsCollectionScheduler {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private HealthEndpoint healthEndpoint;

    // Collect health status metrics every 30 seconds
    @Scheduled(fixedRate = 30000)
    public void collectHealthMetrics() {
        try {
            var health = healthEndpoint.health();

            // Convert health status to numeric value for easier alerting
            double healthValue = switch (health.getStatus().getCode()) {
                case "UP" -> 1.0;
                case "DOWN" -> 0.0;
                case "WARNING", "UNKNOWN" -> 0.5;
                default -> -1.0;
            };

            Gauge.builder("application.health.status", () -> healthValue)
                    .description("Overall application health status")
                    .register(meterRegistry);

        } catch (Exception e) {
            log.warn("Failed to collect health metrics: {}", e.getMessage());
        }
    }

    // Collect custom application metrics every minute
    @Scheduled(fixedRate = 60000)
    public void collectApplicationMetrics() {
        try {
            // Record current timestamp for monitoring freshness
            Gauge.builder("application.metrics.last.collection", () -> System.currentTimeMillis() / 1000.0)
                    .description("Timestamp of last metrics collection")
                    .register(meterRegistry);

        } catch (Exception e) {
            log.warn("Failed to collect application metrics: {}", e.getMessage());
        }
    }
}
