package com.crm.platform.customobjects.event;

import com.crm.platform.customobjects.entity.CustomField;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a custom field is created
 */
public class CustomFieldCreatedEvent {

    private UUID customFieldId;
    private UUID tenantId;
    private UUID customObjectId;
    private String objectType;
    private String fieldName;
    private String fieldLabel;
    private CustomField.FieldType fieldType;
    private CustomField.DataType dataType;
    private Boolean isRequired;
    private Boolean isUnique;
    private Boolean isIndexed;
    private UUID createdBy;
    private LocalDateTime createdAt;

    // Constructors
    public CustomFieldCreatedEvent() {}

    public CustomFieldCreatedEvent(UUID customFieldId, UUID tenantId, UUID customObjectId, String objectType,
                                  String fieldName, String fieldLabel, CustomField.FieldType fieldType,
                                  CustomField.DataType dataType, Boolean isRequired, Boolean isUnique,
                                  Boolean isIndexed, UUID createdBy, LocalDateTime createdAt) {
        this.customFieldId = customFieldId;
        this.tenantId = tenantId;
        this.customObjectId = customObjectId;
        this.objectType = objectType;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.fieldType = fieldType;
        this.dataType = dataType;
        this.isRequired = isRequired;
        this.isUnique = isUnique;
        this.isIndexed = isIndexed;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(UUID customFieldId) {
        this.customFieldId = customFieldId;
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

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public CustomField.FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(CustomField.FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public CustomField.DataType getDataType() {
        return dataType;
    }

    public void setDataType(CustomField.DataType dataType) {
        this.dataType = dataType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }

    public Boolean getIsIndexed() {
        return isIndexed;
    }

    public void setIsIndexed(Boolean isIndexed) {
        this.isIndexed = isIndexed;
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
        return "CustomFieldCreatedEvent{" +
                "customFieldId=" + customFieldId +
                ", tenantId=" + tenantId +
                ", customObjectId=" + customObjectId +
                ", objectType='" + objectType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldLabel='" + fieldLabel + '\'' +
                ", fieldType=" + fieldType +
                ", dataType=" + dataType +
                ", isRequired=" + isRequired +
                ", isUnique=" + isUnique +
                ", isIndexed=" + isIndexed +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }
}