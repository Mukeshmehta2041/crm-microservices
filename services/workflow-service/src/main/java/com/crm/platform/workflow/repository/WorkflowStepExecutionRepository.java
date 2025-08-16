package com.crm.platform.workflow.repository;

import com.crm.platform.workflow.entity.WorkflowStepExecution;
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
 * Repository interface for WorkflowStepExecution entities
 */
@Repository
public interface WorkflowStepExecutionRepository extends JpaRepository<WorkflowStepExecution, UUID> {

    /**
     * Find step executions by workflow execution ID
     */
    List<WorkflowStepExecution> findByWorkflowExecutionIdOrderByStartedAt(UUID workflowExecutionId);

    /**
     * Find step executions by workflow execution ID with pagination
     */
    Page<WorkflowStepExecution> findByWorkflowExecutionId(UUID workflowExecutionId, Pageable pageable);

    /**
     * Find step executions by tenant ID
     */
    Page<WorkflowStepExecution> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find step executions by tenant ID and status
     */
    Page<WorkflowStepExecution> findByTenantIdAndStatus(UUID tenantId, 
                                                       WorkflowStepExecution.StepStatus status, 
                                                       Pageable pageable);

    /**
     * Find step execution by workflow execution ID and step ID
     */
    WorkflowStepExecution findByWorkflowExecutionIdAndStepId(UUID workflowExecutionId, String stepId);

    /**
     * Count step executions by workflow execution ID
     */
    long countByWorkflowExecutionId(UUID workflowExecutionId);

    /**
     * Count step executions by workflow execution ID and status
     */
    long countByWorkflowExecutionIdAndStatus(UUID workflowExecutionId, WorkflowStepExecution.StepStatus status);

    /**
     * Count step executions by workflow execution ID and status in list
     */
    long countByWorkflowExecutionIdAndStatusIn(UUID workflowExecutionId, List<WorkflowStepExecution.StepStatus> statuses);

    /**
     * Find failed step executions that can be retried
     */
    @Query("SELECT s FROM WorkflowStepExecution s WHERE s.tenantId = :tenantId " +
           "AND s.status = 'FAILED' AND s.retryCount < 3 " +
           "AND s.completedAt > :retryThreshold")
    List<WorkflowStepExecution> findRetryableFailedSteps(@Param("tenantId") UUID tenantId,
                                                         @Param("retryThreshold") LocalDateTime retryThreshold);

    /**
     * Find long-running step executions
     */
    @Query("SELECT s FROM WorkflowStepExecution s WHERE s.tenantId = :tenantId " +
           "AND s.status = 'RUNNING' AND s.startedAt < :threshold")
    List<WorkflowStepExecution> findLongRunningSteps(@Param("tenantId") UUID tenantId,
                                                     @Param("threshold") LocalDateTime threshold);

    /**
     * Get step execution statistics by workflow execution ID
     */
    @Query("SELECT s.status, COUNT(s) FROM WorkflowStepExecution s " +
           "WHERE s.workflowExecution.id = :workflowExecutionId GROUP BY s.status")
    List<Object[]> getStepStatsByWorkflowExecutionId(@Param("workflowExecutionId") UUID workflowExecutionId);

    /**
     * Get step execution statistics by tenant ID
     */
    @Query("SELECT s.status, COUNT(s) FROM WorkflowStepExecution s " +
           "WHERE s.tenantId = :tenantId GROUP BY s.status")
    List<Object[]> getStepStatsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Get average step execution duration by step type
     */
    @Query("SELECT s.stepType, AVG(s.durationMs) FROM WorkflowStepExecution s " +
           "WHERE s.tenantId = :tenantId AND s.status = 'COMPLETED' " +
           "AND s.durationMs IS NOT NULL GROUP BY s.stepType")
    List<Object[]> getAverageStepDurationByType(@Param("tenantId") UUID tenantId);

    /**
     * Find step executions by step type
     */
    Page<WorkflowStepExecution> findByTenantIdAndStepType(UUID tenantId, String stepType, Pageable pageable);

    /**
     * Find step executions within date range
     */
    @Query("SELECT s FROM WorkflowStepExecution s WHERE s.tenantId = :tenantId " +
           "AND s.startedAt BETWEEN :startDate AND :endDate")
    Page<WorkflowStepExecution> findByTenantIdAndStartedAtBetween(@Param("tenantId") UUID tenantId,
                                                                 @Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate,
                                                                 Pageable pageable);
}