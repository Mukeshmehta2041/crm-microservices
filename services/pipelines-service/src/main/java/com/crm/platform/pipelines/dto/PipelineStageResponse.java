package com.crm.platform.pipelines.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PipelineStageResponse {

    private UUID id;
    private UUID pipelineId;
    private String name;
    private String description;
    private BigDecimal defaultProbability;
    private Boolean isActive;
    private Boolean isClosed;
    private Boolean isWon;
    private Integer displayOrder;
    private String color;
    private Map<String, Object> automationRules;
    private Map<String, Object> stageConfiguration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private List<AutomationRuleResponse> stageAutomationRules;
    private StageMetrics metrics;

    // Constructors
    public PipelineStageResponse() {}

    public PipelineStageResponse(UUID id, String name, Integer displayOrder, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
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

    public BigDecimal getDefaultProbability() {
        return defaultProbability;
    }

    public void setDefaultProbability(BigDecimal defaultProbability) {
        this.defaultProbability = defaultProbability;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean isClosed) {
        this.isClosed = isClosed;
    }

    public Boolean getIsWon() {
        return isWon;
    }

    public void setIsWon(Boolean isWon) {
        this.isWon = isWon;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Map<String, Object> getAutomationRules() {
        return automationRules;
    }

    public void setAutomationRules(Map<String, Object> automationRules) {
        this.automationRules = automationRules;
    }

    public Map<String, Object> getStageConfiguration() {
        return stageConfiguration;
    }

    public void setStageConfiguration(Map<String, Object> stageConfiguration) {
        this.stageConfiguration = stageConfiguration;
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

    public List<AutomationRuleResponse> getStageAutomationRules() {
        return stageAutomationRules;
    }

    public void setStageAutomationRules(List<AutomationRuleResponse> stageAutomationRules) {
        this.stageAutomationRules = stageAutomationRules;
    }

    public StageMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(StageMetrics metrics) {
        this.metrics = metrics;
    }

    // Nested class for stage metrics
    public static class StageMetrics {
        private Integer automationRuleCount;
        private BigDecimal averageTimeInStage;
        private BigDecimal conversionRate;
        private Integer dealCount;

        // Constructors
        public StageMetrics() {}

        public StageMetrics(Integer automationRuleCount, BigDecimal averageTimeInStage, 
                           BigDecimal conversionRate, Integer dealCount) {
            this.automationRuleCount = automationRuleCount;
            this.averageTimeInStage = averageTimeInStage;
            this.conversionRate = conversionRate;
            this.dealCount = dealCount;
        }

        // Getters and Setters
        public Integer getAutomationRuleCount() {
            return automationRuleCount;
        }

        public void setAutomationRuleCount(Integer automationRuleCount) {
            this.automationRuleCount = automationRuleCount;
        }

        public BigDecimal getAverageTimeInStage() {
            return averageTimeInStage;
        }

        public void setAverageTimeInStage(BigDecimal averageTimeInStage) {
            this.averageTimeInStage = averageTimeInStage;
        }

        public BigDecimal getConversionRate() {
            return conversionRate;
        }

        public void setConversionRate(BigDecimal conversionRate) {
            this.conversionRate = conversionRate;
        }

        public Integer getDealCount() {
            return dealCount;
        }

        public void setDealCount(Integer dealCount) {
            this.dealCount = dealCount;
        }
    }

    @Override
    public String toString() {
        return "PipelineStageResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayOrder=" + displayOrder +
                ", isActive=" + isActive +
                ", isClosed=" + isClosed +
                ", isWon=" + isWon +
                '}';
    }
}