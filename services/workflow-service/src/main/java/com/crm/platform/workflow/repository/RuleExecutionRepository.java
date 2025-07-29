package com.crm.platform.workflow.repository;

import com.crm.platform.workflow.entity.RuleExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for RuleExecution entities
 */
@Repository
public interface RuleExecutionRepository extends JpaRepository<RuleExecution, UUID> {

    /**
     * Find rule executions by tenant ID
     */
    Page<RuleExecution> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find rule executions by business rule ID
     */
    Page<RuleExecution> findByBusinessRuleId(UUID businessRuleId, Pageable pageable);

    /**
     * Find rule executions by tenant ID and business rule ID
     */
    Page<RuleExecution> findByTenantIdAndBusinessRuleId(UUID tenantId, UUID businessRuleId, Pageable pageable);

    /**
     * Find rule executions by tenant ID and entity
     */
    Page<RuleExecution> findByTenantIdAndEntityTypeAndEntityId(UUID tenantId, String entityType, UUID entityId, Pageable pageable);

    /**
     * Find rule executions by tenant ID and status
     */
    Page<RuleExecution> findByTenantIdAndStatus(UUID tenantId, RuleExecution.ExecutionStatus status, Pageable pageable);

    /**
     * Find rule executions by tenant ID and trigger event
     */
    Page<RuleExecution> findByTenantIdAndTriggerEvent(UUID tenantId, String triggerEvent, Pageable pageable);

    /**
     * Find rule executions within date range
     */
    @Query("SELECT r FROM RuleExecution r WHERE r.tenantId = :tenantId " +
           "AND r.executedAt BETWEEN :startDate AND :endDate")
    Page<RuleExecution> findByTenantIdAndExecutedAtBetween(@Param("tenantId") UUID tenantId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate,
                                                           Pageable pageable);

    /**
     * Count rule executions by tenant ID and status
     */
    long countByTenantIdAndStatus(UUID tenantId, RuleExecution.ExecutionStatus status);

    /**
     * Count rule executions by business rule ID
     */
    long countByBusinessRuleId(UUID businessRuleId);

    /**
     * Count rule executions by business rule ID and status
     */
    long countByBusinessRuleIdAndStatus(UUID businessRuleId, RuleExecution.ExecutionStatus status);

    /**
     * Get rule execution statistics by tenant ID
     */
    @Query("SELECT r.status, COUNT(r) FROM RuleExecution r " +
           "WHERE r.tenantId = :tenantId GROUP BY r.status")
    List<Object[]> getRuleExecutionStatsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Get rule execution statistics by business rule ID
     */
    @Query("SELECT r.status, COUNT(r) FROM RuleExecution r " +
           "WHERE r.businessRule.id = :businessRuleId GROUP BY r.status")
    List<Object[]> getRuleExecutionStatsByBusinessRuleId(@Param("businessRuleId") UUID businessRuleId);

    /**
     * Get average execution duration by tenant ID
     */
    @Query("SELECT AVG(r.durationMs) FROM RuleExecution r " +
           "WHERE r.tenantId = :tenantId AND r.status = 'COMPLETED' AND r.durationMs IS NOT NULL")
    Double getAverageExecutionDurationByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Get average execution duration by business rule ID
     */
    @Query("SELECT AVG(r.durationMs) FROM RuleExecution r " +
           "WHERE r.businessRule.id = :businessRuleId AND r.status = 'COMPLETED' AND r.durationMs IS NOT NULL")
    Double getAverageExecutionDurationByBusinessRuleId(@Param("businessRuleId") UUID businessRuleId);

    /**
     * Find recent rule executions for entity
     */
    @Query("SELECT r FROM RuleExecution r WHERE r.tenantId = :tenantId " +
           "AND r.entityType = :entityType AND r.entityId = :entityId " +
           "ORDER BY r.executedAt DESC")
    List<RuleExecution> findRecentExecutionsForEntity(@Param("tenantId") UUID tenantId,
                                                      @Param("entityType") String entityType,
                                                      @Param("entityId") UUID entityId,
                                                      Pageable pageable);

    /**
     * Find failed rule executions that can be retried
     */
    @Query("SELECT r FROM RuleExecution r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'FAILED' AND r.executedAt > :retryThreshold")
    List<RuleExecution> findRetryableFailedExecutions(@Param("tenantId") UUID tenantId,
                                                      @Param("retryThreshold") LocalDateTime retryThreshold);
}