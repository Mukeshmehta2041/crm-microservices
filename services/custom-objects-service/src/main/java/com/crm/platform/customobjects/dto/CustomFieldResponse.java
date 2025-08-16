package com.crm.platform.customobjects.dto;

import com.crm.platform.customobjects.entity.CustomField;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for custom fields
 */
public class CustomFieldResponse {

    private UUID id;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("object_type")
    private String objectType;

    @JsonProperty("custom_object_id")
    private UUID customObjectId;

    @JsonProperty("field_name")
    private String fieldName;

    @JsonProperty("field_label")
    private String fieldLabel;

    @JsonProperty("field_type")
    private CustomField.FieldType fieldType;

    @JsonProperty("data_type")
    private CustomField.DataType dataType;

    @JsonProperty("is_required")
    private Boolean isRequired;

    @JsonProperty("is_unique")
    private Boolean isUnique;

    @JsonProperty("is_indexed")
    private Boolean isIndexed;

    @JsonProperty("default_value")
    private String defaultValue;

    @JsonProperty("help_text")
    private String helpText;

    @JsonProperty("field_order")
    private Integer fieldOrder;

    @JsonProperty("validation_rules")
    private Map<String, Object> validationRules;

    @JsonProperty("display_options")
    private Map<String, Object> displayOptions;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("updated_by")
    private UUID updatedBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("field_options")
    private List<CustomFieldOptionResponse> fieldOptions;

    // Constructors
    public CustomFieldResponse() {}

    public CustomFieldResponse(UUID id, UUID tenantId, String objectType, String fieldName,
                              String fieldLabel, CustomField.FieldType fieldType,
                              CustomField.DataType dataType) {
        this.id = id;
        this.tenantId = tenantId;
        this.objectType = objectType;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.fieldType = fieldType;
        this.dataType = dataType;
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

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public UUID getCustomObjectId() {
        return customObjectId;
    }

    public void setCustomObjectId(UUID customObjectId) {
        this.customObjectId = customObjectId;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public Integer getFieldOrder() {
        return fieldOrder;
    }

    public void setFieldOrder(Integer fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    public Map<String, Object> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
    }

    public Map<String, Object> getDisplayOptions() {
        return displayOptions;
    }

    public void setDisplayOptions(Map<String, Object> displayOptions) {
        this.displayOptions = displayOptions;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public List<CustomFieldOptionResponse> getFieldOptions() {
        return fieldOptions;
    }

    public void setFieldOptions(List<CustomFieldOptionResponse> fieldOptions) {
        this.fieldOptions = fieldOptions;
    }

    @Override
    public String toString() {
        return "CustomFieldResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", objectType='" + objectType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldLabel='" + fieldLabel + '\'' +
                ", fieldType=" + fieldType +
                ", dataType=" + dataType +
                '}';
    }
}