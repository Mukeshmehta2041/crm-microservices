package com.crm.platform.pipelines.dto;

import com.crm.platform.pipelines.entity.TriggerType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AutomationRuleResponse {

    private UUID id;
    private UUID tenantId;
    private UUID pipelineId;
    private UUID stageId;
    private String name;
    private String description;
    private TriggerType triggerType;
    private Map<String, Object> triggerConditions;
    private Map<String, Object> actions;
    private Boolean isActive;
    private Integer executionOrder;
    private LocalDateTime lastExecutedAt;
    private Long executionCount;
    private Long errorCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private RuleMetrics metrics;

    // Constructors
    public AutomationRuleResponse() {}

    public AutomationRuleResponse(UUID id, String name, TriggerType triggerType, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.triggerType = triggerType;
        this.isActive = isActive;
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

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
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

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public Map<String, Object> getTriggerConditions() {
        return triggerConditions;
    }

    public void setTriggerConditions(Map<String, Object> triggerConditions) {
        this.triggerConditions = triggerConditions;
    }

    public Map<String, Object> getActions() {
        return actions;
    }

    public void setActions(Map<String, Object> actions) {
        this.actions = actions;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
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

    public RuleMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(RuleMetrics metrics) {
        this.metrics = metrics;
    }

    // Nested class for rule metrics
    public static class RuleMetrics {
        private Double successRate;
        private Double errorRate;
        private Long averageExecutionTime;
        private LocalDateTime lastSuccessfulExecution;

        // Constructors
        public RuleMetrics() {}

        public RuleMetrics(Double successRate, Double errorRate, Long averageExecutionTime, 
                          LocalDateTime lastSuccessfulExecution) {
            this.successRate = successRate;
            this.errorRate = errorRate;
            this.averageExecutionTime = averageExecutionTime;
            this.lastSuccessfulExecution = lastSuccessfulExecution;
        }

        // Getters and Setters
        public Double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(Double successRate) {
            this.successRate = successRate;
        }

        public Double getErrorRate() {
            return errorRate;
        }

        public void setErrorRate(Double errorRate) {
            this.errorRate = errorRate;
        }

        public Long getAverageExecutionTime() {
            return averageExecutionTime;
        }

        public void setAverageExecutionTime(Long averageExecutionTime) {
            this.averageExecutionTime = averageExecutionTime;
        }

        public LocalDateTime getLastSuccessfulExecution() {
            return lastSuccessfulExecution;
        }

        public void setLastSuccessfulExecution(LocalDateTime lastSuccessfulExecution) {
            this.lastSuccessfulExecution = lastSuccessfulExecution;
        }
    }

    @Override
    public String toString() {
        return "AutomationRuleResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", triggerType=" + triggerType +
                ", isActive=" + isActive +
                ", executionOrder=" + executionOrder +
                ", executionCount=" + executionCount +
                ", errorCount=" + errorCount +
                '}';
    }
}