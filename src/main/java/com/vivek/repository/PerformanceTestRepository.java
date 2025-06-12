package com.vivek.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.model.PerformanceTestRecord;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PerformanceTestRepository extends JpaRepository<PerformanceTestRecord, Long> {

    // Custom queries for performance testing

    @Query("SELECT p FROM PerformanceTestRecord p WHERE p.category = :category")
    List<PerformanceTestRecord> findByCategory(@Param("category") String category);

    @Query("SELECT p FROM PerformanceTestRecord p WHERE p.isActive = true ORDER BY p.id")
    Page<PerformanceTestRecord> findActiveRecords(Pageable pageable);

    @Query("SELECT COUNT(p) FROM PerformanceTestRecord p WHERE p.category = :category")
    Long countByCategory(@Param("category") String category);

    // Batch deletion methods for performance testing

    @Modifying
    @Transactional
    @Query("DELETE FROM PerformanceTestRecord p WHERE p.id = :id")
    void deleteById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerformanceTestRecord p WHERE p.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerformanceTestRecord p WHERE p.category = :category")
    int deleteByCategory(@Param("category") String category);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerformanceTestRecord p WHERE p.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Optimized batch operations

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM performance_test_records WHERE id IN (:ids)", nativeQuery = true)
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    @Query("SELECT p.id FROM PerformanceTestRecord p ORDER BY p.id")
    List<Long> findAllIds();

    @Query(value = "SELECT p.id FROM performance_test_records p ORDER BY p.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findIdsPaginated(@Param("limit") int limit, @Param("offset") int offset);

    // Performance monitoring queries

    @Query("SELECT COUNT(p) FROM PerformanceTestRecord p")
    Long getTotalRecordCount();

    @Query("SELECT p.category, COUNT(p) FROM PerformanceTestRecord p GROUP BY p.category")
    List<Object[]> getRecordCountByCategory();

    @Query(value = "SELECT pg_size_pretty(pg_total_relation_size('performance_test_records')) as table_size", nativeQuery = true)
    String getTableSize();

    @Query(value = "SELECT pg_stat_get_tuples_inserted(c.oid) as inserts, " +
            "pg_stat_get_tuples_updated(c.oid) as updates, " +
            "pg_stat_get_tuples_deleted(c.oid) as deletes " +
            "FROM pg_class c WHERE c.relname = 'performance_test_records'", nativeQuery = true)
    Object[] getTableStatistics();

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO performance_test_records
            (test_id, category, description, numeric_value, string_value, json_data, is_active, priority, tags, created_at)
            VALUES
            (:testId, :category, :description, :numericValue, :stringValue, :jsonData, :isActive, :priority, :tags, NOW())
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

    // Better batch insert using JDBC batch
    default void trueBatchInsert(List<PerformanceTestRecord> records) {
        // This will use Spring's built-in batch optimization
        this.saveAll(records);
        this.flush(); // Force immediate execution
    }

}