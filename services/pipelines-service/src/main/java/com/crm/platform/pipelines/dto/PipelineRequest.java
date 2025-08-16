package com.crm.platform.pipelines.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PipelineRequest {

    @NotBlank(message = "Pipeline name is required")
    @Size(max = 255, message = "Pipeline name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive = true;

    private Boolean isDefault = false;

    @Min(value = 0, message = "Display order must be non-negative")
    private Integer displayOrder = 0;

    private UUID templateId;

    private Map<String, Object> configuration;

    @Valid
    private List<PipelineStageRequest> stages;

    // Constructors
    public PipelineRequest() {}

    public PipelineRequest(String name, String description) {
        this.name = name;
        this.description = description;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public List<PipelineStageRequest> getStages() {
        return stages;
    }

    public void setStages(List<PipelineStageRequest> stages) {
        this.stages = stages;
    }

    @Override
    public String toString() {
        return "PipelineRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", isDefault=" + isDefault +
                ", displayOrder=" + displayOrder +
                ", templateId=" + templateId +
                '}';
    }
}