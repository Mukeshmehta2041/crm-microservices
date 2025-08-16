package com.crm.platform.analytics.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ReportResponse {

    private Long id;
    private String name;
    private String description;
    private String reportType;
    private String queryDefinition;
    private Map<String, String> parameters;
    private String createdBy;
    private String organizationId;
    private Boolean isActive;
    private Boolean isScheduled;
    private String scheduleExpression;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ReportExecutionSummary lastExecution;

    @Data
    @Builder
    public static class ReportExecutionSummary {
        private Long executionId;
        private String status;
        private LocalDateTime executedAt;
        private Long executionTimeMs;
        private Integer recordCount;
        private String errorMessage;
    }
}