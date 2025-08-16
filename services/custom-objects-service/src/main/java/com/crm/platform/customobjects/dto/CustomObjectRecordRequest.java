package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating and updating custom object records
 */
public class CustomObjectRecordRequest {

    @NotNull(message = "Custom object ID is required")
    @JsonProperty("custom_object_id")
    private UUID customObjectId;

    @Size(max = 255, message = "Record name must not exceed 255 characters")
    @JsonProperty("record_name")
    private String recordName;

    @JsonProperty("field_values")
    private Map<String, Object> fieldValues;

    @JsonProperty("is_active")
    private Boolean isActive = true;

    @JsonProperty("owner_id")
    private UUID ownerId;

    // Constructors
    public CustomObjectRecordRequest() {}

    public CustomObjectRecordRequest(UUID customObjectId, String recordName, Map<String, Object> fieldValues) {
        this.customObjectId = customObjectId;
        this.recordName = recordName;
        this.fieldValues = fieldValues;
    }

    // Getters and Setters
    public UUID getCustomObjectId() {
        return customObjectId;
    }

    public void setCustomObjectId(UUID customObjectId) {
        this.customObjectId = customObjectId;
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

    @Override
    public String toString() {
        return "CustomObjectRecordRequest{" +
                "customObjectId=" + customObjectId +
                ", recordName='" + recordName + '\'' +
                ", isActive=" + isActive +
                ", ownerId=" + ownerId +
                '}';
    }
}