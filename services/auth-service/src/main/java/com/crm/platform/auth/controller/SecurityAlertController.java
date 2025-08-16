package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.SecurityAlert;
import com.crm.platform.auth.service.SecurityAlertService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/security/alerts")
@Tag(name = "Security Alert Management", description = "Security alert management and monitoring endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SecurityAlertController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertController.class);

    private final SecurityAlertService securityAlertService;

    @Autowired
    public SecurityAlertController(SecurityAlertService securityAlertService) {
        this.securityAlertService = securityAlertService;
    }

    @GetMapping
    @Operation(
        summary = "Get security alerts",
        description = "Retrieve security alerts with optional filtering"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alerts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<List<SecurityAlert>>> getAlerts(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            @Parameter(description = "Alert status filter (optional)")
            @RequestParam(required = false) SecurityAlert.AlertStatus status,
            
            @Parameter(description = "Alert type filter (optional)")
            @RequestParam(required = false) SecurityAlert.AlertType alertType,
            
            @Parameter(description = "Only high priority alerts")
            @RequestParam(defaultValue = "false") boolean highPriorityOnly,
            
            HttpServletRequest request) {

        try {
            List<SecurityAlert> alerts;
            
            if (highPriorityOnly) {
                alerts = securityAlertService.getHighPriorityAlerts(tenantId);
            } else if (status != null) {
                alerts = securityAlertService.getAlertsByStatus(status, tenantId);
            } else if (alertType != null) {
                alerts = securityAlertService.getAlertsByType(alertType, tenantId);
            } else {
                alerts = securityAlertService.getActiveAlerts(tenantId);
            }
            
            return ResponseEntity.ok(ApiResponse.success(alerts));

        } catch (Exception e) {
            logger.error("Error retrieving security alerts", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve security alerts: " + e.getMessage()));
        }
    }

    @GetMapping("/{alertId}")
    @Operation(
        summary = "Get security alert by ID",
        description = "Retrieve a specific security alert by its ID"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<SecurityAlert>> getAlertById(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable String alertId,
            
            HttpServletRequest request) {

        try {
            Optional<SecurityAlert> alert = securityAlertService.getAlertById(alertId);
            
            if (alert.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(alert.get()));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving security alert: {}", alertId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve security alert: " + e.getMessage()));
        }
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(
        summary = "Resolve security alert",
        description = "Mark a security alert as resolved"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert resolved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> resolveAlert(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable String alertId,
            
            @Parameter(description = "Resolution notes")
            @RequestParam(required = false, defaultValue = "") String notes,
            
            HttpServletRequest request) {

        try {
            // Get current user ID from security context
            UUID resolvedBy = getCurrentUserId(request);
            
            boolean success = securityAlertService.resolveAlert(alertId, resolvedBy, notes);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Alert resolved successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error resolving security alert: {}", alertId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to resolve security alert: " + e.getMessage()));
        }
    }

    @PostMapping("/{alertId}/false-positive")
    @Operation(
        summary = "Mark alert as false positive",
        description = "Mark a security alert as a false positive"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert marked as false positive successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> markAsFalsePositive(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable String alertId,
            
            @Parameter(description = "Notes explaining why this is a false positive")
            @RequestParam(required = false, defaultValue = "") String notes,
            
            HttpServletRequest request) {

        try {
            // Get current user ID from security context
            UUID resolvedBy = getCurrentUserId(request);
            
            boolean success = securityAlertService.markAsFalsePositive(alertId, resolvedBy, notes);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Alert marked as false positive"));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error marking security alert as false positive: {}", alertId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to mark alert as false positive: " + e.getMessage()));
        }
    }

    @PutMapping("/{alertId}/status")
    @Operation(
        summary = "Update alert status",
        description = "Update the status of a security alert"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> updateAlertStatus(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable String alertId,
            
            @Parameter(description = "New status", required = true)
            @RequestParam SecurityAlert.AlertStatus status,
            
            HttpServletRequest request) {

        try {
            boolean success = securityAlertService.updateAlertStatus(alertId, status);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Alert status updated successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error updating security alert status: {}", alertId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update alert status: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get alert statistics",
        description = "Retrieve comprehensive statistics about security alerts"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertStatistics(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            HttpServletRequest request) {

        try {
            Map<String, Object> statistics = securityAlertService.getAlertStatistics(tenantId);
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            logger.error("Error retrieving alert statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve alert statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    @Operation(
        summary = "Get active alerts",
        description = "Retrieve all currently active (open) security alerts"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active alerts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<List<SecurityAlert>>> getActiveAlerts(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            HttpServletRequest request) {

        try {
            List<SecurityAlert> alerts = securityAlertService.getActiveAlerts(tenantId);
            return ResponseEntity.ok(ApiResponse.success(alerts));

        } catch (Exception e) {
            logger.error("Error retrieving active alerts", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve active alerts: " + e.getMessage()));
        }
    }

    @GetMapping("/high-priority")
    @Operation(
        summary = "Get high priority alerts",
        description = "Retrieve high priority security alerts that require immediate attention"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "High priority alerts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<List<SecurityAlert>>> getHighPriorityAlerts(
            @Parameter(description = "Tenant ID for filtering (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            HttpServletRequest request) {

        try {
            List<SecurityAlert> alerts = securityAlertService.getHighPriorityAlerts(tenantId);
            return ResponseEntity.ok(ApiResponse.success(alerts));

        } catch (Exception e) {
            logger.error("Error retrieving high priority alerts", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve high priority alerts: " + e.getMessage()));
        }
    }

    @PostMapping("/cleanup")
    @Operation(
        summary = "Cleanup expired alerts",
        description = "Manually trigger cleanup of expired security alerts"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredAlerts(HttpServletRequest request) {
        try {
            securityAlertService.cleanupExpiredAlerts();
            return ResponseEntity.ok(ApiResponse.success("Alert cleanup completed successfully"));

        } catch (Exception e) {
            logger.error("Error during alert cleanup", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to cleanup alerts: " + e.getMessage()));
        }
    }

    @PostMapping("/auto-resolve")
    @Operation(
        summary = "Auto-resolve old alerts",
        description = "Manually trigger auto-resolution of old alerts"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Auto-resolve completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> autoResolveOldAlerts(HttpServletRequest request) {
        try {
            securityAlertService.autoResolveOldAlerts();
            return ResponseEntity.ok(ApiResponse.success("Auto-resolve completed successfully"));

        } catch (Exception e) {
            logger.error("Error during auto-resolve", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to auto-resolve alerts: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    @Operation(
        summary = "Create security alert",
        description = "Manually create a security alert (for testing or manual reporting)"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<SecurityAlert>> createAlert(
            @Parameter(description = "Alert type", required = true)
            @RequestParam SecurityAlert.AlertType alertType,
            
            @Parameter(description = "Alert severity", required = true)
            @RequestParam SecurityAlert.Severity severity,
            
            @Parameter(description = "Alert title", required = true)
            @RequestParam String title,
            
            @Parameter(description = "Alert description", required = true)
            @RequestParam String description,
            
            @Parameter(description = "User ID (optional)")
            @RequestParam(required = false) UUID userId,
            
            @Parameter(description = "Tenant ID (optional)")
            @RequestParam(required = false) UUID tenantId,
            
            HttpServletRequest request) {

        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            SecurityAlert alert = securityAlertService.createAlert(
                alertType, severity, title, description, 
                userId, tenantId, clientIp, userAgent, null
            );
            
            return ResponseEntity.ok(ApiResponse.success(alert));

        } catch (Exception e) {
            logger.error("Error creating security alert", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to create security alert: " + e.getMessage()));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        // Extract user ID from JWT token in Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                // This would typically use JwtTokenProvider to extract user ID
                // For now, return a placeholder - in production this should extract from JWT
                return UUID.fromString("00000000-0000-0000-0000-000000000001");
            } catch (Exception e) {
                logger.warn("Failed to extract user ID from token", e);
            }
        }
        
        // Fallback - this should not happen in production
        throw new SecurityException("Unable to determine current user");
    }
}