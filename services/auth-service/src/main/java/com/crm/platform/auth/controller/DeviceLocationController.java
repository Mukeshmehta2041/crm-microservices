package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.DeviceInfo;
import com.crm.platform.auth.dto.LocationInfo;
import com.crm.platform.auth.dto.SuspiciousActivityAlert;
import com.crm.platform.auth.service.DeviceLocationService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Device and location tracking controller providing device detection,
 * location services, and suspicious activity monitoring endpoints.
 */
@RestController
@RequestMapping("/api/v1/device-location")
@Tag(name = "Device & Location Tracking", description = "Device detection and location tracking endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeviceLocationController {

    @Autowired
    private DeviceLocationService deviceLocationService;

    // ==================== Device Detection ====================

    @GetMapping("/device-info")
    @Operation(summary = "Get Device Information", description = "Get comprehensive device information from request")
    @Monitored("device-info")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device information retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<DeviceInfo>> getDeviceInfo(HttpServletRequest request) {
        DeviceInfo deviceInfo = deviceLocationService.createDeviceInfo(request);
        return ResponseEntity.ok(ApiResponse.success(deviceInfo));
    }

    @GetMapping("/detect-device-type")
    @Operation(summary = "Detect Device Type", description = "Detect device type from User-Agent")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device type detected successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> detectDeviceType(
            @Parameter(description = "User-Agent header")
            @RequestParam(value = "user_agent", required = false) String userAgent,
            HttpServletRequest request) {

        String userAgentToUse = userAgent != null ? userAgent : request.getHeader("User-Agent");
        String deviceType = deviceLocationService.detectDeviceType(userAgentToUse);
        
        DeviceInfo.BrowserInfo browserInfo = deviceLocationService.extractBrowserInfo(userAgentToUse);
        DeviceInfo.OSInfo osInfo = deviceLocationService.extractOSInfo(userAgentToUse);

        Map<String, String> result = Map.of(
            "device_type", deviceType,
            "browser_name", browserInfo.getName(),
            "browser_version", browserInfo.getVersion(),
            "os_name", osInfo.getName(),
            "os_version", osInfo.getVersion(),
            "user_agent", userAgentToUse != null ? userAgentToUse : "Unknown"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/generate-fingerprint")
    @Operation(summary = "Generate Device Fingerprint", description = "Generate advanced device fingerprint")
    @Monitored("device-fingerprint")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device fingerprint generated successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> generateDeviceFingerprint(
            HttpServletRequest request) {

        String fingerprint = deviceLocationService.generateAdvancedDeviceFingerprint(request);
        
        Map<String, String> result = Map.of(
            "fingerprint", fingerprint,
            "timestamp", java.time.Instant.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Location Services ====================

    @GetMapping("/location-info")
    @Operation(summary = "Get Location Information", description = "Get location information from IP address")
    @Monitored("location-info")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Location information retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid IP address")
    })
    public ResponseEntity<ApiResponse<LocationInfo>> getLocationInfo(
            @Parameter(description = "IP address to lookup")
            @RequestParam(value = "ip", required = false) String ipAddress,
            HttpServletRequest request) {

        String ipToUse = ipAddress != null ? ipAddress : extractClientIpAddress(request);
        LocationInfo locationInfo = deviceLocationService.getLocationFromIP(ipToUse);

        return ResponseEntity.ok(ApiResponse.success(locationInfo));
    }

    @PostMapping("/calculate-distance")
    @Operation(summary = "Calculate Distance", description = "Calculate distance between two locations")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Distance calculated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid location data")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateDistance(
            @Parameter(description = "First location latitude") @RequestParam double lat1,
            @Parameter(description = "First location longitude") @RequestParam double lon1,
            @Parameter(description = "Second location latitude") @RequestParam double lat2,
            @Parameter(description = "Second location longitude") @RequestParam double lon2) {

        LocationInfo location1 = new LocationInfo("", "", "", "", lat1, lon1);
        LocationInfo location2 = new LocationInfo("", "", "", "", lat2, lon2);
        
        double distance = location1.distanceTo(location2);

        Map<String, Object> result = Map.of(
            "distance_km", distance,
            "distance_miles", distance * 0.621371,
            "location1", Map.of("latitude", lat1, "longitude", lon1),
            "location2", Map.of("latitude", lat2, "longitude", lon2)
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Suspicious Activity Detection ====================

    @PostMapping("/detect-suspicious-activity")
    @Operation(summary = "Detect Suspicious Activity", description = "Analyze device and location for suspicious activity")
    @SecurityLog(operation = "suspicious-activity-detection", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suspicious activity analysis completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<ApiResponse<List<SuspiciousActivityAlert>>> detectSuspiciousActivity(
            @Parameter(description = "User ID", required = true) @RequestParam UUID userId,
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            HttpServletRequest request) {

        DeviceInfo currentDevice = deviceLocationService.createDeviceInfo(request);
        List<SuspiciousActivityAlert> alerts = deviceLocationService.detectSuspiciousActivity(
            userId, tenantId, currentDevice);

        // Send notifications if alerts found
        if (!alerts.isEmpty()) {
            deviceLocationService.sendSecurityNotifications(userId, tenantId, alerts, currentDevice);
        }

        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    // ==================== Device Statistics ====================

    @GetMapping("/device-statistics")
    @Operation(summary = "Get Device Statistics", description = "Get device usage statistics for a user")
    @SecurityLog(operation = "device-statistics", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeviceStatistics(
            @Parameter(description = "User ID", required = true) @RequestParam UUID userId,
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId) {

        Map<String, Object> statistics = deviceLocationService.getDeviceStatistics(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    // ==================== Utility Endpoints ====================

    @GetMapping("/client-ip")
    @Operation(summary = "Get Client IP", description = "Get the client's IP address")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Client IP retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> getClientIP(HttpServletRequest request) {
        String clientIP = extractClientIpAddress(request);
        
        Map<String, String> result = Map.of(
            "ip_address", clientIP,
            "x_forwarded_for", request.getHeader("X-Forwarded-For") != null ? 
                request.getHeader("X-Forwarded-For") : "Not present",
            "x_real_ip", request.getHeader("X-Real-IP") != null ? 
                request.getHeader("X-Real-IP") : "Not present",
            "remote_addr", request.getRemoteAddr()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/request-headers")
    @Operation(summary = "Get Request Headers", description = "Get relevant request headers for debugging")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request headers retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = Map.of(
            "user_agent", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "Not present",
            "accept", request.getHeader("Accept") != null ? request.getHeader("Accept") : "Not present",
            "accept_language", request.getHeader("Accept-Language") != null ? request.getHeader("Accept-Language") : "Not present",
            "accept_encoding", request.getHeader("Accept-Encoding") != null ? request.getHeader("Accept-Encoding") : "Not present",
            "connection", request.getHeader("Connection") != null ? request.getHeader("Connection") : "Not present",
            "cache_control", request.getHeader("Cache-Control") != null ? request.getHeader("Cache-Control") : "Not present"
        );

        return ResponseEntity.ok(ApiResponse.success(headers));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Device Location Service Health", description = "Check device location service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "device-location-tracking",
            "timestamp", java.time.Instant.now()
        )));
    }

    // Helper methods
    private String extractClientIpAddress(HttpServletRequest request) {
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
}