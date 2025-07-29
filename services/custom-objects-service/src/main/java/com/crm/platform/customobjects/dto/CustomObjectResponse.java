package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for custom objects
 */
public class CustomObjectResponse {

    private UUID id;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    private String name;

    private String label;

    @JsonProperty("plural_label")
    private String pluralLabel;

    private String description;

    @JsonProperty("api_name")
    private String apiName;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("allow_reports")
    private Boolean allowReports;

    @JsonProperty("allow_activities")
    private Boolean allowActivities;

    @JsonProperty("record_name_field")
    private String recordNameField;

    private String icon;

    private String color;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("updated_by")
    private UUID updatedBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("custom_fields")
    private List<CustomFieldResponse> customFields;

    @JsonProperty("record_count")
    private Long recordCount;

    @JsonProperty("field_count")
    private Long fieldCount;

    // Constructors
    public CustomObjectResponse() {}

    public CustomObjectResponse(UUID id, UUID tenantId, String name, String label, String pluralLabel,
                               String apiName, Boolean isActive) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.label = label;
        this.pluralLabel = pluralLabel;
        this.apiName = apiName;
        this.isActive = isActive;
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

    public String getPluralLabel() {
        return pluralLabel;
    }

    public void setPluralLabel(String pluralLabel) {
        this.pluralLabel = pluralLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getAllowReports() {
        return allowReports;
    }

    public void setAllowReports(Boolean allowReports) {
        this.allowReports = allowReports;
    }

    public Boolean getAllowActivities() {
        return allowActivities;
    }

    public void setAllowActivities(Boolean allowActivities) {
        this.allowActivities = allowActivities;
    }

    public String getRecordNameField() {
        return recordNameField;
    }

    public void setRecordNameField(String recordNameField) {
        this.recordNameField = recordNameField;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public List<CustomFieldResponse> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<CustomFieldResponse> customFields) {
        this.customFields = customFields;
    }

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Long getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(Long fieldCount) {
        this.fieldCount = fieldCount;
    }

    @Override
    public String toString() {
        return "CustomObjectResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", apiName='" + apiName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}