package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PipelinesReorderedEvent {

    private UUID tenantId;
    private List<UUID> pipelineIds;
    private LocalDateTime timestamp;

    // Constructors
    public PipelinesReorderedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PipelinesReorderedEvent(UUID tenantId, List<UUID> pipelineIds) {
        this();
        this.tenantId = tenantId;
        this.pipelineIds = pipelineIds;
    }

    // Getters and Setters
    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public List<UUID> getPipelineIds() {
        return pipelineIds;
    }

    public void setPipelineIds(List<UUID> pipelineIds) {
        this.pipelineIds = pipelineIds;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PipelinesReorderedEvent{" +
                "tenantId=" + tenantId +
                ", pipelineIds=" + pipelineIds +
                ", timestamp=" + timestamp +
                '}';
    }
}