package com.crm.platform.pipelines.repository;

import com.crm.platform.pipelines.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID>, JpaSpecificationExecutor<PipelineStage> {

    /**
     * Find all stages for a pipeline ordered by display order
     */
    List<PipelineStage> findByPipelineIdOrderByDisplayOrderAsc(UUID pipelineId);

    /**
     * Find active stages for a pipeline
     */
    List<PipelineStage> findByPipelineIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID pipelineId);

    /**
     * Find stage by pipeline and name
     */
    Optional<PipelineStage> findByPipelineIdAndName(UUID pipelineId, String name);

    /**
     * Find closed stages for a pipeline
     */
    List<PipelineStage> findByPipelineIdAndIsClosedTrueOrderByDisplayOrderAsc(UUID pipelineId);

    /**
     * Find won stages for a pipeline
     */
    List<PipelineStage> findByPipelineIdAndIsWonTrueOrderByDisplayOrderAsc(UUID pipelineId);

    /**
     * Check if stage name exists in pipeline
     */
    boolean existsByPipelineIdAndName(UUID pipelineId, String name);

    /**
     * Check if stage name exists in pipeline excluding specific stage
     */
    boolean existsByPipelineIdAndNameAndIdNot(UUID pipelineId, String name, UUID id);

    /**
     * Check if display order exists in pipeline
     */
    boolean existsByPipelineIdAndDisplayOrder(UUID pipelineId, Integer displayOrder);

    /**
     * Check if display order exists in pipeline excluding specific stage
     */
    boolean existsByPipelineIdAndDisplayOrderAndIdNot(UUID pipelineId, Integer displayOrder, UUID id);

    /**
     * Find stage with automation rules
     */
    @Query("SELECT s FROM PipelineStage s " +
           "LEFT JOIN FETCH s.stageAutomationRules ar " +
           "WHERE s.id = :stageId")
    Optional<PipelineStage> findByIdWithAutomationRules(@Param("stageId") UUID stageId);

    /**
     * Find stages by tenant (through pipeline relationship)
     */
    @Query("SELECT s FROM PipelineStage s " +
           "JOIN s.pipeline p " +
           "WHERE p.tenantId = :tenantId " +
           "ORDER BY p.displayOrder ASC, s.displayOrder ASC")
    List<PipelineStage> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find stages with analytics
     */
    @Query("SELECT s FROM PipelineStage s " +
           "LEFT JOIN FETCH s.stageAnalytics a " +
           "WHERE s.pipeline.id = :pipelineId " +
           "ORDER BY s.displayOrder ASC")
    List<PipelineStage> findByPipelineIdWithAnalytics(@Param("pipelineId") UUID pipelineId);

    /**
     * Get stage performance metrics
     */
    @Query("SELECT s.id, s.name, s.defaultProbability, " +
           "COUNT(ar.id) as automationRuleCount " +
           "FROM PipelineStage s " +
           "LEFT JOIN s.stageAutomationRules ar " +
           "WHERE s.pipeline.id = :pipelineId " +
           "GROUP BY s.id, s.name, s.defaultProbability " +
           "ORDER BY s.displayOrder ASC")
    List<Object[]> getStageMetrics(@Param("pipelineId") UUID pipelineId);

    /**
     * Find next stage in pipeline
     */
    @Query("SELECT s FROM PipelineStage s " +
           "WHERE s.pipeline.id = :pipelineId AND s.displayOrder > :currentOrder " +
           "ORDER BY s.displayOrder ASC")
    Optional<PipelineStage> findNextStage(@Param("pipelineId") UUID pipelineId, 
                                         @Param("currentOrder") Integer currentOrder);

    /**
     * Find previous stage in pipeline
     */
    @Query("SELECT s FROM PipelineStage s " +
           "WHERE s.pipeline.id = :pipelineId AND s.displayOrder < :currentOrder " +
           "ORDER BY s.displayOrder DESC")
    Optional<PipelineStage> findPreviousStage(@Param("pipelineId") UUID pipelineId, 
                                             @Param("currentOrder") Integer currentOrder);

    /**
     * Get maximum display order for pipeline
     */
    @Query("SELECT COALESCE(MAX(s.displayOrder), 0) FROM PipelineStage s WHERE s.pipeline.id = :pipelineId")
    Integer getMaxDisplayOrder(@Param("pipelineId") UUID pipelineId);

    /**
     * Find stages by color
     */
    List<PipelineStage> findByPipelineIdAndColor(UUID pipelineId, String color);

    /**
     * Count active stages in pipeline
     */
    long countByPipelineIdAndIsActiveTrue(UUID pipelineId);

    /**
     * Count closed stages in pipeline
     */
    long countByPipelineIdAndIsClosedTrue(UUID pipelineId);
}