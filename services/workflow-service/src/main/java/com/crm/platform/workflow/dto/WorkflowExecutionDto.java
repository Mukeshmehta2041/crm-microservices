package com.crm.platform.workflow.dto;

import com.crm.platform.workflow.entity.WorkflowExecution;
import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for WorkflowExecution
 */
public class WorkflowExecutionDto {

    private UUID id;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Workflow definition ID is required")
    private UUID workflowDefinitionId;

    private String workflowName;

    private String executionKey;

    private WorkflowExecution.ExecutionStatus status;

    private String triggerType;

    private JsonNode triggerData;

    private JsonNode variables;

    private String currentStep;

    @Min(0)
    @Max(100)
    private Integer progressPercentage = 0;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String errorMessage;

    private JsonNode errorDetails;

    private UUID createdBy;

    private List<WorkflowStepExecutionDto> stepExecutions;

    // Calculated fields
    private Long durationMs;
    private Integer totalSteps;
    private Integer completedSteps;

    // Constructors
    public WorkflowExecutionDto() {}

    public WorkflowExecutionDto(UUID tenantId, UUID workflowDefinitionId, String triggerType) {
        this.tenantId = tenantId;
        this.workflowDefinitionId = workflowDefinitionId;
        this.triggerType = triggerType;
        this.status = WorkflowExecution.ExecutionStatus.PENDING;
    }

    // Utility methods
    public boolean isCompleted() {
        return status == WorkflowExecution.ExecutionStatus.COMPLETED ||
               status == WorkflowExecution.ExecutionStatus.FAILED ||
               status == WorkflowExecution.ExecutionStatus.CANCELLED;
    }

    public boolean isRunning() {
        return status == WorkflowExecution.ExecutionStatus.RUNNING ||
               status == WorkflowExecution.ExecutionStatus.PENDING;
    }

    public Long calculateDuration() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
        } else if (startedAt != null) {
            return java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis();
        }
        return null;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(UUID workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getExecutionKey() {
        return executionKey;
    }

    public void setExecutionKey(String executionKey) {
        this.executionKey = executionKey;
    }

    public WorkflowExecution.ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowExecution.ExecutionStatus status) {
        this.status = status;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public JsonNode getTriggerData() {
        return triggerData;
    }

    public void setTriggerData(JsonNode triggerData) {
        this.triggerData = triggerData;
    }

    public JsonNode getVariables() {
        return variables;
    }

    public void setVariables(JsonNode variables) {
        this.variables = variables;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public JsonNode getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(JsonNode errorDetails) {
        this.errorDetails = errorDetails;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public List<WorkflowStepExecutionDto> getStepExecutions() {
        return stepExecutions;
    }

    public void setStepExecutions(List<WorkflowStepExecutionDto> stepExecutions) {
        this.stepExecutions = stepExecutions;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Integer completedSteps) {
        this.completedSteps = completedSteps;
    }
}