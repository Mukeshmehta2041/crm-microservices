package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class StageCreatedEvent {

    private UUID stageId;
    private UUID pipelineId;
    private UUID tenantId;
    private String stageName;
    private Integer displayOrder;
    private UUID createdBy;
    private LocalDateTime timestamp;

    // Constructors
    public StageCreatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public StageCreatedEvent(UUID stageId, UUID pipelineId, UUID tenantId, String stageName, 
                            Integer displayOrder, UUID createdBy) {
        this();
        this.stageId = stageId;
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.stageName = stageName;
        this.displayOrder = displayOrder;
        this.createdBy = createdBy;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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
        return "StageCreatedEvent{" +
                "stageId=" + stageId +
                ", pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", stageName='" + stageName + '\'' +
                ", displayOrder=" + displayOrder +
                ", createdBy=" + createdBy +
                ", timestamp=" + timestamp +
                '}';
    }
}