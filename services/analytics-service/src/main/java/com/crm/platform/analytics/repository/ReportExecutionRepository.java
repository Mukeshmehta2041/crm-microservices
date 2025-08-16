package com.crm.platform.analytics.repository;

import com.crm.platform.analytics.entity.ReportExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, Long> {

    Page<ReportExecution> findByReportIdOrderByExecutedAtDesc(Long reportId, Pageable pageable);

    List<ReportExecution> findByReportIdAndStatusOrderByExecutedAtDesc(Long reportId, ReportExecution.ExecutionStatus status);

    @Query("SELECT re FROM ReportExecution re WHERE re.report.organizationId = :organizationId " +
           "AND re.executedAt >= :startDate AND re.executedAt <= :endDate " +
           "ORDER BY re.executedAt DESC")
    Page<ReportExecution> findExecutionsByOrganizationAndDateRange(
            @Param("organizationId") String organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(re) FROM ReportExecution re WHERE re.report.organizationId = :organizationId " +
           "AND re.status = :status AND re.executedAt >= :startDate")
    long countByOrganizationAndStatusSince(
            @Param("organizationId") String organizationId,
            @Param("status") ReportExecution.ExecutionStatus status,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT AVG(re.executionTimeMs) FROM ReportExecution re WHERE re.report.id = :reportId " +
           "AND re.status = 'COMPLETED' AND re.executedAt >= :startDate")
    Double getAverageExecutionTime(@Param("reportId") Long reportId, @Param("startDate") LocalDateTime startDate);
}