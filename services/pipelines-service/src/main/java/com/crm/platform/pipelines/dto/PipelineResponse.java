package com.crm.platform.pipelines.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PipelineResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private Boolean isActive;
    private Boolean isDefault;
    private Integer displayOrder;
    private UUID templateId;
    private Map<String, Object> configuration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private List<PipelineStageResponse> stages;
    private List<AutomationRuleResponse> automationRules;
    private PipelineMetrics metrics;

    // Constructors
    public PipelineResponse() {}

    public PipelineResponse(UUID id, String name, String description, Boolean isActive, Boolean isDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.isDefault = isDefault;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
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

    public List<PipelineStageResponse> getStages() {
        return stages;
    }

    public void setStages(List<PipelineStageResponse> stages) {
        this.stages = stages;
    }

    public List<AutomationRuleResponse> getAutomationRules() {
        return automationRules;
    }

    public void setAutomationRules(List<AutomationRuleResponse> automationRules) {
        this.automationRules = automationRules;
    }

    public PipelineMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(PipelineMetrics metrics) {
        this.metrics = metrics;
    }

    // Nested class for pipeline metrics
    public static class PipelineMetrics {
        private Integer stageCount;
        private Integer automationRuleCount;
        private Integer activeStageCount;
        private Integer closedStageCount;

        // Constructors
        public PipelineMetrics() {}

        public PipelineMetrics(Integer stageCount, Integer automationRuleCount, 
                              Integer activeStageCount, Integer closedStageCount) {
            this.stageCount = stageCount;
            this.automationRuleCount = automationRuleCount;
            this.activeStageCount = activeStageCount;
            this.closedStageCount = closedStageCount;
        }

        // Getters and Setters
        public Integer getStageCount() {
            return stageCount;
        }

        public void setStageCount(Integer stageCount) {
            this.stageCount = stageCount;
        }

        public Integer getAutomationRuleCount() {
            return automationRuleCount;
        }

        public void setAutomationRuleCount(Integer automationRuleCount) {
            this.automationRuleCount = automationRuleCount;
        }

        public Integer getActiveStageCount() {
            return activeStageCount;
        }

        public void setActiveStageCount(Integer activeStageCount) {
            this.activeStageCount = activeStageCount;
        }

        public Integer getClosedStageCount() {
            return closedStageCount;
        }

        public void setClosedStageCount(Integer closedStageCount) {
            this.closedStageCount = closedStageCount;
        }
    }

    @Override
    public String toString() {
        return "PipelineResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", isDefault=" + isDefault +
                ", displayOrder=" + displayOrder +
                '}';
    }
}