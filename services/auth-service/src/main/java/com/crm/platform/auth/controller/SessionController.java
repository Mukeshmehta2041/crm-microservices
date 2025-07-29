package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.DeviceInfo;
import com.crm.platform.auth.dto.SessionInfo;
import com.crm.platform.auth.dto.SessionValidationRequest;
import com.crm.platform.auth.dto.SuspiciousActivityAlert;
import com.crm.platform.auth.service.DeviceLocationService;
import com.crm.platform.auth.service.SessionService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import com.crm.platform.common.monitoring.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Session management controller providing comprehensive session lifecycle management,
 * device tracking, security monitoring, and session analytics endpoints.
 */
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Session Management", description = "Session lifecycle and security management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private DeviceLocationService deviceLocationService;

    // ==================== Session Listing ====================

    @GetMapping
    @Operation(summary = "List User Sessions", description = "Get all sessions for the authenticated user")
    @Timed(value = "auth.session.list", description = "Session listing operation")
    @Monitored("session-list")
    @SecurityLog(operation = "session-list", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<List<SessionInfo>>> listUserSessions(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization) {

        List<SessionInfo> sessions = sessionService.listUserSessions(authorization);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/paginated")
    @Operation(summary = "List Sessions with Pagination", description = "Get paginated list of user sessions")
    @SecurityLog(operation = "session-list-paginated", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated sessions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Page<SessionInfo>>> listUserSessionsPaginated(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(value = "direction", defaultValue = "desc") String direction,
            @Parameter(description = "Filter by session status")
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by device type")
            @RequestParam(value = "device_type", required = false) String deviceType) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // For now, we'll return the basic list wrapped in a Page
        // In a full implementation, this would use repository pagination
        List<SessionInfo> sessions = sessionService.listUserSessions(authorization);
        
        // Apply filters if provided
        if (status != null) {
            sessions = sessions.stream()
                .filter(session -> status.equalsIgnoreCase(session.getStatus()))
                .toList();
        }
        
        if (deviceType != null) {
            sessions = sessions.stream()
                .filter(session -> deviceType.equalsIgnoreCase(session.getDeviceType()))
                .toList();
        }

        // Simple pagination implementation
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sessions.size());
        List<SessionInfo> pageContent = sessions.subList(start, end);
        
        Page<SessionInfo> pageResult = new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, sessions.size());

        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    // ==================== Current Session ====================

    @GetMapping("/current")
    @Operation(summary = "Get Current Session", description = "Get detailed information about the current session")
    @Timed(value = "auth.session.current", description = "Current session retrieval")
    @SecurityLog(operation = "session-current", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current session retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Current session not found")
    })
    public ResponseEntity<ApiResponse<SessionInfo>> getCurrentSession(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization) {

        SessionInfo currentSession = sessionService.getCurrentSession(authorization);
        return ResponseEntity.ok(ApiResponse.success(currentSession));
    }

    @GetMapping("/current/detailed")
    @Operation(summary = "Get Current Session with Device Info", description = "Get current session with comprehensive device and location information")
    @Monitored("session-current-detailed")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detailed session information retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentSessionDetailed(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            HttpServletRequest request) {

        SessionInfo currentSession = sessionService.getCurrentSession(authorization);
        DeviceInfo deviceInfo = deviceLocationService.createDeviceInfo(request);

        Map<String, Object> detailedInfo = Map.of(
            "session", currentSession,
            "device_info", deviceInfo,
            "request_timestamp", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(detailedInfo));
    }

    // ==================== Session Validation ====================

    @PostMapping("/validate")
    @Operation(summary = "Validate Session", description = "Validate session token and return session status")
    @Timed(value = "auth.session.validate", description = "Session validation operation")
    @Monitored("session-validate")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session validation completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid validation request")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSession(
            @Parameter(description = "Session validation request", required = true)
            @Valid @RequestBody SessionValidationRequest request) {

        Map<String, Object> validationResult = sessionService.validateSession(request);
        return ResponseEntity.ok(ApiResponse.success(validationResult));
    }

    @PostMapping("/validate-and-renew")
    @Operation(summary = "Validate and Renew Session", description = "Validate session and extend expiry if valid")
    @Monitored("session-validate-renew")
    @SecurityLog(operation = "session-validate-renew", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session validated and renewed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid session or renewal failed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateAndRenewSession(
            @Parameter(description = "Session validation request", required = true)
            @Valid @RequestBody SessionValidationRequest request) {

        Map<String, Object> validationResult = sessionService.validateSession(request);
        
        if ((Boolean) validationResult.get("valid")) {
            // Renew the session if it's valid
            sessionService.renewSession(request.getSessionId());
            validationResult.put("renewed", true);
            validationResult.put("renewed_at", java.time.LocalDateTime.now());
        }

        return ResponseEntity.ok(ApiResponse.success(validationResult));
    }

    // ==================== Session Termination ====================

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Terminate Specific Session", description = "Terminate a specific session by ID")
    @Monitored("session-terminate")
    @SecurityLog(operation = "session-terminate", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session terminated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cannot terminate another user's session"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> terminateSession(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Session ID to terminate", required = true)
            @PathVariable @NotBlank String sessionId,
            HttpServletRequest request) {

        Map<String, Object> result = sessionService.terminateSession(authorization, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping
    @Operation(summary = "Terminate All Sessions", description = "Terminate all user sessions except the current one")
    @Monitored("session-terminate-all")
    @SecurityLog(operation = "session-terminate-all", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All sessions terminated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> terminateAllSessions(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            HttpServletRequest request) {

        Map<String, Object> result = sessionService.terminateAllSessions(authorization, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/terminate-by-criteria")
    @Operation(summary = "Terminate Sessions by Criteria", description = "Terminate sessions matching specific criteria")
    @SecurityLog(operation = "session-terminate-criteria", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sessions terminated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid termination criteria")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> terminateSessionsByCriteria(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Device type to terminate")
            @RequestParam(value = "device_type", required = false) String deviceType,
            @Parameter(description = "Location to terminate")
            @RequestParam(value = "location", required = false) String location,
            @Parameter(description = "Older than hours")
            @RequestParam(value = "older_than_hours", required = false) Integer olderThanHours,
            HttpServletRequest request) {

        // This would implement criteria-based session termination
        // For now, return a placeholder response
        Map<String, Object> result = Map.of(
            "message", "Criteria-based termination not yet implemented",
            "criteria", Map.of(
                "device_type", deviceType,
                "location", location,
                "older_than_hours", olderThanHours
            )
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Session Renewal ====================

    @PostMapping("/{sessionId}/renew")
    @Operation(summary = "Renew Session", description = "Extend the expiry time of a specific session")
    @Monitored("session-renew")
    @SecurityLog(operation = "session-renew", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session renewed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot renew inactive session")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> renewSession(
            @Parameter(description = "Session ID to renew", required = true)
            @PathVariable @NotBlank String sessionId) {

        sessionService.renewSession(sessionId);
        
        Map<String, Object> result = Map.of(
            "renewed", true,
            "session_id", sessionId,
            "renewed_at", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Session Security ====================

    @PostMapping("/security-check")
    @Operation(summary = "Perform Security Check", description = "Check current session for suspicious activity")
    @Monitored("session-security-check")
    @SecurityLog(operation = "session-security-check", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security check completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> performSecurityCheck(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam UUID tenantId,
            HttpServletRequest request) {

        DeviceInfo currentDevice = deviceLocationService.createDeviceInfo(request);
        List<SuspiciousActivityAlert> alerts = deviceLocationService.detectSuspiciousActivity(
            userId, tenantId, currentDevice);

        Map<String, Object> securityCheck = Map.of(
            "alerts", alerts,
            "alert_count", alerts.size(),
            "risk_level", alerts.isEmpty() ? "LOW" : 
                alerts.stream().anyMatch(a -> a.getRiskLevel().getSeverity() >= 3) ? "HIGH" : "MEDIUM",
            "device_info", currentDevice,
            "check_timestamp", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(securityCheck));
    }

    @GetMapping("/security-events")
    @Operation(summary = "Get Session Security Events", description = "Get security events related to user sessions")
    @SecurityLog(operation = "session-security-events", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security events retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityEvents(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam UUID tenantId,
            @Parameter(description = "Hours to look back", example = "24")
            @RequestParam(value = "hours", defaultValue = "24") int hours) {

        // This would retrieve security events from the audit service
        // For now, return a placeholder response
        Map<String, Object> events = Map.of(
            "events", List.of(),
            "period_hours", hours,
            "total_events", 0,
            "retrieved_at", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    // ==================== Session Analytics ====================

    @GetMapping("/analytics")
    @Operation(summary = "Get Session Analytics", description = "Get comprehensive session usage analytics")
    @Monitored("session-analytics")
    @SecurityLog(operation = "session-analytics", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session analytics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionAnalytics(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam UUID tenantId,
            @Parameter(description = "Analysis period in days", example = "30")
            @RequestParam(value = "days", defaultValue = "30") int days) {

        Map<String, Object> deviceStats = deviceLocationService.getDeviceStatistics(userId, tenantId);
        
        // Enhance with additional analytics
        Map<String, Object> analytics = Map.of(
            "device_statistics", deviceStats,
            "analysis_period_days", days,
            "generated_at", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get Session Summary", description = "Get a summary of current session status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionSummary(
            @Parameter(description = "Authorization header", required = true)
            @RequestHeader("Authorization") String authorization) {

        List<SessionInfo> sessions = sessionService.listUserSessions(authorization);
        
        long activeSessions = sessions.stream()
            .filter(session -> "ACTIVE".equals(session.getStatus()))
            .count();
        
        Map<String, Long> deviceBreakdown = sessions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                session -> session.getDeviceType() != null ? session.getDeviceType() : "Unknown",
                java.util.stream.Collectors.counting()
            ));

        Map<String, Object> summary = Map.of(
            "total_sessions", sessions.size(),
            "active_sessions", activeSessions,
            "device_breakdown", deviceBreakdown,
            "current_session_id", sessions.stream()
                .filter(SessionInfo::isCurrent)
                .map(SessionInfo::getSessionId)
                .findFirst()
                .orElse("unknown"),
            "summary_timestamp", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== Session Cleanup ====================

    @PostMapping("/cleanup")
    @Operation(summary = "Trigger Session Cleanup", description = "Manually trigger cleanup of expired sessions")
    @SecurityLog(operation = "session-cleanup", type = SecurityLog.SecurityType.MAINTENANCE, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session cleanup completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSessionCleanup() {
        
        sessionService.cleanupExpiredSessions();
        
        Map<String, Object> result = Map.of(
            "cleanup_completed", true,
            "cleanup_timestamp", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Session Service Health", description = "Check session management service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "session-management",
            "timestamp", java.time.Instant.now()
        )));
    }
}