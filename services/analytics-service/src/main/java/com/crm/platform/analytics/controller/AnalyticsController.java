package com.crm.platform.analytics.controller;

import com.crm.platform.analytics.dto.AnalyticsQueryRequest;
import com.crm.platform.analytics.dto.AnalyticsQueryResponse;
import com.crm.platform.analytics.service.AnalyticsQueryService;
import com.crm.platform.analytics.service.InfluxDBService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;
    private final InfluxDBService influxDBService;

    @PostMapping("/query")
    public ResponseEntity<AnalyticsQueryResponse> executeQuery(
            @Valid @RequestBody AnalyticsQueryRequest request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Executing analytics query for organization: {}", organizationId);
        AnalyticsQueryResponse response = analyticsQueryService.executeQuery(request, organizationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/query/async")
    public ResponseEntity<CompletableFuture<AnalyticsQueryResponse>> executeQueryAsync(
            @Valid @RequestBody AnalyticsQueryRequest request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Executing async analytics query for organization: {}", organizationId);
        CompletableFuture<AnalyticsQueryResponse> future = analyticsQueryService.executeQueryAsync(request, organizationId);
        return ResponseEntity.ok(future);
    }

    @GetMapping("/metrics/realtime")
    public ResponseEntity<List<Map<String, Object>>> getRealtimeMetrics(
            @RequestParam String measurement,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting realtime metrics for measurement: {} in organization: {}", measurement, organizationId);
        List<Map<String, Object>> metrics = analyticsQueryService.getRealtimeMetrics(organizationId, measurement);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/trends")
    public ResponseEntity<List<Map<String, Object>>> getTrendAnalysis(
            @RequestParam String measurement,
            @RequestParam(defaultValue = "-24h") String timeRange,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting trend analysis for measurement: {} in organization: {}", measurement, organizationId);
        List<Map<String, Object>> trends = analyticsQueryService.getTrendAnalysis(organizationId, measurement, timeRange);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/metrics/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics(
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting performance metrics for organization: {}", organizationId);
        Map<String, Object> metrics = analyticsQueryService.getPerformanceMetrics(organizationId);
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/metrics/write")
    public ResponseEntity<Void> writeMetric(
            @RequestParam String measurement,
            @RequestBody Map<String, Object> data,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Writing metric: {} for organization: {}", measurement, organizationId);
        
        @SuppressWarnings("unchecked")
        Map<String, String> tags = (Map<String, String>) data.getOrDefault("tags", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) data.getOrDefault("fields", Map.of());
        
        // Add organization tag
        tags.put("organization_id", organizationId);
        
        influxDBService.writeMetric(measurement, tags, fields);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<List<Map<String, Object>>> getDashboardMetrics(
            @RequestParam(defaultValue = "-1h") String timeRange,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting dashboard metrics for organization: {}", organizationId);
        List<Map<String, Object>> metrics = influxDBService.getDashboardMetrics(organizationId, timeRange);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/service/performance")
    public ResponseEntity<List<Map<String, Object>>> getServicePerformance(
            @RequestParam String service,
            @RequestParam(defaultValue = "-1h") String timeRange) {
        
        log.info("Getting service performance metrics for service: {}", service);
        List<Map<String, Object>> metrics = influxDBService.getPerformanceMetrics(service, timeRange);
        return ResponseEntity.ok(metrics);
    }
}