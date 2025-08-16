package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating and updating custom field options
 */
public class CustomFieldOptionRequest {

    @NotBlank(message = "Option label is required")
    @Size(max = 255, message = "Option label must not exceed 255 characters")
    @JsonProperty("option_label")
    private String optionLabel;

    @NotBlank(message = "Option value is required")
    @Size(max = 255, message = "Option value must not exceed 255 characters")
    @JsonProperty("option_value")
    private String optionValue;

    @JsonProperty("option_order")
    private Integer optionOrder = 0;

    @JsonProperty("is_default")
    private Boolean isDefault = false;

    @JsonProperty("is_active")
    private Boolean isActive = true;

    @Size(max = 7, message = "Color must not exceed 7 characters")
    private String color;

    private String description;

    // Constructors
    public CustomFieldOptionRequest() {}

    public CustomFieldOptionRequest(String optionLabel, String optionValue, Integer optionOrder) {
        this.optionLabel = optionLabel;
        this.optionValue = optionValue;
        this.optionOrder = optionOrder;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CustomFieldOptionRequest{" +
                "optionLabel='" + optionLabel + '\'' +
                ", optionValue='" + optionValue + '\'' +
                ", optionOrder=" + optionOrder +
                ", isDefault=" + isDefault +
                '}';
    }
}