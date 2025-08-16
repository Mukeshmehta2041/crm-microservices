package com.crm.platform.customobjects.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a custom object is created
 */
public class CustomObjectCreatedEvent {

    private UUID customObjectId;
    private UUID tenantId;
    private String name;
    private String label;
    private String apiName;
    private UUID createdBy;
    private LocalDateTime createdAt;

    // Constructors
    public CustomObjectCreatedEvent() {}

    public CustomObjectCreatedEvent(UUID customObjectId, UUID tenantId, String name, String label,
                                   String apiName, UUID createdBy, LocalDateTime createdAt) {
        this.customObjectId = customObjectId;
        this.tenantId = tenantId;
        this.name = name;
        this.label = label;
        this.apiName = apiName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CustomObjectCreatedEvent{" +
                "customObjectId=" + customObjectId +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", apiName='" + apiName + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }
}