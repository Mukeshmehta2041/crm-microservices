package com.crm.platform.pipelines.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public class PipelineStageRequest {

    @NotBlank(message = "Stage name is required")
    @Size(max = 255, message = "Stage name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Default probability must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Default probability must be between 0 and 100")
    private BigDecimal defaultProbability;

    private Boolean isActive = true;

    private Boolean isClosed = false;

    private Boolean isWon = false;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be non-negative")
    private Integer displayOrder;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code")
    private String color;

    private Map<String, Object> automationRules;

    private Map<String, Object> stageConfiguration;

    // Constructors
    public PipelineStageRequest() {}

    public PipelineStageRequest(String name, Integer displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDefaultProbability() {
        return defaultProbability;
    }

    public void setDefaultProbability(BigDecimal defaultProbability) {
        this.defaultProbability = defaultProbability;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean isClosed) {
        this.isClosed = isClosed;
    }

    public Boolean getIsWon() {
        return isWon;
    }

    public void setIsWon(Boolean isWon) {
        this.isWon = isWon;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Map<String, Object> getAutomationRules() {
        return automationRules;
    }

    public void setAutomationRules(Map<String, Object> automationRules) {
        this.automationRules = automationRules;
    }

    public Map<String, Object> getStageConfiguration() {
        return stageConfiguration;
    }

    public void setStageConfiguration(Map<String, Object> stageConfiguration) {
        this.stageConfiguration = stageConfiguration;
    }

    @Override
    public String toString() {
        return "PipelineStageRequest{" +
                "name='" + name + '\'' +
                ", displayOrder=" + displayOrder +
                ", isActive=" + isActive +
                ", isClosed=" + isClosed +
                ", isWon=" + isWon +
                '}';
    }
}