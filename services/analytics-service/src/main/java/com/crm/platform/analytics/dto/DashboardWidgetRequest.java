package com.crm.platform.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

@Data
public class DashboardWidgetRequest {

    @NotBlank(message = "Widget title is required")
    private String title;

    @NotNull(message = "Widget type is required")
    private String type;

    @NotBlank(message = "Query definition is required")
    private String queryDefinition;

    private Map<String, String> configuration;

    @Min(value = 0, message = "Position X must be non-negative")
    private Integer positionX = 0;

    @Min(value = 0, message = "Position Y must be non-negative")
    private Integer positionY = 0;

    @Min(value = 1, message = "Width must be at least 1")
    private Integer width = 4;

    @Min(value = 1, message = "Height must be at least 1")
    private Integer height = 3;

    @Min(value = 5, message = "Refresh interval must be at least 5 seconds")
    private Integer refreshInterval = 30;
}