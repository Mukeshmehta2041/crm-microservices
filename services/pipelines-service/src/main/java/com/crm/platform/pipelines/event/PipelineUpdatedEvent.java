package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class PipelineUpdatedEvent {

    private UUID pipelineId;
    private UUID tenantId;
    private String pipelineName;
    private Boolean isActive;
    private Boolean isDefault;
    private UUID updatedBy;
    private LocalDateTime timestamp;

    // Constructors
    public PipelineUpdatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PipelineUpdatedEvent(UUID pipelineId, UUID tenantId, String pipelineName, 
                               Boolean isActive, Boolean isDefault, UUID updatedBy) {
        this();
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.pipelineName = pipelineName;
        this.isActive = isActive;
        this.isDefault = isDefault;
        this.updatedBy = updatedBy;
    }

    // Getters and Setters
    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
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

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PipelineUpdatedEvent{" +
                "pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", pipelineName='" + pipelineName + '\'' +
                ", isActive=" + isActive +
                ", isDefault=" + isDefault +
                ", updatedBy=" + updatedBy +
                ", timestamp=" + timestamp +
                '}';
    }
}