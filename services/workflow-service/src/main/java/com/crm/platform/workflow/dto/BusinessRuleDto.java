package com.crm.platform.workflow.dto;

import com.crm.platform.workflow.entity.BusinessRule;
import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for BusinessRule
 */
public class BusinessRuleDto {

    private UUID id;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Rule type is required")
    private BusinessRule.RuleType ruleType;

    private String entityType;

    @NotNull(message = "Conditions are required")
    private JsonNode conditions;

    @NotNull(message = "Actions are required")
    private JsonNode actions;

    private Boolean isActive = true;

    private Integer priority = 0;

    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Statistics fields
    private Long executionCount;
    private Long successfulExecutions;
    private Long failedExecutions;
    private Double averageExecutionTimeMs;

    // Constructors
    public BusinessRuleDto() {}

    public BusinessRuleDto(String name, BusinessRule.RuleType ruleType, 
                          JsonNode conditions, JsonNode actions) {
        this.name = name;
        this.ruleType = ruleType;
        this.conditions = conditions;
        this.actions = actions;
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

    public BusinessRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(BusinessRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public JsonNode getConditions() {
        return conditions;
    }

    public void setConditions(JsonNode conditions) {
        this.conditions = conditions;
    }

    public JsonNode getActions() {
        return actions;
    }

    public void setActions(JsonNode actions) {
        this.actions = actions;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public Double getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }

    public void setAverageExecutionTimeMs(Double averageExecutionTimeMs) {
        this.averageExecutionTimeMs = averageExecutionTimeMs;
    }
}