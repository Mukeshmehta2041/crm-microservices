package com.crm.platform.pipelines.repository;

import com.crm.platform.pipelines.entity.AutomationRule;
import com.crm.platform.pipelines.entity.TriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID>, JpaSpecificationExecutor<AutomationRule> {

    /**
     * Find all automation rules for a tenant
     */
    List<AutomationRule> findByTenantIdOrderByExecutionOrderAsc(UUID tenantId);

    /**
     * Find active automation rules for a tenant
     */
    List<AutomationRule> findByTenantIdAndIsActiveTrueOrderByExecutionOrderAsc(UUID tenantId);

    /**
     * Find automation rules for a pipeline
     */
    List<AutomationRule> findByPipelineIdOrderByExecutionOrderAsc(UUID pipelineId);

    /**
     * Find active automation rules for a pipeline
     */
    List<AutomationRule> findByPipelineIdAndIsActiveTrueOrderByExecutionOrderAsc(UUID pipelineId);

    /**
     * Find automation rules for a stage
     */
    List<AutomationRule> findByStageIdOrderByExecutionOrderAsc(UUID stageId);

    /**
     * Find active automation rules for a stage
     */
    List<AutomationRule> findByStageIdAndIsActiveTrueOrderByExecutionOrderAsc(UUID stageId);

    /**
     * Find automation rules by trigger type
     */
    List<AutomationRule> findByTenantIdAndTriggerTypeAndIsActiveTrueOrderByExecutionOrderAsc(
            UUID tenantId, TriggerType triggerType);

    /**
     * Find automation rules by trigger type for pipeline
     */
    List<AutomationRule> findByPipelineIdAndTriggerTypeAndIsActiveTrueOrderByExecutionOrderAsc(
            UUID pipelineId, TriggerType triggerType);

    /**
     * Find automation rules by trigger type for stage
     */
    List<AutomationRule> findByStageIdAndTriggerTypeAndIsActiveTrueOrderByExecutionOrderAsc(
            UUID stageId, TriggerType triggerType);

    /**
     * Find automation rule by tenant and name
     */
    Optional<AutomationRule> findByTenantIdAndName(UUID tenantId, String name);

    /**
     * Check if automation rule name exists for tenant
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Check if automation rule name exists for tenant excluding specific rule
     */
    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);

    /**
     * Search automation rules by name
     */
    @Query("SELECT ar FROM AutomationRule ar WHERE ar.tenantId = :tenantId AND " +
           "LOWER(ar.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY ar.executionOrder ASC")
    Page<AutomationRule> searchByName(@Param("tenantId") UUID tenantId, 
                                     @Param("searchTerm") String searchTerm, 
                                     Pageable pageable);

    /**
     * Find rules with high error rates
     */
    @Query("SELECT ar FROM AutomationRule ar WHERE ar.tenantId = :tenantId AND " +
           "ar.executionCount > 0 AND " +
           "(CAST(ar.errorCount AS double) / CAST(ar.executionCount AS double)) > :errorThreshold " +
           "ORDER BY (CAST(ar.errorCount AS double) / CAST(ar.executionCount AS double)) DESC")
    List<AutomationRule> findRulesWithHighErrorRate(@Param("tenantId") UUID tenantId, 
                                                   @Param("errorThreshold") double errorThreshold);

    /**
     * Find recently executed rules
     */
    List<AutomationRule> findByTenantIdAndLastExecutedAtAfterOrderByLastExecutedAtDesc(
            UUID tenantId, LocalDateTime since);

    /**
     * Find rules that haven't been executed recently
     */
    @Query("SELECT ar FROM AutomationRule ar WHERE ar.tenantId = :tenantId AND " +
           "ar.isActive = true AND " +
           "(ar.lastExecutedAt IS NULL OR ar.lastExecutedAt < :threshold)")
    List<AutomationRule> findInactiveRules(@Param("tenantId") UUID tenantId, 
                                          @Param("threshold") LocalDateTime threshold);

    /**
     * Get automation rule statistics
     */
    @Query("SELECT ar.triggerType, COUNT(ar.id), " +
           "SUM(ar.executionCount), SUM(ar.errorCount) " +
           "FROM AutomationRule ar " +
           "WHERE ar.tenantId = :tenantId " +
           "GROUP BY ar.triggerType")
    List<Object[]> getAutomationRuleStatistics(@Param("tenantId") UUID tenantId);

    /**
     * Count active automation rules for tenant
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Count automation rules for pipeline
     */
    long countByPipelineId(UUID pipelineId);

    /**
     * Count automation rules for stage
     */
    long countByStageId(UUID stageId);

    /**
     * Find rules created by user
     */
    List<AutomationRule> findByTenantIdAndCreatedByOrderByCreatedAtDesc(UUID tenantId, UUID createdBy);

    /**
     * Get maximum execution order for pipeline
     */
    @Query("SELECT COALESCE(MAX(ar.executionOrder), 0) FROM AutomationRule ar WHERE ar.pipeline.id = :pipelineId")
    Integer getMaxExecutionOrderForPipeline(@Param("pipelineId") UUID pipelineId);

    /**
     * Get maximum execution order for stage
     */
    @Query("SELECT COALESCE(MAX(ar.executionOrder), 0) FROM AutomationRule ar WHERE ar.stage.id = :stageId")
    Integer getMaxExecutionOrderForStage(@Param("stageId") UUID stageId);

    /**
     * Find rules by execution order range
     */
    @Query("SELECT ar FROM AutomationRule ar WHERE ar.tenantId = :tenantId AND " +
           "ar.executionOrder BETWEEN :minOrder AND :maxOrder " +
           "ORDER BY ar.executionOrder ASC")
    List<AutomationRule> findByExecutionOrderRange(@Param("tenantId") UUID tenantId,
                                                   @Param("minOrder") Integer minOrder,
                                                   @Param("maxOrder") Integer maxOrder);
}