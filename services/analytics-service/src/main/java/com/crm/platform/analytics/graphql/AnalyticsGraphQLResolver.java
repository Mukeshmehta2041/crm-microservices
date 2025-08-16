package com.crm.platform.analytics.graphql;

import com.crm.platform.analytics.dto.*;
import com.crm.platform.analytics.entity.ReportExecution;
import com.crm.platform.analytics.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AnalyticsGraphQLResolver {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final AnalyticsQueryService analyticsQueryService;
    private final WebSocketService webSocketService;

    // Query resolvers
    @QueryMapping
    public DashboardResponse dashboard(@Argument String id) {
        // Note: In a real implementation, you'd get organizationId from security context
        String organizationId = getCurrentOrganizationId();
        return dashboardService.getDashboard(Long.parseLong(id), organizationId);
    }

    @QueryMapping
    public Page<DashboardResponse> dashboards(@Argument Map<String, Object> filter, 
                                            @Argument Map<String, Object> pagination) {
        String organizationId = getCurrentOrganizationId();
        
        String createdBy = filter != null ? (String) filter.get("createdBy") : null;
        String search = filter != null ? (String) filter.get("search") : null;
        
        Pageable pageable = createPageable(pagination);
        
        return dashboardService.getDashboards(organizationId, createdBy, search, pageable);
    }

    @QueryMapping
    public DashboardResponse defaultDashboard() {
        String organizationId = getCurrentOrganizationId();
        return dashboardService.getDefaultDashboard(organizationId);
    }

    @QueryMapping
    public ReportResponse report(@Argument String id) {
        String organizationId = getCurrentOrganizationId();
        return reportService.getReport(Long.parseLong(id), organizationId);
    }

    @QueryMapping
    public Page<ReportResponse> reports(@Argument Map<String, Object> filter, 
                                      @Argument Map<String, Object> pagination) {
        String organizationId = getCurrentOrganizationId();
        
        String reportType = filter != null ? (String) filter.get("reportType") : null;
        String createdBy = filter != null ? (String) filter.get("createdBy") : null;
        String search = filter != null ? (String) filter.get("search") : null;
        
        Pageable pageable = createPageable(pagination);
        
        return reportService.getReports(organizationId, reportType, createdBy, search, pageable);
    }

    @QueryMapping
    public ReportExecution reportExecution(@Argument String id) {
        String organizationId = getCurrentOrganizationId();
        return reportService.getReportExecution(Long.parseLong(id), organizationId);
    }

    @QueryMapping
    public AnalyticsQueryResponse analyticsQuery(@Argument AnalyticsQueryRequest request) {
        String organizationId = getCurrentOrganizationId();
        return analyticsQueryService.executeQuery(request, organizationId);
    }

    @QueryMapping
    public List<Map<String, Object>> realtimeMetrics(@Argument String measurement) {
        String organizationId = getCurrentOrganizationId();
        return analyticsQueryService.getRealtimeMetrics(organizationId, measurement);
    }

    @QueryMapping
    public List<Map<String, Object>> trendAnalysis(@Argument String measurement, 
                                                  @Argument String timeRange) {
        String organizationId = getCurrentOrganizationId();
        String range = timeRange != null ? timeRange : "-24h";
        return analyticsQueryService.getTrendAnalysis(organizationId, measurement, range);
    }

    @QueryMapping
    public Map<String, Object> performanceMetrics() {
        String organizationId = getCurrentOrganizationId();
        return analyticsQueryService.getPerformanceMetrics(organizationId);
    }

    // Mutation resolvers
    @MutationMapping
    public DashboardResponse createDashboard(@Argument DashboardRequest input) {
        String userId = getCurrentUserId();
        String organizationId = getCurrentOrganizationId();
        return dashboardService.createDashboard(input, userId, organizationId);
    }

    @MutationMapping
    public DashboardResponse updateDashboard(@Argument String id, @Argument DashboardRequest input) {
        String userId = getCurrentUserId();
        String organizationId = getCurrentOrganizationId();
        return dashboardService.updateDashboard(Long.parseLong(id), input, userId, organizationId);
    }

    @MutationMapping
    public Boolean deleteDashboard(@Argument String id) {
        String organizationId = getCurrentOrganizationId();
        dashboardService.deleteDashboard(Long.parseLong(id), organizationId);
        return true;
    }

    @MutationMapping
    public DashboardWidgetResponse addWidget(@Argument String dashboardId, 
                                           @Argument DashboardWidgetRequest input) {
        String organizationId = getCurrentOrganizationId();
        return dashboardService.addWidget(Long.parseLong(dashboardId), input, organizationId);
    }

    @MutationMapping
    public DashboardWidgetResponse updateWidget(@Argument String id, 
                                              @Argument DashboardWidgetRequest input) {
        String organizationId = getCurrentOrganizationId();
        // Note: You'd need to get dashboardId from the widget
        Long dashboardId = getDashboardIdFromWidget(Long.parseLong(id));
        return dashboardService.updateWidget(dashboardId, Long.parseLong(id), input, organizationId);
    }

    @MutationMapping
    public Boolean deleteWidget(@Argument String id) {
        String organizationId = getCurrentOrganizationId();
        Long dashboardId = getDashboardIdFromWidget(Long.parseLong(id));
        dashboardService.deleteWidget(dashboardId, Long.parseLong(id), organizationId);
        return true;
    }

    @MutationMapping
    public ReportResponse createReport(@Argument ReportRequest input) {
        String userId = getCurrentUserId();
        String organizationId = getCurrentOrganizationId();
        return reportService.createReport(input, userId, organizationId);
    }

    @MutationMapping
    public ReportResponse updateReport(@Argument String id, @Argument ReportRequest input) {
        String userId = getCurrentUserId();
        String organizationId = getCurrentOrganizationId();
        return reportService.updateReport(Long.parseLong(id), input, userId, organizationId);
    }

    @MutationMapping
    public Boolean deleteReport(@Argument String id) {
        String organizationId = getCurrentOrganizationId();
        reportService.deleteReport(Long.parseLong(id), organizationId);
        return true;
    }

    @MutationMapping
    public ReportExecution executeReport(@Argument String id) {
        String userId = getCurrentUserId();
        String organizationId = getCurrentOrganizationId();
        return reportService.executeReport(Long.parseLong(id), userId, organizationId);
    }

    // Subscription resolvers
    @SubscriptionMapping
    public Flux<DashboardResponse> dashboardUpdated(@Argument String dashboardId) {
        // This would be implemented with reactive streams in a real application
        return Flux.empty(); // Placeholder
    }

    @SubscriptionMapping
    public Flux<DashboardWidgetResponse> widgetUpdated(@Argument String widgetId) {
        // This would be implemented with reactive streams in a real application
        return Flux.empty(); // Placeholder
    }

    @SubscriptionMapping
    public Flux<ReportExecution> reportExecutionUpdated(@Argument String reportId) {
        // This would be implemented with reactive streams in a real application
        return Flux.empty(); // Placeholder
    }

    @SubscriptionMapping
    public Flux<Map<String, Object>> analyticsAlert(@Argument String organizationId) {
        // This would be implemented with reactive streams in a real application
        return Flux.empty(); // Placeholder
    }

    // Helper methods
    private String getCurrentUserId() {
        // In a real implementation, this would get the user ID from the security context
        return "current-user-id";
    }

    private String getCurrentOrganizationId() {
        // In a real implementation, this would get the organization ID from the security context
        return "current-org-id";
    }

    private Long getDashboardIdFromWidget(Long widgetId) {
        // In a real implementation, this would query the widget to get its dashboard ID
        return 1L; // Placeholder
    }

    private Pageable createPageable(Map<String, Object> pagination) {
        if (pagination == null) {
            return PageRequest.of(0, 20);
        }

        int page = (Integer) pagination.getOrDefault("page", 0);
        int size = (Integer) pagination.getOrDefault("size", 20);
        String sort = (String) pagination.get("sort");
        String direction = (String) pagination.getOrDefault("direction", "ASC");

        if (sort != null) {
            Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) ? 
                    Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(sortDirection, sort));
        }

        return PageRequest.of(page, size);
    }
}