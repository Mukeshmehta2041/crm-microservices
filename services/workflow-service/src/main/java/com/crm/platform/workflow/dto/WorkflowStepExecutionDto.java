package com.crm.platform.workflow.dto;

import com.crm.platform.workflow.entity.WorkflowStepExecution;
import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for WorkflowStepExecution
 */
public class WorkflowStepExecutionDto {

    private UUID id;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Workflow execution ID is required")
    private UUID workflowExecutionId;

    @NotBlank(message = "Step ID is required")
    private String stepId;

    @NotBlank(message = "Step name is required")
    private String stepName;

    @NotBlank(message = "Step type is required")
    private String stepType;

    private WorkflowStepExecution.StepStatus status;

    private JsonNode inputData;

    private JsonNode outputData;

    private String errorMessage;

    private JsonNode errorDetails;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Long durationMs;

    @Min(0)
    private Integer retryCount = 0;

    // Constructors
    public WorkflowStepExecutionDto() {}

    public WorkflowStepExecutionDto(UUID tenantId, UUID workflowExecutionId, 
                                   String stepId, String stepName, String stepType) {
        this.tenantId = tenantId;
        this.workflowExecutionId = workflowExecutionId;
        this.stepId = stepId;
        this.stepName = stepName;
        this.stepType = stepType;
        this.status = WorkflowStepExecution.StepStatus.PENDING;
    }

    // Utility methods
    public boolean isCompleted() {
        return status == WorkflowStepExecution.StepStatus.COMPLETED ||
               status == WorkflowStepExecution.StepStatus.FAILED ||
               status == WorkflowStepExecution.StepStatus.SKIPPED ||
               status == WorkflowStepExecution.StepStatus.CANCELLED;
    }

    public boolean isSuccessful() {
        return status == WorkflowStepExecution.StepStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == WorkflowStepExecution.StepStatus.FAILED;
    }

    public Long calculateDuration() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
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

    public UUID getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(UUID workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public WorkflowStepExecution.StepStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStepExecution.StepStatus status) {
        this.status = status;
    }

    public JsonNode getInputData() {
        return inputData;
    }

    public void setInputData(JsonNode inputData) {
        this.inputData = inputData;
    }

    public JsonNode getOutputData() {
        return outputData;
    }

    public void setOutputData(JsonNode outputData) {
        this.outputData = outputData;
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
}