package com.crm.platform.customobjects.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a custom object record is created
 */
public class CustomObjectRecordCreatedEvent {

    private UUID recordId;
    private UUID tenantId;
    private UUID customObjectId;
    private String customObjectName;
    private String recordName;
    private Map<String, Object> fieldValues;
    private UUID ownerId;
    private UUID createdBy;
    private LocalDateTime createdAt;

    // Constructors
    public CustomObjectRecordCreatedEvent() {}

    public CustomObjectRecordCreatedEvent(UUID recordId, UUID tenantId, UUID customObjectId,
                                        String customObjectName, String recordName,
                                        Map<String, Object> fieldValues, UUID ownerId,
                                        UUID createdBy, LocalDateTime createdAt) {
        this.recordId = recordId;
        this.tenantId = tenantId;
        this.customObjectId = customObjectId;
        this.customObjectName = customObjectName;
        this.recordName = recordName;
        this.fieldValues = fieldValues;
        this.ownerId = ownerId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCustomObjectId() {
        return customObjectId;
    }

    public void setCustomObjectId(UUID customObjectId) {
        this.customObjectId = customObjectId;
    }

    public String getCustomObjectName() {
        return customObjectName;
    }

    public void setCustomObjectName(String customObjectName) {
        this.customObjectName = customObjectName;
    }

    public String getRecordName() {
        return recordName;
    }

    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(Map<String, Object> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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
        return "CustomObjectRecordCreatedEvent{" +
                "recordId=" + recordId +
                ", tenantId=" + tenantId +
                ", customObjectId=" + customObjectId +
                ", customObjectName='" + customObjectName + '\'' +
                ", recordName='" + recordName + '\'' +
                ", ownerId=" + ownerId +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }
}