package com.crm.platform.workflow.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a workflow step execution
 */
@Entity
@Table(name = "workflow_step_executions")
public class WorkflowStepExecution {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_execution_id", nullable = false)
    private WorkflowExecution workflowExecution;

    @NotBlank
    @Column(name = "step_id", nullable = false)
    private String stepId;

    @NotBlank
    @Column(name = "step_name", nullable = false)
    private String stepName;

    @NotBlank
    @Column(name = "step_type", nullable = false)
    private String stepType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Type(type = "jsonb")
    @Column(name = "input_data", columnDefinition = "jsonb")
    private JsonNode inputData;

    @Type(type = "jsonb")
    @Column(name = "output_data", columnDefinition = "jsonb")
    private JsonNode outputData;

    @Column(name = "error_message")
    private String errorMessage;

    @Type(type = "jsonb")
    @Column(name = "error_details", columnDefinition = "jsonb")
    private JsonNode errorDetails;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Min(0)
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    // Constructors
    public WorkflowStepExecution() {}

    public WorkflowStepExecution(UUID tenantId, WorkflowExecution workflowExecution, 
                                String stepId, String stepName, String stepType) {
        this.tenantId = tenantId;
        this.workflowExecution = workflowExecution;
        this.stepId = stepId;
        this.stepName = stepName;
        this.stepType = stepType;
        this.status = StepStatus.PENDING;
    }

    // Enums
    public enum StepStatus {
        PENDING, RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED
    }

    // Utility methods
    public void markAsStarted() {
        this.status = StepStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted(JsonNode outputData) {
        this.status = StepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.outputData = outputData;
        calculateDuration();
    }

    public void markAsFailed(String errorMessage, JsonNode errorDetails) {
        this.status = StepStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        calculateDuration();
    }

    public void markAsSkipped() {
        this.status = StepStatus.SKIPPED;
        this.completedAt = LocalDateTime.now();
        calculateDuration();
    }

    private void calculateDuration() {
        if (startedAt != null && completedAt != null) {
            this.durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        }
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

    public WorkflowExecution getWorkflowExecution() {
        return workflowExecution;
    }

    public void setWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
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

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
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