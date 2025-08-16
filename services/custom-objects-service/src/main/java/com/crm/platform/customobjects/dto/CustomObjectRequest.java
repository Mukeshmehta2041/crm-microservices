package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating and updating custom objects
 */
public class CustomObjectRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Label is required")
    @Size(max = 255, message = "Label must not exceed 255 characters")
    private String label;

    @NotBlank(message = "Plural label is required")
    @Size(max = 255, message = "Plural label must not exceed 255 characters")
    @JsonProperty("plural_label")
    private String pluralLabel;

    private String description;

    @NotBlank(message = "API name is required")
    @Size(max = 100, message = "API name must not exceed 100 characters")
    @Pattern(regexp = "^[a-z][a-z0-9_]*[a-z0-9]$", message = "API name must be lowercase with underscores")
    @JsonProperty("api_name")
    private String apiName;

    @JsonProperty("is_active")
    private Boolean isActive = true;

    @JsonProperty("allow_reports")
    private Boolean allowReports = true;

    @JsonProperty("allow_activities")
    private Boolean allowActivities = true;

    @Size(max = 100, message = "Record name field must not exceed 100 characters")
    @JsonProperty("record_name_field")
    private String recordNameField;

    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    private String color;

    // Constructors
    public CustomObjectRequest() {}

    public CustomObjectRequest(String name, String label, String pluralLabel, String apiName) {
        this.name = name;
        this.label = label;
        this.pluralLabel = pluralLabel;
        this.apiName = apiName;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CustomObjectRequest{" +
                "name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", pluralLabel='" + pluralLabel + '\'' +
                ", apiName='" + apiName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}