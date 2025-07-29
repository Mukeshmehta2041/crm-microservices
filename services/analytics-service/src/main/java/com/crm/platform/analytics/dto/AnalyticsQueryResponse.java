package com.crm.platform.analytics.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsQueryResponse {

    private List<Map<String, Object>> data;
    private QueryMetadata metadata;
    private LocalDateTime executedAt;
    private Long executionTimeMs;

    @Data
    @Builder
    public static class QueryMetadata {
        private Integer totalRecords;
        private Integer returnedRecords;
        private String query;
        private Map<String, Object> parameters;
        private Boolean fromCache;
        private LocalDateTime cacheExpiry;
    }
}