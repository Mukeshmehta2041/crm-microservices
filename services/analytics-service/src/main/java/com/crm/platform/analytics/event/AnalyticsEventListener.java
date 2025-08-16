package com.crm.platform.analytics.event;

import com.crm.platform.analytics.service.InfluxDBService;
import com.crm.platform.analytics.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

    private final InfluxDBService influxDBService;
    private final WebSocketService webSocketService;

    @KafkaListener(topics = "user-activity", groupId = "analytics-service")
    public void handleUserActivity(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String organizationId = (String) event.get("organizationId");
            String activity = (String) event.get("activity");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) event.getOrDefault("metadata", Map.of());
            
            influxDBService.writeUserActivity(userId, organizationId, activity, metadata);
            log.debug("Processed user activity event: {} for user: {}", activity, userId);
            
        } catch (Exception e) {
            log.error("Error processing user activity event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "contact-created", groupId = "analytics-service")
    public void handleContactCreated(Map<String, Object> event) {
        try {
            String organizationId = (String) event.get("organizationId");
            String userId = (String) event.get("createdBy");
            
            Map<String, String> tags = Map.of(
                    "organization_id", organizationId,
                    "user_id", userId,
                    "entity_type", "contact",
                    "action", "created"
            );
            
            Map<String, Object> fields = Map.of(
                    "count", 1,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            influxDBService.writeMetric("entity_operations", tags, fields);
            log.debug("Processed contact created event for organization: {}", organizationId);
            
        } catch (Exception e) {
            log.error("Error processing contact created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "deal-updated", groupId = "analytics-service")
    public void handleDealUpdated(Map<String, Object> event) {
        try {
            String organizationId = (String) event.get("organizationId");
            String userId = (String) event.get("updatedBy");
            String dealId = (String) event.get("dealId");
            Object value = event.get("value");
            String stage = (String) event.get("stage");
            
            Map<String, String> tags = Map.of(
                    "organization_id", organizationId,
                    "user_id", userId,
                    "entity_type", "deal",
                    "action", "updated",
                    "stage", stage != null ? stage : "unknown"
            );
            
            Map<String, Object> fields = Map.of(
                    "count", 1,
                    "deal_value", value != null ? value : 0,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            influxDBService.writeMetric("entity_operations", tags, fields);
            influxDBService.writeMetric("deal_metrics", tags, fields);
            
            log.debug("Processed deal updated event for deal: {} in organization: {}", dealId, organizationId);
            
        } catch (Exception e) {
            log.error("Error processing deal updated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "pipeline-stage-changed", groupId = "analytics-service")
    public void handlePipelineStageChanged(Map<String, Object> event) {
        try {
            String organizationId = (String) event.get("organizationId");
            String pipelineId = (String) event.get("pipelineId");
            String fromStage = (String) event.get("fromStage");
            String toStage = (String) event.get("toStage");
            String dealId = (String) event.get("dealId");
            
            Map<String, String> tags = Map.of(
                    "organization_id", organizationId,
                    "pipeline_id", pipelineId,
                    "from_stage", fromStage != null ? fromStage : "unknown",
                    "to_stage", toStage != null ? toStage : "unknown"
            );
            
            Map<String, Object> fields = Map.of(
                    "count", 1,
                    "deal_id", dealId,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            influxDBService.writeMetric("pipeline_transitions", tags, fields);
            log.debug("Processed pipeline stage change for deal: {} in organization: {}", dealId, organizationId);
            
        } catch (Exception e) {
            log.error("Error processing pipeline stage change event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "report-executed", groupId = "analytics-service")
    public void handleReportExecuted(Map<String, Object> event) {
        try {
            String organizationId = (String) event.get("organizationId");
            String reportId = (String) event.get("reportId");
            String status = (String) event.get("status");
            Long executionTime = (Long) event.get("executionTimeMs");
            
            Map<String, String> tags = Map.of(
                    "organization_id", organizationId,
                    "report_id", reportId,
                    "status", status
            );
            
            Map<String, Object> fields = Map.of(
                    "count", 1,
                    "execution_time_ms", executionTime != null ? executionTime : 0,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            influxDBService.writeMetric("report_executions", tags, fields);
            
            // Send real-time update via WebSocket
            webSocketService.sendReportUpdate(organizationId, Long.parseLong(reportId), status, event);
            
            log.debug("Processed report execution event for report: {} in organization: {}", reportId, organizationId);
            
        } catch (Exception e) {
            log.error("Error processing report execution event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "dashboard-viewed", groupId = "analytics-service")
    public void handleDashboardViewed(Map<String, Object> event) {
        try {
            String organizationId = (String) event.get("organizationId");
            String userId = (String) event.get("userId");
            String dashboardId = (String) event.get("dashboardId");
            
            Map<String, String> tags = Map.of(
                    "organization_id", organizationId,
                    "user_id", userId,
                    "dashboard_id", dashboardId,
                    "action", "viewed"
            );
            
            Map<String, Object> fields = Map.of(
                    "count", 1,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            influxDBService.writeMetric("dashboard_usage", tags, fields);
            log.debug("Processed dashboard viewed event for dashboard: {} by user: {}", dashboardId, userId);
            
        } catch (Exception e) {
            log.error("Error processing dashboard viewed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "system-performance", groupId = "analytics-service")
    public void handleSystemPerformance(Map<String, Object> event) {
        try {
            String service = (String) event.get("service");
            String operation = (String) event.get("operation");
            Long duration = (Long) event.get("duration");
            Boolean success = (Boolean) event.get("success");
            
            influxDBService.writePerformanceMetric(service, operation, duration, success);
            log.debug("Processed system performance event for service: {} operation: {}", service, operation);
            
        } catch (Exception e) {
            log.error("Error processing system performance event: {}", e.getMessage(), e);
        }
    }
}