package com.crm.platform.workflow.repository;

import com.crm.platform.workflow.entity.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WorkflowExecution entities
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    /**
     * Find workflow executions by tenant ID
     */
    Page<WorkflowExecution> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find workflow executions by tenant ID and status
     */
    Page<WorkflowExecution> findByTenantIdAndStatus(UUID tenantId, WorkflowExecution.ExecutionStatus status, Pageable pageable);

    /**
     * Find workflow executions by workflow definition ID
     */
    Page<WorkflowExecution> findByWorkflowDefinitionId(UUID workflowDefinitionId, Pageable pageable);

    /**
     * Find workflow executions by tenant ID and workflow definition ID
     */
    Page<WorkflowExecution> findByTenantIdAndWorkflowDefinitionId(UUID tenantId, UUID workflowDefinitionId, Pageable pageable);

    /**
     * Find workflow execution by execution key
     */
    Optional<WorkflowExecution> findByTenantIdAndExecutionKey(UUID tenantId, String executionKey);

    /**
     * Find running workflow executions by tenant ID
     */
    @Query("SELECT w FROM WorkflowExecution w WHERE w.tenantId = :tenantId " +
           "AND w.status IN ('PENDING', 'RUNNING', 'SUSPENDED')")
    List<WorkflowExecution> findRunningExecutionsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find workflow executions by tenant ID and trigger type
     */
    Page<WorkflowExecution> findByTenantIdAndTriggerType(UUID tenantId, String triggerType, Pageable pageable);

    /**
     * Find workflow executions by tenant ID and created by user
     */
    Page<WorkflowExecution> findByTenantIdAndCreatedBy(UUID tenantId, UUID createdBy, Pageable pageable);

    /**
     * Find workflow executions started within date range
     */
    @Query("SELECT w FROM WorkflowExecution w WHERE w.tenantId = :tenantId " +
           "AND w.startedAt BETWEEN :startDate AND :endDate")
    Page<WorkflowExecution> findByTenantIdAndStartedAtBetween(@Param("tenantId") UUID tenantId,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate,
                                                              Pageable pageable);

    /**
     * Find completed workflow executions within date range
     */
    @Query("SELECT w FROM WorkflowExecution w WHERE w.tenantId = :tenantId " +
           "AND w.completedAt BETWEEN :startDate AND :endDate " +
           "AND w.status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    Page<WorkflowExecution> findCompletedByTenantIdAndCompletedAtBetween(@Param("tenantId") UUID tenantId,
                                                                         @Param("startDate") LocalDateTime startDate,
                                                                         @Param("endDate") LocalDateTime endDate,
                                                                         Pageable pageable);

    /**
     * Count workflow executions by tenant ID and status
     */
    long countByTenantIdAndStatus(UUID tenantId, WorkflowExecution.ExecutionStatus status);

    /**
     * Count workflow executions by workflow definition ID
     */
    long countByWorkflowDefinitionId(UUID workflowDefinitionId);

    /**
     * Find failed workflow executions that can be retried
     */
    @Query("SELECT w FROM WorkflowExecution w WHERE w.tenantId = :tenantId " +
           "AND w.status = 'FAILED' AND w.completedAt > :retryThreshold")
    List<WorkflowExecution> findRetryableFailedExecutions(@Param("tenantId") UUID tenantId,
                                                          @Param("retryThreshold") LocalDateTime retryThreshold);

    /**
     * Find long-running workflow executions
     */
    @Query("SELECT w FROM WorkflowExecution w WHERE w.tenantId = :tenantId " +
           "AND w.status IN ('RUNNING', 'SUSPENDED') " +
           "AND w.startedAt < :threshold")
    List<WorkflowExecution> findLongRunningExecutions(@Param("tenantId") UUID tenantId,
                                                      @Param("threshold") LocalDateTime threshold);

    /**
     * Get execution statistics by tenant ID
     */
    @Query("SELECT w.status, COUNT(w) FROM WorkflowExecution w " +
           "WHERE w.tenantId = :tenantId GROUP BY w.status")
    List<Object[]> getExecutionStatsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Get execution statistics by workflow definition ID
     */
    @Query("SELECT w.status, COUNT(w) FROM WorkflowExecution w " +
           "WHERE w.workflowDefinition.id = :workflowDefinitionId GROUP BY w.status")
    List<Object[]> getExecutionStatsByWorkflowDefinitionId(@Param("workflowDefinitionId") UUID workflowDefinitionId);
}