package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class PipelineCreatedEvent {

    private UUID pipelineId;
    private UUID tenantId;
    private String pipelineName;
    private Boolean isDefault;
    private UUID createdBy;
    private LocalDateTime timestamp;

    // Constructors
    public PipelineCreatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PipelineCreatedEvent(UUID pipelineId, UUID tenantId, String pipelineName, 
                               Boolean isDefault, UUID createdBy) {
        this();
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.pipelineName = pipelineName;
        this.isDefault = isDefault;
        this.createdBy = createdBy;
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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
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
        return "PipelineCreatedEvent{" +
                "pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", pipelineName='" + pipelineName + '\'' +
                ", isDefault=" + isDefault +
                ", createdBy=" + createdBy +
                ", timestamp=" + timestamp +
                '}';
    }
}