package com.crm.platform.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void sendDashboardUpdate(String organizationId, Long dashboardId, Object data) {
        String destination = String.format("/topic/dashboard/%s/%d", organizationId, dashboardId);
        messagingTemplate.convertAndSend(destination, data);
        log.debug("Sent dashboard update to: {}", destination);
    }

    @Async
    public void sendWidgetUpdate(String organizationId, Long dashboardId, Long widgetId, Object data) {
        String destination = String.format("/topic/widget/%s/%d/%d", organizationId, dashboardId, widgetId);
        messagingTemplate.convertAndSend(destination, data);
        log.debug("Sent widget update to: {}", destination);
    }

    @Async
    public void sendReportUpdate(String organizationId, Long reportId, String status, Object data) {
        String destination = String.format("/topic/report/%s/%d", organizationId, reportId);
        Map<String, Object> message = Map.of(
                "reportId", reportId,
                "status", status,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent report update to: {}", destination);
    }

    @Async
    public void sendAnalyticsAlert(String organizationId, String alertType, Object data) {
        String destination = String.format("/topic/alerts/%s", organizationId);
        Map<String, Object> message = Map.of(
                "type", alertType,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent analytics alert to: {}", destination);
    }

    @Async
    public void sendUserNotification(String userId, String message, String type) {
        String destination = String.format("/user/%s/notifications", userId);
        Map<String, Object> notification = Map.of(
                "message", message,
                "type", type,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSendToUser(userId, "/notifications", notification);
        log.debug("Sent user notification to: {}", userId);
    }

    @Async
    public void broadcastSystemMetrics(Object metrics) {
        messagingTemplate.convertAndSend("/topic/system/metrics", metrics);
        log.debug("Broadcasted system metrics");
    }
}