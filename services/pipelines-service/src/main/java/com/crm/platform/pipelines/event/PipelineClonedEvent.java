package com.crm.platform.pipelines.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class PipelineClonedEvent {

    private UUID pipelineId;
    private UUID tenantId;
    private String pipelineName;
    private UUID templateId;
    private String templateName;
    private UUID createdBy;
    private LocalDateTime timestamp;

    // Constructors
    public PipelineClonedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PipelineClonedEvent(UUID pipelineId, UUID tenantId, String pipelineName, 
                              UUID templateId, String templateName, UUID createdBy) {
        this();
        this.pipelineId = pipelineId;
        this.tenantId = tenantId;
        this.pipelineName = pipelineName;
        this.templateId = templateId;
        this.templateName = templateName;
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

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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
        return "PipelineClonedEvent{" +
                "pipelineId=" + pipelineId +
                ", tenantId=" + tenantId +
                ", pipelineName='" + pipelineName + '\'' +
                ", templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", createdBy=" + createdBy +
                ", timestamp=" + timestamp +
                '}';
    }
}