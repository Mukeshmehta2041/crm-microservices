package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.DashboardResponse;
import com.crm.platform.analytics.dto.DashboardWidgetResponse;
import com.crm.platform.analytics.entity.Dashboard;
import com.crm.platform.analytics.entity.DashboardWidget;
import com.crm.platform.analytics.repository.DashboardWidgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardMapper {

    private final DashboardWidgetRepository widgetRepository;
    private final AnalyticsCacheService cacheService;

    public DashboardResponse toResponse(Dashboard dashboard) {
        List<DashboardWidget> widgets = widgetRepository
                .findByDashboardIdAndIsActiveTrueOrderByPositionYAscPositionXAsc(dashboard.getId());

        return DashboardResponse.builder()
                .id(dashboard.getId())
                .name(dashboard.getName())
                .description(dashboard.getDescription())
                .createdBy(dashboard.getCreatedBy())
                .organizationId(dashboard.getOrganizationId())
                .isActive(dashboard.getIsActive())
                .isDefault(dashboard.getIsDefault())
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .widgets(widgets.stream().map(this::toWidgetResponse).collect(Collectors.toList()))
                .metrics(buildDashboardMetrics(dashboard, widgets))
                .build();
    }

    public DashboardResponse toResponseWithData(Dashboard dashboard, String organizationId) {
        List<DashboardWidget> widgets = widgetRepository
                .findByDashboardIdAndOrganizationId(dashboard.getId(), organizationId);

        return DashboardResponse.builder()
                .id(dashboard.getId())
                .name(dashboard.getName())
                .description(dashboard.getDescription())
                .createdBy(dashboard.getCreatedBy())
                .organizationId(dashboard.getOrganizationId())
                .isActive(dashboard.getIsActive())
                .isDefault(dashboard.getIsDefault())
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .widgets(widgets.stream().map(this::toWidgetResponseWithData).collect(Collectors.toList()))
                .metrics(buildDashboardMetrics(dashboard, widgets))
                .build();
    }

    public DashboardWidgetResponse toWidgetResponse(DashboardWidget widget) {
        return DashboardWidgetResponse.builder()
                .id(widget.getId())
                .dashboardId(widget.getDashboard().getId())
                .title(widget.getTitle())
                .type(widget.getType().name())
                .queryDefinition(widget.getQueryDefinition())
                .configuration(widget.getConfiguration())
                .positionX(widget.getPositionX())
                .positionY(widget.getPositionY())
                .width(widget.getWidth())
                .height(widget.getHeight())
                .refreshInterval(widget.getRefreshInterval())
                .isActive(widget.getIsActive())
                .createdAt(widget.getCreatedAt())
                .updatedAt(widget.getUpdatedAt())
                .build();
    }

    public DashboardWidgetResponse toWidgetResponseWithData(DashboardWidget widget) {
        DashboardWidgetResponse response = toWidgetResponse(widget);
        
        // Try to get cached data
        Object cachedData = cacheService.getCachedWidgetData(widget.getId()).orElse(null);
        if (cachedData != null) {
            response.setData(cachedData);
            response.setLastRefreshed(LocalDateTime.now().minusSeconds(cacheService.getCacheTtl()));
        }
        
        return response;
    }

    private DashboardResponse.DashboardMetrics buildDashboardMetrics(Dashboard dashboard, List<DashboardWidget> widgets) {
        long activeWidgets = widgets.stream().filter(DashboardWidget::getIsActive).count();
        double avgRefreshInterval = widgets.stream()
                .filter(DashboardWidget::getIsActive)
                .mapToInt(DashboardWidget::getRefreshInterval)
                .average()
                .orElse(0.0);

        return DashboardResponse.DashboardMetrics.builder()
                .totalWidgets(widgets.size())
                .activeWidgets((int) activeWidgets)
                .lastUpdated(dashboard.getUpdatedAt())
                .averageRefreshInterval(avgRefreshInterval)
                .build();
    }
}