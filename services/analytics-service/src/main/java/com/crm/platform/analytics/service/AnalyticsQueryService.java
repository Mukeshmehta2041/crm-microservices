package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.AnalyticsQueryRequest;
import com.crm.platform.analytics.dto.AnalyticsQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsQueryService {

    private final InfluxDBService influxDBService;
    private final AnalyticsCacheService cacheService;

    public AnalyticsQueryResponse executeQuery(AnalyticsQueryRequest request, String organizationId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Generate cache key
            String queryHash = cacheService.generateQueryHash(request.getQuery(), request.getParameters());
            
            // Check cache first
            Optional<List<Map<String, Object>>> cachedResult = cacheService.getCachedQueryResult(queryHash);
            if (cachedResult.isPresent()) {
                return buildResponse(cachedResult.get(), request, startTime, true);
            }
            
            // Execute query
            List<Map<String, Object>> data = executeInfluxQuery(request, organizationId);
            
            // Cache the result
            if (cacheService.isCacheEnabled()) {
                cacheService.cacheQueryResult(queryHash, data);
            }
            
            return buildResponse(data, request, startTime, false);
            
        } catch (Exception e) {
            log.error("Error executing analytics query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute analytics query", e);
        }
    }

    public CompletableFuture<AnalyticsQueryResponse> executeQueryAsync(AnalyticsQueryRequest request, String organizationId) {
        return CompletableFuture.supplyAsync(() -> executeQuery(request, organizationId));
    }

    private List<Map<String, Object>> executeInfluxQuery(AnalyticsQueryRequest request, String organizationId) {
        String fluxQuery = buildFluxQuery(request, organizationId);
        return influxDBService.query(fluxQuery);
    }

    private String buildFluxQuery(AnalyticsQueryRequest request, String organizationId) {
        StringBuilder queryBuilder = new StringBuilder();
        
        // Base query with bucket and range
        queryBuilder.append("from(bucket: \"analytics\")");
        
        // Time range
        if (request.getStartTime() != null) {
            queryBuilder.append(" |> range(start: ").append(request.getStartTime()).append(")");
        } else {
            queryBuilder.append(" |> range(start: -1h)"); // Default to last hour
        }
        
        if (request.getEndTime() != null) {
            queryBuilder.append(" |> range(stop: ").append(request.getEndTime()).append(")");
        }
        
        // Organization filter
        queryBuilder.append(" |> filter(fn: (r) => r.organization_id == \"").append(organizationId).append("\")");
        
        // Custom query filters
        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            // Parse and apply custom filters from the query
            String customFilters = parseCustomQuery(request.getQuery());
            if (!customFilters.isEmpty()) {
                queryBuilder.append(customFilters);
            }
        }
        
        // Group by
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            queryBuilder.append(" |> group(columns: [\"").append(request.getGroupBy()).append("\"])");
        }
        
        // Aggregation
        if (request.getAggregation() != null && !request.getAggregation().isEmpty()) {
            queryBuilder.append(" |> ").append(request.getAggregation()).append("()");
        }
        
        // Order by
        if (request.getOrderBy() != null && !request.getOrderBy().isEmpty()) {
            queryBuilder.append(" |> sort(columns: [\"").append(request.getOrderBy()).append("\"])");
        }
        
        // Limit
        if (request.getLimit() != null && request.getLimit() > 0) {
            queryBuilder.append(" |> limit(n: ").append(request.getLimit()).append(")");
        }
        
        return queryBuilder.toString();
    }

    private String parseCustomQuery(String query) {
        // Simple query parser - in production, this would be more sophisticated
        StringBuilder filters = new StringBuilder();
        
        // Handle measurement filter
        if (query.contains("measurement:")) {
            String measurement = extractValue(query, "measurement:");
            filters.append(" |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")");
        }
        
        // Handle field filters
        if (query.contains("field:")) {
            String field = extractValue(query, "field:");
            filters.append(" |> filter(fn: (r) => r._field == \"").append(field).append("\")");
        }
        
        // Handle tag filters
        String[] parts = query.split(" ");
        for (String part : parts) {
            if (part.contains("=") && !part.contains("measurement:") && !part.contains("field:")) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    filters.append(" |> filter(fn: (r) => r.").append(keyValue[0])
                           .append(" == \"").append(keyValue[1]).append("\")");
                }
            }
        }
        
        return filters.toString();
    }

    private String extractValue(String query, String prefix) {
        int startIndex = query.indexOf(prefix) + prefix.length();
        int endIndex = query.indexOf(" ", startIndex);
        if (endIndex == -1) {
            endIndex = query.length();
        }
        return query.substring(startIndex, endIndex);
    }

    private AnalyticsQueryResponse buildResponse(List<Map<String, Object>> data, 
                                               AnalyticsQueryRequest request, 
                                               long startTime, 
                                               boolean fromCache) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        return AnalyticsQueryResponse.builder()
                .data(data)
                .metadata(AnalyticsQueryResponse.QueryMetadata.builder()
                        .totalRecords(data.size())
                        .returnedRecords(data.size())
                        .query(request.getQuery())
                        .parameters(request.getParameters())
                        .fromCache(fromCache)
                        .cacheExpiry(fromCache ? LocalDateTime.now().plusSeconds(cacheService.getCacheTtl()) : null)
                        .build())
                .executedAt(LocalDateTime.now())
                .executionTimeMs(executionTime)
                .build();
    }

    public List<Map<String, Object>> getRealtimeMetrics(String organizationId, String measurement) {
        return influxDBService.queryTimeSeriesData(measurement, 
                Map.of("organization_id", organizationId), 
                "-5m", 
                "mean(column: \"_value\")");
    }

    public List<Map<String, Object>> getTrendAnalysis(String organizationId, String measurement, String timeRange) {
        Map<String, String> tags = Map.of("organization_id", organizationId);
        return influxDBService.queryTimeSeriesData(measurement, tags, timeRange, 
                "aggregateWindow(every: 1h, fn: mean, createEmpty: false)");
    }

    public Map<String, Object> getPerformanceMetrics(String organizationId) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Query execution performance
        List<Map<String, Object>> queryPerf = influxDBService.getPerformanceMetrics("analytics-service", "-1h");
        metrics.put("queryPerformance", queryPerf);
        
        // Dashboard metrics
        List<Map<String, Object>> dashboardMetrics = influxDBService.getDashboardMetrics(organizationId, "-1h");
        metrics.put("dashboardActivity", dashboardMetrics);
        
        return metrics;
    }
}