package com.crm.platform.analytics.controller;

import com.crm.platform.analytics.dto.*;
import com.crm.platform.analytics.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping
    public ResponseEntity<DashboardResponse> createDashboard(
            @Valid @RequestBody DashboardRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Creating dashboard: {} for user: {} in organization: {}", request.getName(), userId, organizationId);
        DashboardResponse response = dashboardService.createDashboard(request, userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{dashboardId}")
    public ResponseEntity<DashboardResponse> updateDashboard(
            @PathVariable Long dashboardId,
            @Valid @RequestBody DashboardRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Updating dashboard: {} for user: {} in organization: {}", dashboardId, userId, organizationId);
        DashboardResponse response = dashboardService.updateDashboard(dashboardId, request, userId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{dashboardId}")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable Long dashboardId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting dashboard: {} for organization: {}", dashboardId, organizationId);
        DashboardResponse response = dashboardService.getDashboard(dashboardId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{dashboardId}/data")
    public ResponseEntity<DashboardResponse> getDashboardWithData(
            @PathVariable Long dashboardId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting dashboard with data: {} for organization: {}", dashboardId, organizationId);
        DashboardResponse response = dashboardService.getDashboardWithData(dashboardId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DashboardResponse>> getDashboards(
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String search,
            @RequestHeader("X-Organization-Id") String organizationId,
            Pageable pageable) {
        
        log.info("Getting dashboards for organization: {} with filters - createdBy: {}, search: {}", 
                organizationId, createdBy, search);
        Page<DashboardResponse> response = dashboardService.getDashboards(organizationId, createdBy, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/default")
    public ResponseEntity<DashboardResponse> getDefaultDashboard(
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting default dashboard for organization: {}", organizationId);
        DashboardResponse response = dashboardService.getDefaultDashboard(organizationId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping("/user")
    public ResponseEntity<List<DashboardResponse>> getUserDashboards(
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Getting dashboards for user: {}", userId);
        List<DashboardResponse> response = dashboardService.getUserDashboards(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{dashboardId}")
    public ResponseEntity<Void> deleteDashboard(
            @PathVariable Long dashboardId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Deleting dashboard: {} for organization: {}", dashboardId, organizationId);
        dashboardService.deleteDashboard(dashboardId, organizationId);
        return ResponseEntity.noContent().build();
    }

    // Widget endpoints
    @PostMapping("/{dashboardId}/widgets")
    public ResponseEntity<DashboardWidgetResponse> addWidget(
            @PathVariable Long dashboardId,
            @Valid @RequestBody DashboardWidgetRequest request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Adding widget to dashboard: {} for organization: {}", dashboardId, organizationId);
        DashboardWidgetResponse response = dashboardService.addWidget(dashboardId, request, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{dashboardId}/widgets/{widgetId}")
    public ResponseEntity<DashboardWidgetResponse> updateWidget(
            @PathVariable Long dashboardId,
            @PathVariable Long widgetId,
            @Valid @RequestBody DashboardWidgetRequest request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Updating widget: {} in dashboard: {} for organization: {}", widgetId, dashboardId, organizationId);
        DashboardWidgetResponse response = dashboardService.updateWidget(dashboardId, widgetId, request, organizationId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{dashboardId}/widgets/{widgetId}")
    public ResponseEntity<Void> deleteWidget(
            @PathVariable Long dashboardId,
            @PathVariable Long widgetId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Deleting widget: {} from dashboard: {} for organization: {}", widgetId, dashboardId, organizationId);
        dashboardService.deleteWidget(dashboardId, widgetId, organizationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{dashboardId}/widgets/{widgetId}/refresh")
    public ResponseEntity<CompletableFuture<Object>> refreshWidget(
            @PathVariable Long dashboardId,
            @PathVariable Long widgetId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Refreshing widget: {} in dashboard: {} for organization: {}", widgetId, dashboardId, organizationId);
        CompletableFuture<Object> future = dashboardService.refreshWidgetData(widgetId, organizationId);
        return ResponseEntity.ok(future);
    }
}