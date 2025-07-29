package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.*;
import com.crm.platform.analytics.entity.Dashboard;
import com.crm.platform.analytics.entity.DashboardWidget;
import com.crm.platform.analytics.repository.DashboardRepository;
import com.crm.platform.analytics.repository.DashboardWidgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardWidgetRepository widgetRepository;
    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsCacheService cacheService;
    private final DashboardMapper dashboardMapper;

    @Transactional
    public DashboardResponse createDashboard(DashboardRequest request, String userId, String organizationId) {
        // Validate dashboard name uniqueness
        if (dashboardRepository.existsByNameAndOrganizationId(request.getName(), organizationId)) {
            throw new IllegalArgumentException("Dashboard with name '" + request.getName() + "' already exists");
        }

        // If this is set as default, unset other defaults
        if (request.getIsDefault()) {
            unsetDefaultDashboard(organizationId);
        }

        Dashboard dashboard = Dashboard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(userId)
                .organizationId(organizationId)
                .isActive(true)
                .isDefault(request.getIsDefault())
                .build();

        Dashboard savedDashboard = dashboardRepository.save(dashboard);
        log.info("Created dashboard: {} for organization: {}", savedDashboard.getName(), organizationId);

        return dashboardMapper.toResponse(savedDashboard);
    }

    @Transactional
    public DashboardResponse updateDashboard(Long dashboardId, DashboardRequest request, String userId, String organizationId) {
        Dashboard dashboard = dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found"));

        // Check name uniqueness if name is being changed
        if (!dashboard.getName().equals(request.getName()) && 
            dashboardRepository.existsByNameAndOrganizationId(request.getName(), organizationId)) {
            throw new IllegalArgumentException("Dashboard with name '" + request.getName() + "' already exists");
        }

        // If this is set as default, unset other defaults
        if (request.getIsDefault() && !dashboard.getIsDefault()) {
            unsetDefaultDashboard(organizationId);
        }

        dashboard.setName(request.getName());
        dashboard.setDescription(request.getDescription());
        dashboard.setIsDefault(request.getIsDefault());

        Dashboard updatedDashboard = dashboardRepository.save(dashboard);
        
        // Invalidate cache
        cacheService.invalidateDashboardCache(dashboardId, organizationId);
        
        log.info("Updated dashboard: {} for organization: {}", updatedDashboard.getName(), organizationId);

        return dashboardMapper.toResponse(updatedDashboard);
    }

    public DashboardResponse getDashboard(Long dashboardId, String organizationId) {
        Dashboard dashboard = dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found"));

        return dashboardMapper.toResponse(dashboard);
    }

    public DashboardResponse getDashboardWithData(Long dashboardId, String organizationId) {
        // Check cache first
        Object cachedData = cacheService.getCachedDashboardData(dashboardId, organizationId).orElse(null);
        if (cachedData != null) {
            return (DashboardResponse) cachedData;
        }

        Dashboard dashboard = dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found"));

        DashboardResponse response = dashboardMapper.toResponseWithData(dashboard, organizationId);
        
        // Cache the result
        cacheService.cacheDashboardData(dashboardId, organizationId, response);
        
        return response;
    }

    public Page<DashboardResponse> getDashboards(String organizationId, String createdBy, 
                                                String searchTerm, Pageable pageable) {
        Page<Dashboard> dashboards = dashboardRepository.findDashboardsWithFilters(
                organizationId, createdBy, searchTerm, pageable);

        return dashboards.map(dashboardMapper::toResponse);
    }

    public DashboardResponse getDefaultDashboard(String organizationId) {
        Dashboard dashboard = dashboardRepository.findByOrganizationIdAndIsActiveTrueAndIsDefaultTrue(organizationId)
                .orElse(null);

        if (dashboard == null) {
            // If no default dashboard, return the first active dashboard
            Page<Dashboard> dashboards = dashboardRepository.findByOrganizationIdAndIsActiveTrue(
                    organizationId, Pageable.ofSize(1));
            if (dashboards.hasContent()) {
                dashboard = dashboards.getContent().get(0);
            }
        }

        return dashboard != null ? dashboardMapper.toResponse(dashboard) : null;
    }

    @Transactional
    public void deleteDashboard(Long dashboardId, String organizationId) {
        Dashboard dashboard = dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found"));

        dashboard.setIsActive(false);
        dashboardRepository.save(dashboard);
        
        // Invalidate cache
        cacheService.invalidateDashboardCache(dashboardId, organizationId);
        
        log.info("Deleted dashboard: {} for organization: {}", dashboard.getName(), organizationId);
    }

    @Transactional
    public DashboardWidgetResponse addWidget(Long dashboardId, DashboardWidgetRequest request, 
                                           String organizationId) {
        Dashboard dashboard = dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found"));

        DashboardWidget widget = DashboardWidget.builder()
                .dashboard(dashboard)
                .title(request.getTitle())
                .type(DashboardWidget.WidgetType.valueOf(request.getType()))
                .queryDefinition(request.getQueryDefinition())
                .configuration(request.getConfiguration())
                .positionX(request.getPositionX())
                .positionY(request.getPositionY())
                .width(request.getWidth())
                .height(request.getHeight())
                .refreshInterval(request.getRefreshInterval())
                .isActive(true)
                .build();

        DashboardWidget savedWidget = widgetRepository.save(widget);
        
        // Invalidate dashboard cache
        cacheService.invalidateDashboardCache(dashboardId, organizationId);
        
        log.info("Added widget: {} to dashboard: {}", savedWidget.getTitle(), dashboard.getName());

        return dashboardMapper.toWidgetResponse(savedWidget);
    }

    @Transactional
    public DashboardWidgetResponse updateWidget(Long dashboardId, Long widgetId, 
                                              DashboardWidgetRequest request, String organizationId) {
        DashboardWidget widget = widgetRepository.findByIdAndDashboardId(widgetId, dashboardId)
                .orElseThrow(() -> new IllegalArgumentException("Widget not found"));

        if (!widget.getDashboard().getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Widget not found");
        }

        widget.setTitle(request.getTitle());
        widget.setType(DashboardWidget.WidgetType.valueOf(request.getType()));
        widget.setQueryDefinition(request.getQueryDefinition());
        widget.setConfiguration(request.getConfiguration());
        widget.setPositionX(request.getPositionX());
        widget.setPositionY(request.getPositionY());
        widget.setWidth(request.getWidth());
        widget.setHeight(request.getHeight());
        widget.setRefreshInterval(request.getRefreshInterval());

        DashboardWidget updatedWidget = widgetRepository.save(widget);
        
        // Invalidate caches
        cacheService.invalidateWidgetCache(widgetId);
        cacheService.invalidateDashboardCache(dashboardId, organizationId);
        
        log.info("Updated widget: {} in dashboard: {}", updatedWidget.getTitle(), dashboardId);

        return dashboardMapper.toWidgetResponse(updatedWidget);
    }

    @Transactional
    public void deleteWidget(Long dashboardId, Long widgetId, String organizationId) {
        DashboardWidget widget = widgetRepository.findByIdAndDashboardId(widgetId, dashboardId)
                .orElseThrow(() -> new IllegalArgumentException("Widget not found"));

        if (!widget.getDashboard().getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Widget not found");
        }

        widget.setIsActive(false);
        widgetRepository.save(widget);
        
        // Invalidate caches
        cacheService.invalidateWidgetCache(widgetId);
        cacheService.invalidateDashboardCache(dashboardId, organizationId);
        
        log.info("Deleted widget: {} from dashboard: {}", widget.getTitle(), dashboardId);
    }

    public CompletableFuture<Object> refreshWidgetData(Long widgetId, String organizationId) {
        return CompletableFuture.supplyAsync(() -> {
            DashboardWidget widget = widgetRepository.findById(widgetId)
                    .orElseThrow(() -> new IllegalArgumentException("Widget not found"));

            if (!widget.getDashboard().getOrganizationId().equals(organizationId)) {
                throw new IllegalArgumentException("Widget not found");
            }

            // Execute widget query
            AnalyticsQueryRequest queryRequest = new AnalyticsQueryRequest();
            queryRequest.setQuery(widget.getQueryDefinition());
            
            AnalyticsQueryResponse response = analyticsQueryService.executeQuery(queryRequest, organizationId);
            
            // Cache the result
            cacheService.cacheWidgetData(widgetId, response.getData());
            
            return response.getData();
        });
    }

    private void unsetDefaultDashboard(String organizationId) {
        dashboardRepository.findByOrganizationIdAndIsActiveTrueAndIsDefaultTrue(organizationId)
                .ifPresent(dashboard -> {
                    dashboard.setIsDefault(false);
                    dashboardRepository.save(dashboard);
                });
    }

    public List<DashboardResponse> getUserDashboards(String userId) {
        List<Dashboard> dashboards = dashboardRepository.findByCreatedByAndIsActiveTrue(userId);
        return dashboards.stream()
                .map(dashboardMapper::toResponse)
                .collect(Collectors.toList());
    }
}