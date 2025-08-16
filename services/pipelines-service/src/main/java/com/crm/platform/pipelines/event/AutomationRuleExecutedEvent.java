package com.crm.platform.pipelines.event;

import com.crm.platform.pipelines.entity.TriggerType;

import java.time.LocalDateTime;
import java.util.UUID;

public class AutomationRuleExecutedEvent {

    private UUID ruleId;
    private UUID tenantId;
    private String ruleName;
    private TriggerType triggerType;
    private Boolean success;
    private String errorMessage;
    private Long executionCount;
    private Long errorCount;
    private LocalDateTime timestamp;

    // Constructors
    public AutomationRuleExecutedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public AutomationRuleExecutedEvent(UUID ruleId, UUID tenantId, String ruleName, TriggerType triggerType, 
                                      Boolean success, String errorMessage, Long executionCount, Long errorCount) {
        this();
        this.ruleId = ruleId;
        this.tenantId = tenantId;
        this.ruleName = ruleName;
        this.triggerType = triggerType;
        this.success = success;
        this.errorMessage = errorMessage;
        this.executionCount = executionCount;
        this.errorCount = errorCount;
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

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AutomationRuleExecutedEvent{" +
                "ruleId=" + ruleId +
                ", tenantId=" + tenantId +
                ", ruleName='" + ruleName + '\'' +
                ", triggerType=" + triggerType +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", executionCount=" + executionCount +
                ", errorCount=" + errorCount +
                ", timestamp=" + timestamp +
                '}';
    }
}