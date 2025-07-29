package com.crm.platform.pipelines.event;

import com.crm.platform.pipelines.entity.TriggerType;

import java.time.LocalDateTime;
import java.util.UUID;

public class AutomationRuleCreatedEvent {

    private UUID ruleId;
    private UUID tenantId;
    private UUID pipelineId;
    private UUID stageId;
    private String ruleName;
    private TriggerType triggerType;
    private UUID createdBy;
    private LocalDateTime timestamp;

    // Constructors
    public AutomationRuleCreatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public AutomationRuleCreatedEvent(UUID ruleId, UUID tenantId, UUID pipelineId, UUID stageId, 
                                     String ruleName, TriggerType triggerType, UUID createdBy) {
        this();
        this.ruleId = ruleId;
        this.tenantId = tenantId;
        this.pipelineId = pipelineId;
        this.stageId = stageId;
        this.ruleName = ruleName;
        this.triggerType = triggerType;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AutomationRuleCreatedEvent{" +
                "ruleId=" + ruleId +
                ", tenantId=" + tenantId +
                ", pipelineId=" + pipelineId +
                ", stageId=" + stageId +
                ", ruleName='" + ruleName + '\'' +
                ", triggerType=" + triggerType +
                ", createdBy=" + createdBy +
                ", timestamp=" + timestamp +
                '}';
    }
}