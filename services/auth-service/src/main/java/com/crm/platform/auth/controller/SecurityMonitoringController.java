package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.ComplianceReport;
import com.crm.platform.auth.dto.SecurityMetrics;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.service.SecurityAuditService;
import com.crm.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/security")
@Tag(name = "Security Monitoring", description = "Security monitoring, metrics, and compliance reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SecurityMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityMonitoringController.class);

    private final SecurityAuditService securityAuditService;

    @Autowired
    public SecurityMonitoringController(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @GetMapping("/metrics")
    @Operation(
        summary = "Get security metrics",
        description = "Retrieve comprehensive security metrics for a specified time period"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security metrics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<SecurityMetrics>> getSecurityMetrics(
            @Parameter(description = "Start time for metrics period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time for metrics period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            HttpServletRequest request) {

        try {
            logger.debug("Generating security metrics for period {} to {}, tenant: {}", 
                        startTime, endTime, tenantId);

            SecurityMetrics metrics = securityAuditService.generateSecurityMetrics(startTime, endTime, tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(metrics));

        } catch (Exception e) {
            logger.error("Error generating security metrics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate security metrics: " + e.getMessage()));
        }
    }

    @GetMapping("/compliance/report")
    @Operation(
        summary = "Generate compliance report",
        description = "Generate a comprehensive compliance report for audit purposes"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Compliance report generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<ComplianceReport>> generateComplianceReport(
            @Parameter(description = "Report type", required = true)
            @RequestParam ComplianceReport.ReportType reportType,
            
            @Parameter(description = "Start time for report period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time for report period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) String tenantId,
            
            HttpServletRequest request) {

        try {
            logger.debug("Generating compliance report of type {} for period {} to {}, tenant: {}", 
                        reportType, startTime, endTime, tenantId);

            ComplianceReport report = securityAuditService.generateComplianceReport(
                    reportType, tenantId, startTime, endTime);
            
            return ResponseEntity.ok(ApiResponse.success(report));

        } catch (Exception e) {
            logger.error("Error generating compliance report", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate compliance report: " + e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    @Operation(
        summary = "Get security audit logs",
        description = "Retrieve security audit logs with filtering and pagination"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Page<SecurityAuditLog>>> getAuditLogs(
            @Parameter(description = "User ID for filtering (optional)")
            @RequestParam(required = false) UUID userId,
            
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            @Parameter(description = "Event type for filtering (optional)")
            @RequestParam(required = false) String eventType,
            
            @Parameter(description = "Start time for filtering (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time for filtering (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            HttpServletRequest request) {

        try {
            logger.debug("Retrieving audit logs - userId: {}, tenantId: {}, eventType: {}, page: {}, size: {}", 
                        userId, tenantId, eventType, page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<SecurityAuditLog> auditLogs = securityAuditService.getAuditLogs(
                    userId, tenantId, eventType, startTime, endTime, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(auditLogs));

        } catch (Exception e) {
            logger.error("Error retrieving audit logs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve audit logs: " + e.getMessage()));
        }
    }

    @PostMapping("/monitoring/trigger")
    @Operation(
        summary = "Trigger security monitoring",
        description = "Manually trigger suspicious activity monitoring and detection"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security monitoring triggered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> triggerSecurityMonitoring(HttpServletRequest request) {

        try {
            logger.debug("Manually triggering security monitoring");

            securityAuditService.monitorSuspiciousActivity();
            
            return ResponseEntity.ok(ApiResponse.success("Security monitoring triggered successfully"));

        } catch (Exception e) {
            logger.error("Error triggering security monitoring", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to trigger security monitoring: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @Operation(
        summary = "Get security dashboard data",
        description = "Retrieve real-time security dashboard metrics and alerts"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<SecurityMetrics>> getSecurityDashboard(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            HttpServletRequest request) {

        try {
            logger.debug("Retrieving security dashboard data for tenant: {}", tenantId);

            // Get metrics for the last 24 hours for dashboard
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24);
            
            SecurityMetrics dashboardMetrics = securityAuditService.generateSecurityMetrics(
                    startTime, endTime, tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(dashboardMetrics));

        } catch (Exception e) {
            logger.error("Error retrieving security dashboard data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve dashboard data: " + e.getMessage()));
        }
    }

    @GetMapping("/monitoring-alerts")
    @Operation(
        summary = "Get security alerts",
        description = "Retrieve recent security alerts and suspicious activities"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security alerts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Page<SecurityAuditLog>>> getSecurityAlerts(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            @Parameter(description = "Hours to look back for alerts")
            @RequestParam(defaultValue = "24") int hours,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            HttpServletRequest request) {

        try {
            logger.debug("Retrieving security alerts for tenant: {}, hours: {}", tenantId, hours);

            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            Pageable pageable = PageRequest.of(page, size);
            
            // Get suspicious activity events
            Page<SecurityAuditLog> alerts = securityAuditService.getAuditLogs(
                    null, tenantId, "BRUTE_FORCE_ATTEMPT", startTime, null, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(alerts));

        } catch (Exception e) {
            logger.error("Error retrieving security alerts", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve security alerts: " + e.getMessage()));
        }
    }

    @GetMapping("/events/search")
    @Operation(
        summary = "Search security events",
        description = "Advanced search and filtering of security events"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security events retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Page<SecurityAuditLog>>> searchSecurityEvents(
            @Parameter(description = "Search query")
            @RequestParam(required = false) String query,
            
            @Parameter(description = "Event types (comma-separated)")
            @RequestParam(required = false) String eventTypes,
            
            @Parameter(description = "User ID filter")
            @RequestParam(required = false) UUID userId,
            
            @Parameter(description = "Tenant ID filter")
            @RequestParam(required = false) UUID tenantId,
            
            @Parameter(description = "IP address filter")
            @RequestParam(required = false) String ipAddress,
            
            @Parameter(description = "Start time")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            HttpServletRequest request) {

        try {
            logger.debug("Searching security events with query: {}", query);

            Pageable pageable = PageRequest.of(page, size);
            
            // Use default time range if not provided
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            // For now, use the basic audit log search
            // In a real implementation, this would use more sophisticated search
            Page<SecurityAuditLog> events = securityAuditService.getAuditLogs(
                    userId, tenantId, null, startTime, endTime, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(events));

        } catch (Exception e) {
            logger.error("Error searching security events", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to search security events: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get security summary",
        description = "Get a comprehensive security summary for dashboard"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecuritySummary(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            @Parameter(description = "Hours to look back")
            @RequestParam(defaultValue = "24") int hours,
            
            HttpServletRequest request) {

        try {
            logger.debug("Generating security summary for tenant: {}", tenantId);

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);
            
            // Get metrics
            SecurityMetrics metrics = securityAuditService.generateSecurityMetrics(startTime, endTime, tenantId);
            
            // Create summary
            Map<String, Object> summary = new HashMap<>();
            summary.put("period_start", startTime);
            summary.put("period_end", endTime);
            summary.put("total_events", metrics.getTotalLoginAttempts());
            summary.put("successful_logins", metrics.getSuccessfulLogins());
            summary.put("failed_logins", metrics.getFailedLogins());
            summary.put("success_rate", metrics.getSuccessRate());
            summary.put("suspicious_activities", metrics.getSuspiciousActivities());
            summary.put("account_lockouts", metrics.getAccountLockouts());
            summary.put("brute_force_attempts", metrics.getBruteForceAttempts());
            summary.put("unique_users", metrics.getUniqueUsers());
            summary.put("unique_ips", metrics.getUniqueIpAddresses());
            summary.put("active_sessions", metrics.getActiveSessions());
            
            // Add trend indicators (simplified)
            summary.put("trends", Map.of(
                "login_attempts", metrics.getTotalLoginAttempts() > 0 ? "stable" : "low",
                "security_incidents", metrics.getSuspiciousActivities() > 10 ? "high" : "normal",
                "success_rate", metrics.getSuccessRate() > 90 ? "good" : "concerning"
            ));
            
            return ResponseEntity.ok(ApiResponse.success(summary));

        } catch (Exception e) {
            logger.error("Error generating security summary", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate security summary: " + e.getMessage()));
        }
    }
}