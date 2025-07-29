package com.crm.platform.customobjects.dto;

import com.crm.platform.customobjects.entity.CustomField;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating and updating custom fields
 */
public class CustomFieldRequest {

    @NotBlank(message = "Object type is required")
    @Size(max = 100, message = "Object type must not exceed 100 characters")
    @JsonProperty("object_type")
    private String objectType;

    @JsonProperty("custom_object_id")
    private UUID customObjectId;

    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name must not exceed 100 characters")
    @JsonProperty("field_name")
    private String fieldName;

    @NotBlank(message = "Field label is required")
    @Size(max = 255, message = "Field label must not exceed 255 characters")
    @JsonProperty("field_label")
    private String fieldLabel;

    @NotNull(message = "Field type is required")
    @JsonProperty("field_type")
    private CustomField.FieldType fieldType;

    @NotNull(message = "Data type is required")
    @JsonProperty("data_type")
    private CustomField.DataType dataType;

    @JsonProperty("is_required")
    private Boolean isRequired = false;

    @JsonProperty("is_unique")
    private Boolean isUnique = false;

    @JsonProperty("is_indexed")
    private Boolean isIndexed = false;

    @JsonProperty("default_value")
    private String defaultValue;

    @JsonProperty("help_text")
    private String helpText;

    @JsonProperty("field_order")
    private Integer fieldOrder = 0;

    @JsonProperty("validation_rules")
    private Map<String, Object> validationRules;

    @JsonProperty("display_options")
    private Map<String, Object> displayOptions;

    @JsonProperty("is_active")
    private Boolean isActive = true;

    @JsonProperty("field_options")
    private List<CustomFieldOptionRequest> fieldOptions;

    // Constructors
    public CustomFieldRequest() {}

    public CustomFieldRequest(String objectType, String fieldName, String fieldLabel,
                             CustomField.FieldType fieldType, CustomField.DataType dataType) {
        this.objectType = objectType;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.fieldType = fieldType;
        this.dataType = dataType;
    }

    // Getters and Setters
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

    public List<CustomFieldOptionRequest> getFieldOptions() {
        return fieldOptions;
    }

    public void setFieldOptions(List<CustomFieldOptionRequest> fieldOptions) {
        this.fieldOptions = fieldOptions;
    }

    @Override
    public String toString() {
        return "CustomFieldRequest{" +
                "objectType='" + objectType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldLabel='" + fieldLabel + '\'' +
                ", fieldType=" + fieldType +
                ", dataType=" + dataType +
                ", isRequired=" + isRequired +
                '}';
    }
}