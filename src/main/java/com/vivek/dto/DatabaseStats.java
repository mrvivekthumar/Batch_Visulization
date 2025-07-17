package com.vivek.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseStats {
    private Long totalRecords;
    private String tableSize;
    private String connectionInfo;
    private LocalDateTime timestamp;
    private int activeOperations;
}
