package com.crm.platform.analytics.service;

import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluxDBService {

    private final WriteApi writeApi;
    private final QueryApi queryApi;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String org;

    public void writeMetric(String measurement, Map<String, String> tags, Map<String, Object> fields) {
        writeMetric(measurement, tags, fields, Instant.now());
    }

    public void writeMetric(String measurement, Map<String, String> tags, Map<String, Object> fields, Instant timestamp) {
        try {
            Point point = Point.measurement(measurement)
                    .time(timestamp, WritePrecision.NS);

            // Add tags
            if (tags != null) {
                tags.forEach(point::addTag);
            }

            // Add fields
            if (fields != null) {
                fields.forEach((key, value) -> {
                    if (value instanceof String) {
                        point.addField(key, (String) value);
                    } else if (value instanceof Number) {
                        point.addField(key, (Number) value);
                    } else if (value instanceof Boolean) {
                        point.addField(key, (Boolean) value);
                    } else {
                        point.addField(key, value.toString());
                    }
                });
            }

            writeApi.writePoint(point);
            log.debug("Written metric: {} with tags: {} and fields: {}", measurement, tags, fields);
        } catch (Exception e) {
            log.error("Error writing metric to InfluxDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to write metric to InfluxDB", e);
        }
    }

    public List<Map<String, Object>> query(String fluxQuery) {
        try {
            log.debug("Executing InfluxDB query: {}", fluxQuery);
            List<FluxTable> tables = queryApi.query(fluxQuery, org);
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> row = new HashMap<>();
                    
                    // Add all values from the record
                    record.getValues().forEach((key, value) -> {
                        if (value != null && !key.startsWith("_") || key.equals("_time") || key.equals("_value")) {
                            row.put(key, value);
                        }
                    });
                    
                    results.add(row);
                }
            }
            
            log.debug("Query returned {} records", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error executing InfluxDB query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute InfluxDB query", e);
        }
    }

    public List<Map<String, Object>> queryTimeSeriesData(String measurement, Map<String, String> tags, 
                                                         String timeRange, String aggregation) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("from(bucket: \"").append(bucket).append("\")");
        queryBuilder.append(" |> range(start: ").append(timeRange).append(")");
        queryBuilder.append(" |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")");
        
        if (tags != null && !tags.isEmpty()) {
            tags.forEach((key, value) -> 
                queryBuilder.append(" |> filter(fn: (r) => r.").append(key).append(" == \"").append(value).append("\")")
            );
        }
        
        if (aggregation != null && !aggregation.isEmpty()) {
            queryBuilder.append(" |> ").append(aggregation);
        }
        
        return query(queryBuilder.toString());
    }

    public void writeUserActivity(String userId, String organizationId, String activity, Map<String, Object> metadata) {
        Map<String, String> tags = Map.of(
                "user_id", userId,
                "organization_id", organizationId,
                "activity", activity
        );
        
        Map<String, Object> fields = new HashMap<>();
        fields.put("count", 1);
        if (metadata != null) {
            fields.putAll(metadata);
        }
        
        writeMetric("user_activity", tags, fields);
    }

    public void writePerformanceMetric(String service, String operation, long duration, boolean success) {
        Map<String, String> tags = Map.of(
                "service", service,
                "operation", operation,
                "status", success ? "success" : "error"
        );
        
        Map<String, Object> fields = Map.of(
                "duration_ms", duration,
                "count", 1
        );
        
        writeMetric("performance", tags, fields);
    }

    public List<Map<String, Object>> getDashboardMetrics(String organizationId, String timeRange) {
        String query = String.format("""
            from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r._measurement == "user_activity")
              |> filter(fn: (r) => r.organization_id == "%s")
              |> group(columns: ["activity"])
              |> count()
            """, bucket, timeRange, organizationId);
        
        return query(query);
    }

    public List<Map<String, Object>> getPerformanceMetrics(String service, String timeRange) {
        String query = String.format("""
            from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r._measurement == "performance")
              |> filter(fn: (r) => r.service == "%s")
              |> group(columns: ["operation", "status"])
              |> mean(column: "_value")
            """, bucket, timeRange, service);
        
        return query(query);
    }
}