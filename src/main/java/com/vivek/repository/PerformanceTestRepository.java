package com.vivek.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.model.PerformanceTestRecord;

import jakarta.persistence.QueryHint;
import java.util.List;

/**
 * Enhanced Repository for Performance Test Records
 *
 * Provides optimized database operations with:
 * - Batch processing capabilities
 * - Query optimization hints
 * - Proper transaction management
 * - Memory-efficient operations
 *
 * @author Vivek
 * @version 1.0.0
 */
@Repository
public interface PerformanceTestRepository extends JpaRepository<PerformanceTestRecord, Long> {

        // ===== QUERY OPERATIONS =====

        /**
         * Delete single record by ID with proper transaction handling
         * Returns the number of deleted records (0 or 1)
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 30)
        @Query("DELETE FROM PerformanceTestRecord p WHERE p.id = :id")
        int deleteRecordById(@Param("id") Long id);

        /**
         * Optimized native batch delete for large datasets
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 120)
        @Query(value = "DELETE FROM performance_test_records WHERE id IN (:ids)", nativeQuery = true)
        int batchDeleteByIds(@Param("ids") List<Long> ids);

        // ===== OPTIMIZED ID RETRIEVAL =====

        /**
         * Get paginated IDs for memory-efficient batch processing
         */
        @Query(value = "SELECT p.id FROM performance_test_records p ORDER BY p.id LIMIT :limit OFFSET :offset", nativeQuery = true)
        @QueryHints({
                        @QueryHint(name = "org.hibernate.fetchSize", value = "5000"),
                        @QueryHint(name = "org.hibernate.readOnly", value = "true")
        })
        List<Long> findIdsPaginated(@Param("limit") int limit, @Param("offset") int offset);

        // ===== PERFORMANCE MONITORING QUERIES =====

        /**
         * Get total record count efficiently
         */
        @Query("SELECT COUNT(p) FROM PerformanceTestRecord p")
        Long getTotalRecordCount();

        /**
         * Get database table size information
         */
        @Query(value = "SELECT pg_size_pretty(pg_total_relation_size('performance_test_records')) as table_size", nativeQuery = true)
        String getTableSize();

        // ===== HEALTH CHECK OPERATIONS =====

        /**
         * Simple connectivity test
         */
        @Query(value = "SELECT 1", nativeQuery = true)
        Integer testConnection();

        /**
         * Active connections count
         */
        @Query(value = """
                        SELECT count(*)
                        FROM pg_stat_activity
                        WHERE datname = current_database()
                        AND state = 'active'
                        """, nativeQuery = true)
        Integer getActiveConnectionCount();
}