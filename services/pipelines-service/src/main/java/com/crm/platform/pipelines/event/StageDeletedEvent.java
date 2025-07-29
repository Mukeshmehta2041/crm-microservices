package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class StageDeletedEvent {

    private UUID stageId;
    private UUID pipelineId;
    private UUID tenantId;
    private String stageName;
    private LocalDateTime timestamp;

    // Constructors
    public StageDeletedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public StageDeletedEvent(UUID stageId, UUID pipelineId, UUID tenantId, String stageName) {
        this();
        this.stageId = stageId;
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.stageName = stageName;
    }

    // Getters and Setters
    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
    }

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

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StageDeletedEvent{" +
                "stageId=" + stageId +
                ", pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", stageName='" + stageName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}