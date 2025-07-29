package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for custom object records
 */
public class CustomObjectRecordResponse {

    private UUID id;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("custom_object_id")
    private UUID customObjectId;

    @JsonProperty("custom_object_name")
    private String customObjectName;

    @JsonProperty("custom_object_label")
    private String customObjectLabel;

    @JsonProperty("record_name")
    private String recordName;

    @JsonProperty("field_values")
    private Map<String, Object> fieldValues;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("owner_id")
    private UUID ownerId;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("updated_by")
    private UUID updatedBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CustomObjectRecordResponse() {}

    public CustomObjectRecordResponse(UUID id, UUID tenantId, UUID customObjectId, String recordName,
                                    Map<String, Object> fieldValues) {
        this.id = id;
        this.tenantId = tenantId;
        this.customObjectId = customObjectId;
        this.recordName = recordName;
        this.fieldValues = fieldValues;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getCustomObjectLabel() {
        return customObjectLabel;
    }

    public void setCustomObjectLabel(String customObjectLabel) {
        this.customObjectLabel = customObjectLabel;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "CustomObjectRecordResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", customObjectId=" + customObjectId +
                ", recordName='" + recordName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}