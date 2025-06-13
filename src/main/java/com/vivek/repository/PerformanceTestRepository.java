package com.vivek.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.model.PerformanceTestRecord;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
         * Find records by category with query optimization
         */
        @Query("SELECT p FROM PerformanceTestRecord p WHERE p.category = :category")
        @QueryHints({
                        @QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
                        @QueryHint(name = "org.hibernate.readOnly", value = "true")
        })
        List<PerformanceTestRecord> findByCategory(@Param("category") String category);

        /**
         * Find active records with pagination and optimization
         */
        @Query("SELECT p FROM PerformanceTestRecord p WHERE p.isActive = true ORDER BY p.id")
        @QueryHints({
                        @QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
                        @QueryHint(name = "org.hibernate.readOnly", value = "true")
        })
        Page<PerformanceTestRecord> findActiveRecords(Pageable pageable);

        /**
         * Count records by category efficiently
         */
        @Query("SELECT COUNT(p) FROM PerformanceTestRecord p WHERE p.category = :category")
        Long countByCategory(@Param("category") String category);

        /**
         * Find record by test ID with optional result
         */
        @Query("SELECT p FROM PerformanceTestRecord p WHERE p.testId = :testId")
        Optional<PerformanceTestRecord> findByTestId(@Param("testId") String testId);

        // ===== BATCH DELETION OPERATIONS =====

        /**
         * Delete single record by ID with proper transaction handling
         * Returns the number of deleted records (0 or 1)
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 30)
        @Query("DELETE FROM PerformanceTestRecord p WHERE p.id = :id")
        int deleteRecordById(@Param("id") Long id);

        /**
         * Batch delete by IDs with optimized query
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 60)
        @Query("DELETE FROM PerformanceTestRecord p WHERE p.id IN :ids")
        int deleteByIdIn(@Param("ids") List<Long> ids);

        /**
         * Delete records by category
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 60)
        @Query("DELETE FROM PerformanceTestRecord p WHERE p.category = :category")
        int deleteByCategory(@Param("category") String category);

        /**
         * Delete old records with timestamp filter
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 120)
        @Query("DELETE FROM PerformanceTestRecord p WHERE p.createdAt < :cutoffDate")
        int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

        /**
         * Optimized native batch delete for large datasets
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 120)
        @Query(value = "DELETE FROM performance_test_records WHERE id IN (:ids)", nativeQuery = true)
        int batchDeleteByIds(@Param("ids") List<Long> ids);

        // ===== OPTIMIZED ID RETRIEVAL =====

        /**
         * Get all IDs efficiently for batch operations
         */
        @Query("SELECT p.id FROM PerformanceTestRecord p ORDER BY p.id")
        @QueryHints({
                        @QueryHint(name = "org.hibernate.fetchSize", value = "10000"),
                        @QueryHint(name = "org.hibernate.readOnly", value = "true")
        })
        List<Long> findAllIds();

        /**
         * Get paginated IDs for memory-efficient batch processing
         */
        @Query(value = "SELECT p.id FROM performance_test_records p ORDER BY p.id LIMIT :limit OFFSET :offset", nativeQuery = true)
        @QueryHints({
                        @QueryHint(name = "org.hibernate.fetchSize", value = "5000"),
                        @QueryHint(name = "org.hibernate.readOnly", value = "true")
        })
        List<Long> findIdsPaginated(@Param("limit") int limit, @Param("offset") int offset);

        /**
         * Get IDs by category for targeted operations
         */
        @Query("SELECT p.id FROM PerformanceTestRecord p WHERE p.category = :category ORDER BY p.id")
        @QueryHints({
                        @QueryHint(name = "org.hibernate.fetchSize", value = "5000"),
                        @QueryHint(name = "org.hibernate.readOnly", value = "true")
        })
        List<Long> findIdsByCategory(@Param("category") String category);

        // ===== PERFORMANCE MONITORING QUERIES =====

        /**
         * Get total record count efficiently
         */
        @Query("SELECT COUNT(p) FROM PerformanceTestRecord p")
        Long getTotalRecordCount();

        /**
         * Get record statistics by category
         */
        @Query("SELECT p.category, COUNT(p) FROM PerformanceTestRecord p GROUP BY p.category")
        List<Object[]> getRecordCountByCategory();

        /**
         * Get database table size information
         */
        @Query(value = "SELECT pg_size_pretty(pg_total_relation_size('performance_test_records')) as table_size", nativeQuery = true)
        String getTableSize();

        /**
         * Get comprehensive table statistics
         */
        @Query(value = """
                        SELECT
                            pg_stat_get_tuples_inserted(c.oid) as inserts,
                            pg_stat_get_tuples_updated(c.oid) as updates,
                            pg_stat_get_tuples_deleted(c.oid) as deletes,
                            pg_stat_get_tuples_fetched(c.oid) as fetches
                        FROM pg_class c
                        WHERE c.relname = 'performance_test_records'
                        """, nativeQuery = true)
        Object[] getTableStatistics();

        // ===== OPTIMIZED BATCH INSERT OPERATIONS =====

        /**
         * Single record insert with native query for better performance
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional(timeout = 30)
        @Query(value = """
                        INSERT INTO performance_test_records
                        (test_id, category, description, numeric_value, string_value, json_data,
                         is_active, priority, tags, created_at)
                        VALUES
                        (:testId, :category, :description, :numericValue, :stringValue, :jsonData,
                         :isActive, :priority, :tags, NOW())
                        """, nativeQuery = true)
        int insertSingleRecord(
                        @Param("testId") String testId,
                        @Param("category") String category,
                        @Param("description") String description,
                        @Param("numericValue") Long numericValue,
                        @Param("stringValue") String stringValue,
                        @Param("jsonData") String jsonData,
                        @Param("isActive") Boolean isActive,
                        @Param("priority") Integer priority,
                        @Param("tags") String tags);

        // ===== MAINTENANCE OPERATIONS =====

        /**
         * Analyze table statistics for query optimization
         */
        @Modifying
        @Transactional(timeout = 60)
        @Query(value = "ANALYZE performance_test_records", nativeQuery = true)
        void analyzeTable();

        /**
         * Vacuum table for performance optimization
         */
        @Modifying
        @Transactional(timeout = 300)
        @Query(value = "VACUUM ANALYZE performance_test_records", nativeQuery = true)
        void vacuumAnalyzeTable();

        /**
         * Get index usage statistics
         */
        @Query(value = """
                        SELECT
                            schemaname,
                            tablename,
                            indexname,
                            idx_tup_read,
                            idx_tup_fetch
                        FROM pg_stat_user_indexes
                        WHERE tablename = 'performance_test_records'
                        ORDER BY idx_tup_read DESC
                        """, nativeQuery = true)
        List<Object[]> getIndexStatistics();

        // ===== HEALTH CHECK OPERATIONS =====

        /**
         * Simple connectivity test
         */
        @Query(value = "SELECT 1", nativeQuery = true)
        Integer testConnection();

        /**
         * Database version and configuration check
         */
        @Query(value = "SELECT version()", nativeQuery = true)
        String getDatabaseVersion();

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

        // ===== BATCH PROCESSING HELPERS =====

        /**
         * Custom batch save method with proper transaction handling
         * Uses Spring's batch processing with proper flushing
         */
        @Transactional(timeout = 300)
        default void batchSave(List<PerformanceTestRecord> records) {
                int batchSize = 1000;
                for (int i = 0; i < records.size(); i += batchSize) {
                        int endIndex = Math.min(i + batchSize, records.size());
                        List<PerformanceTestRecord> batch = records.subList(i, endIndex);

                        // Save batch
                        saveAll(batch);

                        // Force flush to database
                        flush();

                        // Clear persistence context to prevent memory issues
                        if (i % (batchSize * 5) == 0) {
                                // Clear every 5 batches to manage memory
                                System.gc(); // Suggest garbage collection
                        }
                }
        }

        /**
         * Memory-efficient batch delete with chunking
         */
        @Transactional(timeout = 600)
        default int batchDeleteByIdsChunked(List<Long> ids) {
                int deletedCount = 0;
                int chunkSize = 1000;

                for (int i = 0; i < ids.size(); i += chunkSize) {
                        int endIndex = Math.min(i + chunkSize, ids.size());
                        List<Long> chunk = ids.subList(i, endIndex);

                        deletedCount += batchDeleteByIds(chunk);

                        // Force flush to database
                        flush();
                }

                return deletedCount;
        }
}