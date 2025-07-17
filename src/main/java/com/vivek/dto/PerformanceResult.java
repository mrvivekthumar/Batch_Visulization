package com.vivek.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerformanceResult {
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
