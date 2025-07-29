package com.crm.platform.workflow.service;

import com.crm.platform.workflow.dto.WorkflowExecutionDto;
import com.crm.platform.workflow.entity.WorkflowDefinition;
import com.crm.platform.workflow.entity.WorkflowExecution;
import com.crm.platform.workflow.exception.WorkflowExecutionException;
import com.crm.platform.workflow.exception.WorkflowNotFoundException;
import com.crm.platform.workflow.mapper.WorkflowExecutionMapper;
import com.crm.platform.workflow.repository.WorkflowDefinitionRepository;
import com.crm.platform.workflow.repository.WorkflowExecutionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing workflow executions
 */
@Service
@Transactional
public class WorkflowExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowExecutionMapper workflowExecutionMapper;
    private final WorkflowEngineService workflowEngineService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public WorkflowExecutionService(WorkflowExecutionRepository workflowExecutionRepository,
                                  WorkflowDefinitionRepository workflowDefinitionRepository,
                                  WorkflowExecutionMapper workflowExecutionMapper,
                                  WorkflowEngineService workflowEngineService,
                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowExecutionMapper = workflowExecutionMapper;
        this.workflowEngineService = workflowEngineService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Start workflow execution
     */
    public WorkflowExecutionDto startWorkflow(UUID tenantId, UUID workflowDefinitionId, 
                                            String triggerType, JsonNode triggerData, 
                                            JsonNode variables, UUID createdBy) {
        logger.info("Starting workflow execution for definition: {} tenant: {}", workflowDefinitionId, tenantId);

        // Get workflow definition
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository.findById(workflowDefinitionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow definition not found: " + workflowDefinitionId));

        // Validate tenant access
        if (!workflowDefinition.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow definition not found: " + workflowDefinitionId);
        }

        // Validate workflow is active and published
        if (!workflowDefinition.getIsActive() || !workflowDefinition.getIsPublished()) {
            throw new WorkflowExecutionException("Workflow is not active or published");
        }

        // Create execution entity
        WorkflowExecution execution = new WorkflowExecution();
        execution.setTenantId(tenantId);
        execution.setWorkflowDefinition(workflowDefinition);
        execution.setExecutionKey(generateExecutionKey(tenantId, workflowDefinitionId));
        execution.setStatus(WorkflowExecution.ExecutionStatus.PENDING);
        execution.setTriggerType(triggerType);
        execution.setTriggerData(triggerData);
        execution.setVariables(variables);
        execution.setCreatedBy(createdBy);

        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Start workflow execution asynchronously
        executeWorkflowAsync(savedExecution);

        // Publish workflow started event
        publishWorkflowEvent("workflow.execution.started", savedExecution);

        logger.info("Started workflow execution: {}", savedExecution.getId());

        return workflowExecutionMapper.toDto(savedExecution);
    }

    /**
     * Execute workflow asynchronously
     */
    @Async
    public CompletableFuture<Void> executeWorkflowAsync(WorkflowExecution execution) {
        try {
            workflowEngineService.executeWorkflow(execution);
        } catch (Exception e) {
            logger.error("Error executing workflow: {}", execution.getId(), e);
            markExecutionAsFailed(execution, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get workflow execution by ID
     */
    @Transactional(readOnly = true)
    public WorkflowExecutionDto getExecution(UUID tenantId, UUID executionId) {
        logger.debug("Getting workflow execution: {} for tenant: {}", executionId, tenantId);

        WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));

        // Validate tenant access
        if (!execution.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow execution not found: " + executionId);
        }

        return workflowExecutionMapper.toDto(execution);
    }

    /**
     * Get workflow execution by execution key
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowExecutionDto> getExecutionByKey(UUID tenantId, String executionKey) {
        logger.debug("Getting workflow execution by key: {} for tenant: {}", executionKey, tenantId);

        Optional<WorkflowExecution> execution = workflowExecutionRepository
                .findByTenantIdAndExecutionKey(tenantId, executionKey);

        return execution.map(workflowExecutionMapper::toDto);
    }

    /**
     * Get all workflow executions for a tenant
     */
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionDto> getExecutions(UUID tenantId, Pageable pageable) {
        logger.debug("Getting workflow executions for tenant: {}", tenantId);

        Page<WorkflowExecution> executions = workflowExecutionRepository.findByTenantId(tenantId, pageable);
        return executions.map(workflowExecutionMapper::toDto);
    }

    /**
     * Get workflow executions by status
     */
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionDto> getExecutionsByStatus(UUID tenantId, 
                                                          WorkflowExecution.ExecutionStatus status, 
                                                          Pageable pageable) {
        logger.debug("Getting workflow executions by status: {} for tenant: {}", status, tenantId);

        Page<WorkflowExecution> executions = workflowExecutionRepository
                .findByTenantIdAndStatus(tenantId, status, pageable);
        return executions.map(workflowExecutionMapper::toDto);
    }

    /**
     * Get workflow executions by workflow definition
     */
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionDto> getExecutionsByWorkflow(UUID tenantId, UUID workflowDefinitionId, 
                                                            Pageable pageable) {
        logger.debug("Getting workflow executions for workflow: {} tenant: {}", workflowDefinitionId, tenantId);

        Page<WorkflowExecution> executions = workflowExecutionRepository
                .findByTenantIdAndWorkflowDefinitionId(tenantId, workflowDefinitionId, pageable);
        return executions.map(workflowExecutionMapper::toDto);
    }

    /**
     * Get running workflow executions
     */
    @Transactional(readOnly = true)
    public List<WorkflowExecutionDto> getRunningExecutions(UUID tenantId) {
        logger.debug("Getting running workflow executions for tenant: {}", tenantId);

        List<WorkflowExecution> executions = workflowExecutionRepository
                .findRunningExecutionsByTenantId(tenantId);
        return workflowExecutionMapper.toDtoList(executions);
    }

    /**
     * Cancel workflow execution
     */
    public WorkflowExecutionDto cancelExecution(UUID tenantId, UUID executionId, UUID cancelledBy) {
        logger.info("Cancelling workflow execution: {} for tenant: {}", executionId, tenantId);

        WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));

        // Validate tenant access
        if (!execution.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow execution not found: " + executionId);
        }

        // Check if execution can be cancelled
        if (execution.getStatus() == WorkflowExecution.ExecutionStatus.COMPLETED ||
            execution.getStatus() == WorkflowExecution.ExecutionStatus.FAILED ||
            execution.getStatus() == WorkflowExecution.ExecutionStatus.CANCELLED) {
            throw new WorkflowExecutionException("Cannot cancel completed workflow execution");
        }

        // Cancel execution
        execution.setStatus(WorkflowExecution.ExecutionStatus.CANCELLED);
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage("Cancelled by user");

        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Cancel in workflow engine
        workflowEngineService.cancelWorkflow(execution);

        // Publish workflow cancelled event
        publishWorkflowEvent("workflow.execution.cancelled", savedExecution);

        logger.info("Cancelled workflow execution: {}", executionId);

        return workflowExecutionMapper.toDto(savedExecution);
    }

    /**
     * Suspend workflow execution
     */
    public WorkflowExecutionDto suspendExecution(UUID tenantId, UUID executionId) {
        logger.info("Suspending workflow execution: {} for tenant: {}", executionId, tenantId);

        WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));

        // Validate tenant access
        if (!execution.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow execution not found: " + executionId);
        }

        // Check if execution can be suspended
        if (execution.getStatus() != WorkflowExecution.ExecutionStatus.RUNNING) {
            throw new WorkflowExecutionException("Can only suspend running workflow executions");
        }

        // Suspend execution
        execution.setStatus(WorkflowExecution.ExecutionStatus.SUSPENDED);
        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Suspend in workflow engine
        workflowEngineService.suspendWorkflow(execution);

        // Publish workflow suspended event
        publishWorkflowEvent("workflow.execution.suspended", savedExecution);

        logger.info("Suspended workflow execution: {}", executionId);

        return workflowExecutionMapper.toDto(savedExecution);
    }

    /**
     * Resume workflow execution
     */
    public WorkflowExecutionDto resumeExecution(UUID tenantId, UUID executionId) {
        logger.info("Resuming workflow execution: {} for tenant: {}", executionId, tenantId);

        WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));

        // Validate tenant access
        if (!execution.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow execution not found: " + executionId);
        }

        // Check if execution can be resumed
        if (execution.getStatus() != WorkflowExecution.ExecutionStatus.SUSPENDED) {
            throw new WorkflowExecutionException("Can only resume suspended workflow executions");
        }

        // Resume execution
        execution.setStatus(WorkflowExecution.ExecutionStatus.RUNNING);
        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Resume in workflow engine
        workflowEngineService.resumeWorkflow(execution);

        // Publish workflow resumed event
        publishWorkflowEvent("workflow.execution.resumed", savedExecution);

        logger.info("Resumed workflow execution: {}", executionId);

        return workflowExecutionMapper.toDto(savedExecution);
    }

    /**
     * Retry failed workflow execution
     */
    public WorkflowExecutionDto retryExecution(UUID tenantId, UUID executionId, UUID retriedBy) {
        logger.info("Retrying workflow execution: {} for tenant: {}", executionId, tenantId);

        WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));

        // Validate tenant access
        if (!execution.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow execution not found: " + executionId);
        }

        // Check if execution can be retried
        if (execution.getStatus() != WorkflowExecution.ExecutionStatus.FAILED) {
            throw new WorkflowExecutionException("Can only retry failed workflow executions");
        }

        // Reset execution status
        execution.setStatus(WorkflowExecution.ExecutionStatus.PENDING);
        execution.setCompletedAt(null);
        execution.setErrorMessage(null);
        execution.setErrorDetails(null);
        execution.setProgressPercentage(0);
        execution.setCurrentStep(null);

        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Start workflow execution asynchronously
        executeWorkflowAsync(savedExecution);

        // Publish workflow retried event
        publishWorkflowEvent("workflow.execution.retried", savedExecution);

        logger.info("Retried workflow execution: {}", executionId);

        return workflowExecutionMapper.toDto(savedExecution);
    }

    /**
     * Update execution progress
     */
    public void updateExecutionProgress(UUID executionId, String currentStep, int progressPercentage) {
        logger.debug("Updating execution progress: {} step: {} progress: {}%", 
                    executionId, currentStep, progressPercentage);

        WorkflowExecution execution = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));

        execution.setCurrentStep(currentStep);
        execution.setProgressPercentage(progressPercentage);
        workflowExecutionRepository.save(execution);

        // Publish progress update event
        publishWorkflowEvent("workflow.execution.progress", execution);
    }

    /**
     * Mark execution as completed
     */
    public void markExecutionAsCompleted(WorkflowExecution execution, JsonNode result) {
        logger.info("Marking workflow execution as completed: {}", execution.getId());

        execution.setStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        execution.setProgressPercentage(100);
        execution.setVariables(result);

        workflowExecutionRepository.save(execution);

        // Publish workflow completed event
        publishWorkflowEvent("workflow.execution.completed", execution);
    }

    /**
     * Mark execution as failed
     */
    public void markExecutionAsFailed(WorkflowExecution execution, String errorMessage) {
        logger.error("Marking workflow execution as failed: {} error: {}", execution.getId(), errorMessage);

        execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);

        workflowExecutionRepository.save(execution);

        // Publish workflow failed event
        publishWorkflowEvent("workflow.execution.failed", execution);
    }

    /**
     * Get execution statistics
     */
    @Transactional(readOnly = true)
    public ExecutionStatistics getExecutionStatistics(UUID tenantId) {
        logger.debug("Getting execution statistics for tenant: {}", tenantId);

        List<Object[]> stats = workflowExecutionRepository.getExecutionStatsByTenantId(tenantId);
        
        ExecutionStatistics statistics = new ExecutionStatistics();
        for (Object[] stat : stats) {
            WorkflowExecution.ExecutionStatus status = (WorkflowExecution.ExecutionStatus) stat[0];
            Long count = (Long) stat[1];
            
            switch (status) {
                case COMPLETED:
                    statistics.setCompletedCount(count);
                    break;
                case FAILED:
                    statistics.setFailedCount(count);
                    break;
                case RUNNING:
                    statistics.setRunningCount(count);
                    break;
                case PENDING:
                    statistics.setPendingCount(count);
                    break;
                case CANCELLED:
                    statistics.setCancelledCount(count);
                    break;
                case SUSPENDED:
                    statistics.setSuspendedCount(count);
                    break;
            }
        }

        return statistics;
    }

    /**
     * Generate unique execution key
     */
    private String generateExecutionKey(UUID tenantId, UUID workflowDefinitionId) {
        return String.format("%s-%s-%d", 
                           tenantId.toString().substring(0, 8),
                           workflowDefinitionId.toString().substring(0, 8),
                           System.currentTimeMillis());
    }

    /**
     * Publish workflow event to Kafka
     */
    private void publishWorkflowEvent(String eventType, WorkflowExecution execution) {
        try {
            WorkflowExecutionDto dto = workflowExecutionMapper.toDto(execution);
            kafkaTemplate.send("workflow-events", eventType, dto);
        } catch (Exception e) {
            logger.error("Error publishing workflow event: {} for execution: {}", eventType, execution.getId(), e);
        }
    }

    /**
     * Get execution entity by ID (for internal use)
     */
    @Transactional(readOnly = true)
    public WorkflowExecution getExecutionEntity(UUID executionId) {
        return workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow execution not found: " + executionId));
    }

    /**
     * Inner class for execution statistics
     */
    public static class ExecutionStatistics {
        private long completedCount = 0;
        private long failedCount = 0;
        private long runningCount = 0;
        private long pendingCount = 0;
        private long cancelledCount = 0;
        private long suspendedCount = 0;

        // Getters and setters
        public long getCompletedCount() { return completedCount; }
        public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
        
        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
        
        public long getRunningCount() { return runningCount; }
        public void setRunningCount(long runningCount) { this.runningCount = runningCount; }
        
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        
        public long getCancelledCount() { return cancelledCount; }
        public void setCancelledCount(long cancelledCount) { this.cancelledCount = cancelledCount; }
        
        public long getSuspendedCount() { return suspendedCount; }
        public void setSuspendedCount(long suspendedCount) { this.suspendedCount = suspendedCount; }
        
        public long getTotalCount() {
            return completedCount + failedCount + runningCount + pendingCount + cancelledCount + suspendedCount;
        }
    }
}