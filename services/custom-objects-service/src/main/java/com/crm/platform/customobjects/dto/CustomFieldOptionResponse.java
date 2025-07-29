package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for custom field options
 */
public class CustomFieldOptionResponse {

    private UUID id;

    @JsonProperty("custom_field_id")
    private UUID customFieldId;

    @JsonProperty("option_label")
    private String optionLabel;

    @JsonProperty("option_value")
    private String optionValue;

    @JsonProperty("option_order")
    private Integer optionOrder;

    @JsonProperty("is_default")
    private Boolean isDefault;

    @JsonProperty("is_active")
    private Boolean isActive;

    private String color;

    private String description;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("updated_by")
    private UUID updatedBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CustomFieldOptionResponse() {}

    public CustomFieldOptionResponse(UUID id, UUID customFieldId, String optionLabel,
                                   String optionValue, Integer optionOrder) {
        this.id = id;
        this.customFieldId = customFieldId;
        this.optionLabel = optionLabel;
        this.optionValue = optionValue;
        this.optionOrder = optionOrder;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(UUID customFieldId) {
        this.customFieldId = customFieldId;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public Integer getOptionOrder() {
        return optionOrder;
    }

    public void setOptionOrder(Integer optionOrder) {
        this.optionOrder = optionOrder;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        return "CustomFieldOptionResponse{" +
                "id=" + id +
                ", customFieldId=" + customFieldId +
                ", optionLabel='" + optionLabel + '\'' +
                ", optionValue='" + optionValue + '\'' +
                ", optionOrder=" + optionOrder +
                ", isDefault=" + isDefault +
                '}';
    }
}