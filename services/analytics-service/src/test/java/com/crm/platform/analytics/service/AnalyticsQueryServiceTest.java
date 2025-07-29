package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.AnalyticsQueryRequest;
import com.crm.platform.analytics.dto.AnalyticsQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private InfluxDBService influxDBService;

    @Mock
    private AnalyticsCacheService cacheService;

    @InjectMocks
    private AnalyticsQueryService analyticsQueryService;

    private AnalyticsQueryRequest queryRequest;
    private String organizationId;

    @BeforeEach
    void setUp() {
        queryRequest = new AnalyticsQueryRequest();
        queryRequest.setQuery("measurement:user_activity");
        queryRequest.setLimit(100);
        
        organizationId = "test-org-123";
    }

    @Test
    void executeQuery_ShouldReturnCachedResult_WhenCacheHit() {
        // Given
        List<Map<String, Object>> cachedData = List.of(Map.of("count", 10));
        when(cacheService.generateQueryHash(anyString(), any())).thenReturn("hash123");
        when(cacheService.getCachedQueryResult("hash123")).thenReturn(Optional.of(cachedData));

        // When
        AnalyticsQueryResponse response = analyticsQueryService.executeQuery(queryRequest, organizationId);

        // Then
        assertThat(response.getData()).isEqualTo(cachedData);
        assertThat(response.getMetadata().getFromCache()).isTrue();
        verify(influxDBService, never()).query(anyString());
    }

    @Test
    void executeQuery_ShouldExecuteQuery_WhenCacheMiss() {
        // Given
        List<Map<String, Object>> queryData = List.of(Map.of("count", 15));
        when(cacheService.generateQueryHash(anyString(), any())).thenReturn("hash123");
        when(cacheService.getCachedQueryResult("hash123")).thenReturn(Optional.empty());
        when(cacheService.isCacheEnabled()).thenReturn(true);
        when(influxDBService.query(anyString())).thenReturn(queryData);

        // When
        AnalyticsQueryResponse response = analyticsQueryService.executeQuery(queryRequest, organizationId);

        // Then
        assertThat(response.getData()).isEqualTo(queryData);
        assertThat(response.getMetadata().getFromCache()).isFalse();
        verify(influxDBService).query(anyString());
        verify(cacheService).cacheQueryResult("hash123", queryData);
    }

    @Test
    void getRealtimeMetrics_ShouldReturnMetrics() {
        // Given
        String measurement = "user_activity";
        List<Map<String, Object>> expectedMetrics = List.of(
                Map.of("_time", "2023-01-01T10:00:00Z", "_value", 5.0)
        );
        when(influxDBService.queryTimeSeriesData(eq(measurement), any(), eq("-5m"), eq("mean(column: \"_value\")")))
                .thenReturn(expectedMetrics);

        // When
        List<Map<String, Object>> result = analyticsQueryService.getRealtimeMetrics(organizationId, measurement);

        // Then
        assertThat(result).isEqualTo(expectedMetrics);
        verify(influxDBService).queryTimeSeriesData(eq(measurement), 
                eq(Map.of("organization_id", organizationId)), eq("-5m"), eq("mean(column: \"_value\")"));
    }

    @Test
    void getTrendAnalysis_ShouldReturnTrendData() {
        // Given
        String measurement = "deal_metrics";
        String timeRange = "-24h";
        List<Map<String, Object>> expectedTrends = List.of(
                Map.of("_time", "2023-01-01T10:00:00Z", "_value", 100.0),
                Map.of("_time", "2023-01-01T11:00:00Z", "_value", 120.0)
        );
        when(influxDBService.queryTimeSeriesData(eq(measurement), any(), eq(timeRange), 
                eq("aggregateWindow(every: 1h, fn: mean, createEmpty: false)")))
                .thenReturn(expectedTrends);

        // When
        List<Map<String, Object>> result = analyticsQueryService.getTrendAnalysis(organizationId, measurement, timeRange);

        // Then
        assertThat(result).isEqualTo(expectedTrends);
        verify(influxDBService).queryTimeSeriesData(eq(measurement), 
                eq(Map.of("organization_id", organizationId)), eq(timeRange), 
                eq("aggregateWindow(every: 1h, fn: mean, createEmpty: false)"));
    }

    @Test
    void getPerformanceMetrics_ShouldReturnPerformanceData() {
        // Given
        List<Map<String, Object>> queryPerf = List.of(Map.of("avg_duration", 150.0));
        List<Map<String, Object>> dashboardMetrics = List.of(Map.of("views", 25));
        
        when(influxDBService.getPerformanceMetrics("analytics-service", "-1h")).thenReturn(queryPerf);
        when(influxDBService.getDashboardMetrics(organizationId, "-1h")).thenReturn(dashboardMetrics);

        // When
        Map<String, Object> result = analyticsQueryService.getPerformanceMetrics(organizationId);

        // Then
        assertThat(result).containsEntry("queryPerformance", queryPerf);
        assertThat(result).containsEntry("dashboardActivity", dashboardMetrics);
    }
}