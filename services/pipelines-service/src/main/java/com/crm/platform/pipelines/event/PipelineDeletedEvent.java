package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class PipelineDeletedEvent {

    private UUID pipelineId;
    private UUID tenantId;
    private String pipelineName;
    private LocalDateTime timestamp;

    // Constructors
    public PipelineDeletedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PipelineDeletedEvent(UUID pipelineId, UUID tenantId, String pipelineName) {
        this();
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.pipelineName = pipelineName;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PipelineDeletedEvent{" +
                "pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", pipelineName='" + pipelineName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}