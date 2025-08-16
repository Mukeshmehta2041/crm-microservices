package com.crm.platform.analytics.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DashboardResponse {

    private Long id;
    private String name;
    private String description;
    private String createdBy;
    private String organizationId;
    private Boolean isActive;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DashboardWidgetResponse> widgets;
    private DashboardMetrics metrics;

    @Data
    @Builder
    public static class DashboardMetrics {
        private Integer totalWidgets;
        private Integer activeWidgets;
        private LocalDateTime lastUpdated;
        private Double averageRefreshInterval;
    }
}