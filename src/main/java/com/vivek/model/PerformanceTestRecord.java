package com.vivek.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance_test_records", indexes = {
                // FIXED: Use actual database column names (lowercase)
                @Index(name = "idx_test_id", columnList = "test_id"),
                @Index(name = "idx_category", columnList = "category"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_composite", columnList = "category, test_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTestRecord {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @Column(name = "test_id", nullable = false)
        private String testId;

        @Column(name = "category", nullable = false, length = 50)
        private String category;

        @Column(name = "description", length = 500)
        private String description;

        @Column(name = "numeric_value")
        private Long numericValue;

        @Column(name = "string_value", length = 255)
        private String stringValue;

        @Column(name = "json_data", columnDefinition = "TEXT")
        private String jsonData;

        @Column(name = "is_active")
        @Builder.Default
        private Boolean isActive = true;

        @Column(name = "priority")
        @Builder.Default
        private Integer priority = 1;

        @Column(name = "tags", length = 1000)
        private String tags;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @Version
        private Long version;

        // Business methods for testing
        public static PerformanceTestRecord createTestRecord(int index) {
                return PerformanceTestRecord.builder()
                                .testId(UUID.randomUUID().toString())
                                .category("BATCH_TEST_" + (index % 10))
                                .description("Performance test record number " + index + " for batch deletion testing")
                                .numericValue((long) (Math.random() * 1000000))
                                .stringValue("TestData_" + index + "_" + System.currentTimeMillis())
                                .jsonData("{\"index\":" + index + ",\"timestamp\":\"" + LocalDateTime.now()
                                                + "\",\"metadata\":{\"batch\":true,\"test\":true}}")
                                .isActive(index % 10 != 0) // 10% inactive records
                                .priority(index % 5 + 1) // Priority 1-5
                                .tags("performance,batch,test,index_" + index)
                                .build();
        }

        public static PerformanceTestRecord createLargeRecord(int index) {
                StringBuilder largeString = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                        largeString.append("LargeData_").append(i).append("_");
                }

                return PerformanceTestRecord.builder()
                                .testId("LARGE_" + UUID.randomUUID().toString())
                                .category("LARGE_RECORD_" + (index % 5))
                                .description("Large performance test record " + index
                                                + " with substantial data for testing")
                                .numericValue((long) index * 1000)
                                .stringValue(largeString.toString())
                                .jsonData("{\"type\":\"large\",\"index\":" + index + ",\"data\":\""
                                                + largeString.toString().substring(0, 200) + "\"}")
                                .isActive(true)
                                .priority(3)
                                .tags("performance,large,batch,memory_test,index_" + index)
                                .build();
        }
}