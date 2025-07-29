package com.crm.platform.customobjects.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a custom object is updated
 */
public class CustomObjectUpdatedEvent {

    private UUID customObjectId;
    private UUID tenantId;
    private String name;
    private String label;
    private String apiName;
    private Boolean isActive;
    private UUID updatedBy;
    private LocalDateTime updatedAt;

    // Constructors
    public CustomObjectUpdatedEvent() {}

    public CustomObjectUpdatedEvent(UUID customObjectId, UUID tenantId, String name, String label,
                                   String apiName, Boolean isActive, UUID updatedBy, LocalDateTime updatedAt) {
        this.customObjectId = customObjectId;
        this.tenantId = tenantId;
        this.name = name;
        this.label = label;
        this.apiName = apiName;
        this.isActive = isActive;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getCustomObjectId() {
        return customObjectId;
    }

    public void setCustomObjectId(UUID customObjectId) {
        this.customObjectId = customObjectId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "CustomObjectUpdatedEvent{" +
                "customObjectId=" + customObjectId +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", apiName='" + apiName + '\'' +
                ", isActive=" + isActive +
                ", updatedBy=" + updatedBy +
                ", updatedAt=" + updatedAt +
                '}';
    }
}