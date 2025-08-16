package com.crm.platform.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ReportRequest {

    @NotBlank(message = "Report name is required")
    private String name;

    private String description;

    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotBlank(message = "Query definition is required")
    private String queryDefinition;

    private Map<String, String> parameters;

    private Boolean isScheduled = false;

    private String scheduleExpression;
}