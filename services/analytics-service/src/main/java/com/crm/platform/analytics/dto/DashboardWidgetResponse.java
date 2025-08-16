package com.crm.platform.analytics.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class DashboardWidgetResponse {

    private Long id;
    private Long dashboardId;
    private String title;
    private String type;
    private String queryDefinition;
    private Map<String, String> configuration;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private Integer refreshInterval;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Object data; // Current widget data
    private LocalDateTime lastRefreshed;
}