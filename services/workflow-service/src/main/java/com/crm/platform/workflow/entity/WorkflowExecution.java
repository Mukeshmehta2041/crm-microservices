package com.crm.platform.workflow.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a workflow execution instance
 */
@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinition workflowDefinition;

    @Column(name = "execution_key")
    private String executionKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "trigger_type")
    private String triggerType;

    @Type(type = "jsonb")
    @Column(name = "trigger_data", columnDefinition = "jsonb")
    private JsonNode triggerData;

    @Type(type = "jsonb")
    @Column(name = "variables", columnDefinition = "jsonb")
    private JsonNode variables;

    @Column(name = "current_step")
    private String currentStep;

    @Min(0)
    @Max(100)
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Type(type = "jsonb")
    @Column(name = "error_details", columnDefinition = "jsonb")
    private JsonNode errorDetails;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "workflowExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowStepExecution> stepExecutions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public WorkflowExecution() {}

    public WorkflowExecution(UUID tenantId, WorkflowDefinition workflowDefinition, String triggerType) {
        this.tenantId = tenantId;
        this.workflowDefinition = workflowDefinition;
        this.triggerType = triggerType;
        this.status = ExecutionStatus.PENDING;
    }

    // Enums
    public enum ExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, SUSPENDED
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

    public WorkflowDefinition getWorkflowDefinition() {
        return workflowDefinition;
    }

    public void setWorkflowDefinition(WorkflowDefinition workflowDefinition) {
        this.workflowDefinition = workflowDefinition;
    }

    public String getExecutionKey() {
        return executionKey;
    }

    public void setExecutionKey(String executionKey) {
        this.executionKey = executionKey;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
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

    public List<WorkflowStepExecution> getStepExecutions() {
        return stepExecutions;
    }

    public void setStepExecutions(List<WorkflowStepExecution> stepExecutions) {
        this.stepExecutions = stepExecutions;
    }
}