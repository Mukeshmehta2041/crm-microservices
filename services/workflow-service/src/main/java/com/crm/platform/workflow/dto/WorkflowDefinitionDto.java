package com.crm.platform.workflow.dto;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for WorkflowDefinition
 */
public class WorkflowDefinitionDto {

    private UUID id;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;

    private String category;

    @Positive(message = "Version must be positive")
    private Integer version = 1;

    private Boolean isActive = true;

    private Boolean isPublished = false;

    @NotNull(message = "Workflow JSON is required")
    private JsonNode workflowJson;

    private JsonNode triggerConfig;

    private JsonNode variablesSchema;

    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Statistics fields
    private Long executionCount;
    private Long successfulExecutions;
    private Long failedExecutions;

    // Constructors
    public WorkflowDefinitionDto() {}

    public WorkflowDefinitionDto(String name, JsonNode workflowJson) {
        this.name = name;
        this.workflowJson = workflowJson;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public JsonNode getWorkflowJson() {
        return workflowJson;
    }

    public void setWorkflowJson(JsonNode workflowJson) {
        this.workflowJson = workflowJson;
    }

    public JsonNode getTriggerConfig() {
        return triggerConfig;
    }

    public void setTriggerConfig(JsonNode triggerConfig) {
        this.triggerConfig = triggerConfig;
    }

    public JsonNode getVariablesSchema() {
        return variablesSchema;
    }

    public void setVariablesSchema(JsonNode variablesSchema) {
        this.variablesSchema = variablesSchema;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }

    public Long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public void setSuccessfulExecutions(Long successfulExecutions) {
        this.successfulExecutions = successfulExecutions;
    }

    public Long getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(Long failedExecutions) {
        this.failedExecutions = failedExecutions;
    }
}