package com.crm.platform.pipelines.repository;

import com.crm.platform.pipelines.entity.MetricType;
import com.crm.platform.pipelines.entity.PipelineAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineAnalyticsRepository extends JpaRepository<PipelineAnalytics, UUID>, JpaSpecificationExecutor<PipelineAnalytics> {

    /**
     * Find analytics for a pipeline
     */
    List<PipelineAnalytics> findByPipelineIdOrderByCalculatedAtDesc(UUID pipelineId);

    /**
     * Find analytics for a stage
     */
    List<PipelineAnalytics> findByStageIdOrderByCalculatedAtDesc(UUID stageId);

    /**
     * Find analytics by tenant
     */
    List<PipelineAnalytics> findByTenantIdOrderByCalculatedAtDesc(UUID tenantId);

    /**
     * Find analytics by metric type
     */
    List<PipelineAnalytics> findByTenantIdAndMetricTypeOrderByCalculatedAtDesc(UUID tenantId, MetricType metricType);

    /**
     * Find analytics for pipeline by metric type
     */
    List<PipelineAnalytics> findByPipelineIdAndMetricTypeOrderByCalculatedAtDesc(UUID pipelineId, MetricType metricType);

    /**
     * Find analytics for stage by metric type
     */
    List<PipelineAnalytics> findByStageIdAndMetricTypeOrderByCalculatedAtDesc(UUID stageId, MetricType metricType);

    /**
     * Find analytics in date range
     */
    List<PipelineAnalytics> findByTenantIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCalculatedAtDesc(
            UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find pipeline analytics in date range
     */
    List<PipelineAnalytics> findByPipelineIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCalculatedAtDesc(
            UUID pipelineId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find stage analytics in date range
     */
    List<PipelineAnalytics> findByStageIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCalculatedAtDesc(
            UUID stageId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find latest analytics for pipeline
     */
    @Query("SELECT pa FROM PipelineAnalytics pa WHERE pa.pipeline.id = :pipelineId AND " +
           "pa.calculatedAt = (SELECT MAX(pa2.calculatedAt) FROM PipelineAnalytics pa2 WHERE pa2.pipeline.id = :pipelineId AND pa2.metricType = pa.metricType)")
    List<PipelineAnalytics> findLatestByPipelineId(@Param("pipelineId") UUID pipelineId);

    /**
     * Find latest analytics for stage
     */
    @Query("SELECT pa FROM PipelineAnalytics pa WHERE pa.stage.id = :stageId AND " +
           "pa.calculatedAt = (SELECT MAX(pa2.calculatedAt) FROM PipelineAnalytics pa2 WHERE pa2.stage.id = :stageId AND pa2.metricType = pa.metricType)")
    List<PipelineAnalytics> findLatestByStageId(@Param("stageId") UUID stageId);

    /**
     * Find specific metric for pipeline in period
     */
    Optional<PipelineAnalytics> findByPipelineIdAndMetricTypeAndPeriodStartAndPeriodEnd(
            UUID pipelineId, MetricType metricType, LocalDateTime periodStart, LocalDateTime periodEnd);

    /**
     * Find specific metric for stage in period
     */
    Optional<PipelineAnalytics> findByStageIdAndMetricTypeAndPeriodStartAndPeriodEnd(
            UUID stageId, MetricType metricType, LocalDateTime periodStart, LocalDateTime periodEnd);

    /**
     * Get pipeline performance summary
     */
    @Query("SELECT pa.metricType, AVG(pa.metricValue), MIN(pa.metricValue), MAX(pa.metricValue) " +
           "FROM PipelineAnalytics pa " +
           "WHERE pa.pipeline.id = :pipelineId AND pa.periodStart >= :startDate AND pa.periodEnd <= :endDate " +
           "GROUP BY pa.metricType")
    List<Object[]> getPipelinePerformanceSummary(@Param("pipelineId") UUID pipelineId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get stage performance summary
     */
    @Query("SELECT pa.metricType, AVG(pa.metricValue), MIN(pa.metricValue), MAX(pa.metricValue) " +
           "FROM PipelineAnalytics pa " +
           "WHERE pa.stage.id = :stageId AND pa.periodStart >= :startDate AND pa.periodEnd <= :endDate " +
           "GROUP BY pa.metricType")
    List<Object[]> getStagePerformanceSummary(@Param("stageId") UUID stageId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Get tenant analytics summary
     */
    @Query("SELECT p.id, p.name, pa.metricType, AVG(pa.metricValue) " +
           "FROM PipelineAnalytics pa " +
           "JOIN pa.pipeline p " +
           "WHERE pa.tenantId = :tenantId AND pa.periodStart >= :startDate AND pa.periodEnd <= :endDate " +
           "GROUP BY p.id, p.name, pa.metricType " +
           "ORDER BY p.name, pa.metricType")
    List<Object[]> getTenantAnalyticsSummary(@Param("tenantId") UUID tenantId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find analytics calculated after date
     */
    List<PipelineAnalytics> findByTenantIdAndCalculatedAtAfterOrderByCalculatedAtDesc(UUID tenantId, LocalDateTime since);

    /**
     * Delete old analytics
     */
    void deleteByTenantIdAndCalculatedAtBefore(UUID tenantId, LocalDateTime before);

    /**
     * Count analytics for pipeline
     */
    long countByPipelineId(UUID pipelineId);

    /**
     * Count analytics for stage
     */
    long countByStageId(UUID stageId);

    /**
     * Count analytics by metric type
     */
    long countByTenantIdAndMetricType(UUID tenantId, MetricType metricType);

    /**
     * Get metric trends
     */
    @Query("SELECT pa.periodStart, pa.metricValue " +
           "FROM PipelineAnalytics pa " +
           "WHERE pa.pipeline.id = :pipelineId AND pa.metricType = :metricType " +
           "ORDER BY pa.periodStart ASC")
    List<Object[]> getMetricTrend(@Param("pipelineId") UUID pipelineId, @Param("metricType") MetricType metricType);

    /**
     * Get stage metric trends
     */
    @Query("SELECT pa.periodStart, pa.metricValue " +
           "FROM PipelineAnalytics pa " +
           "WHERE pa.stage.id = :stageId AND pa.metricType = :metricType " +
           "ORDER BY pa.periodStart ASC")
    List<Object[]> getStageMetricTrend(@Param("stageId") UUID stageId, @Param("metricType") MetricType metricType);
}