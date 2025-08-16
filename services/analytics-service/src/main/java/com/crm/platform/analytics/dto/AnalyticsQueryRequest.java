package com.crm.platform.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AnalyticsQueryRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private Map<String, Object> parameters;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer limit = 1000;

    private String aggregation;

    private String groupBy;

    private String orderBy;
}