package com.crm.platform.pipelines.dto;

import com.crm.platform.pipelines.entity.TriggerType;
import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.UUID;

public class AutomationRuleRequest {

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Trigger type is required")
    private TriggerType triggerType;

    @NotNull(message = "Trigger conditions are required")
    private Map<String, Object> triggerConditions;

    @NotNull(message = "Actions are required")
    private Map<String, Object> actions;

    private Boolean isActive = true;

    @Min(value = 0, message = "Execution order must be non-negative")
    private Integer executionOrder = 0;

    private UUID pipelineId;

    private UUID stageId;

    // Constructors
    public AutomationRuleRequest() {}

    public AutomationRuleRequest(String name, TriggerType triggerType, 
                                Map<String, Object> triggerConditions, Map<String, Object> actions) {
        this.name = name;
        this.triggerType = triggerType;
        this.triggerConditions = triggerConditions;
        this.actions = actions;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "AutomationRuleRequest{" +
                "name='" + name + '\'' +
                ", triggerType=" + triggerType +
                ", isActive=" + isActive +
                ", executionOrder=" + executionOrder +
                ", pipelineId=" + pipelineId +
                ", stageId=" + stageId +
                '}';
    }
}