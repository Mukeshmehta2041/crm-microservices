package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class StageUpdatedEvent {

    private UUID stageId;
    private UUID pipelineId;
    private UUID tenantId;
    private String stageName;
    private Boolean isActive;
    private Boolean isClosed;
    private Boolean isWon;
    private UUID updatedBy;
    private LocalDateTime timestamp;

    // Constructors
    public StageUpdatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public StageUpdatedEvent(UUID stageId, UUID pipelineId, UUID tenantId, String stageName, 
                            Boolean isActive, Boolean isClosed, Boolean isWon, UUID updatedBy) {
        this();
        this.stageId = stageId;
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.stageName = stageName;
        this.isActive = isActive;
        this.isClosed = isClosed;
        this.isWon = isWon;
        this.updatedBy = updatedBy;
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
        return "StageUpdatedEvent{" +
                "stageId=" + stageId +
                ", pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", stageName='" + stageName + '\'' +
                ", isActive=" + isActive +
                ", isClosed=" + isClosed +
                ", isWon=" + isWon +
                ", updatedBy=" + updatedBy +
                ", timestamp=" + timestamp +
                '}';
    }
}