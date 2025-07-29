package com.crm.platform.workflow.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a business rule execution
 */
@Entity
@Table(name = "rule_executions")
public class RuleExecution {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_rule_id", nullable = false)
    private BusinessRule businessRule;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "trigger_event")
    private String triggerEvent;

    @Type(type = "jsonb")
    @Column(name = "input_data", columnDefinition = "jsonb")
    private JsonNode inputData;

    @Type(type = "jsonb")
    @Column(name = "output_data", columnDefinition = "jsonb")
    private JsonNode outputData;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Type(type = "jsonb")
    @Column(name = "error_details", columnDefinition = "jsonb")
    private JsonNode errorDetails;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public RuleExecution() {}

    public RuleExecution(UUID tenantId, BusinessRule businessRule, UUID entityId, String entityType) {
        this.tenantId = tenantId;
        this.businessRule = businessRule;
        this.entityId = entityId;
        this.entityType = entityType;
        this.status = ExecutionStatus.PENDING;
    }

    // Enums
    public enum ExecutionStatus {
        PENDING, COMPLETED, FAILED, SKIPPED
    }

    // Utility methods
    public void markAsCompleted(JsonNode outputData, long durationMs) {
        this.status = ExecutionStatus.COMPLETED;
        this.outputData = outputData;
        this.durationMs = durationMs;
    }

    public void markAsFailed(String errorMessage, JsonNode errorDetails, long durationMs) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.durationMs = durationMs;
    }

    public void markAsSkipped() {
        this.status = ExecutionStatus.SKIPPED;
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

    public BusinessRule getBusinessRule() {
        return businessRule;
    }

    public void setBusinessRule(BusinessRule businessRule) {
        this.businessRule = businessRule;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
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

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
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

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}