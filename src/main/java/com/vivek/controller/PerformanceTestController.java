package com.vivek.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vivek.service.PerformanceTestService;
import com.vivek.service.PerformanceTestService.DatabaseStats;
import com.vivek.service.PerformanceTestService.PerformanceResult;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/performance")
@CrossOrigin(origins = "*")
public class PerformanceTestController {

    @Autowired
    private PerformanceTestService performanceTestService;
    

    /**
     * Smart Initialize - Insert records based on batch size
     * If batchSize = 1: Insert one by one
     * If batchSize > 1: Insert in batches
     */
    @PostMapping("/initialize")
    public ResponseEntity<PerformanceResult> smartInitialize(
            @RequestParam(defaultValue = "1000") int totalRecords,
            @RequestParam(defaultValue = "1") int batchSize) {

        log.info("📝 Smart Initialize: {} records with batch size {}", totalRecords, batchSize);

        try {
            PerformanceResult result = performanceTestService.smartInsert(totalRecords, batchSize);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Smart initialize failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Smart Delete - Delete records based on batch size
     * If batchSize = 1: Delete one by one
     * If batchSize > 1: Delete in batches
     */
    @PostMapping("/delete")
    public ResponseEntity<PerformanceResult> smartDelete(
            @RequestParam(defaultValue = "1000") int totalRecords,
            @RequestParam(defaultValue = "1") int batchSize) {

        log.info("🗑️ Smart Delete: {} records with batch size {}", totalRecords, batchSize);

        try {
            PerformanceResult result = performanceTestService.smartDelete(totalRecords, batchSize);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Smart delete failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get database statistics
     */
    @GetMapping("/stats/database")
    public ResponseEntity<DatabaseStats> getDatabaseStats() {
        try {
            DatabaseStats stats = performanceTestService.getDatabaseStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Failed to get database stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get system information
     */
    @GetMapping("/stats/system")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemStats = new HashMap<>();

            systemStats.put("availableProcessors", runtime.availableProcessors());
            systemStats.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
            systemStats.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
            systemStats.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
            systemStats.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
            systemStats.put("javaVersion", System.getProperty("java.version"));
            systemStats.put("osName", System.getProperty("os.name"));
            systemStats.put("osVersion", System.getProperty("os.version"));

            return ResponseEntity.ok(systemStats);
        } catch (Exception e) {
            log.error("❌ Failed to get system stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Performance Test API");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(health);
    }
}