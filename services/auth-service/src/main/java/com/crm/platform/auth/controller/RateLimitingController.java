package com.crm.platform.auth.controller;

import com.crm.platform.auth.service.CaptchaService;
import com.crm.platform.auth.service.RateLimitingService;
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/rate-limiting")
@Tag(name = "Rate Limiting", description = "Rate limiting, IP blocking, and CAPTCHA management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class RateLimitingController {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingController.class);

    private final RateLimitingService rateLimitingService;
    private final CaptchaService captchaService;

    @Autowired
    public RateLimitingController(RateLimitingService rateLimitingService, CaptchaService captchaService) {
        this.rateLimitingService = rateLimitingService;
        this.captchaService = captchaService;
    }

    // IP Blocking Management

    @GetMapping("/blocked-ips")
    @Operation(
        summary = "Get blocked IP addresses",
        description = "Retrieve list of all blocked IP addresses"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Blocked IPs retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Set<String>>> getBlockedIPs(HttpServletRequest request) {
        try {
            Set<String> blockedIPs = rateLimitingService.getBlockedIPs();
            return ResponseEntity.ok(ApiResponse.success(blockedIPs));
        } catch (Exception e) {
            logger.error("Error retrieving blocked IPs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve blocked IPs: " + e.getMessage()));
        }
    }

    @GetMapping("/whitelisted-ips")
    @Operation(
        summary = "Get whitelisted IP addresses",
        description = "Retrieve list of all whitelisted IP addresses"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Whitelisted IPs retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Set<String>>> getWhitelistedIPs(HttpServletRequest request) {
        try {
            Set<String> whitelistedIPs = rateLimitingService.getWhitelistedIPs();
            return ResponseEntity.ok(ApiResponse.success(whitelistedIPs));
        } catch (Exception e) {
            logger.error("Error retrieving whitelisted IPs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve whitelisted IPs: " + e.getMessage()));
        }
    }

    @PostMapping("/block-ip")
    @Operation(
        summary = "Block IP address",
        description = "Block an IP address permanently or temporarily"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP blocked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> blockIP(
            @Parameter(description = "IP address to block", required = true)
            @RequestParam String ipAddress,
            
            @Parameter(description = "Reason for blocking", required = true)
            @RequestParam String reason,
            
            @Parameter(description = "Block permanently (default: false)")
            @RequestParam(defaultValue = "false") boolean permanent,
            
            HttpServletRequest request) {

        try {
            if (permanent) {
                rateLimitingService.blockIPPermanently(ipAddress, reason);
            } else {
                rateLimitingService.blockIPTemporarily(ipAddress, reason);
            }
            
            String message = String.format("IP %s blocked %s. Reason: %s", 
                                          ipAddress, permanent ? "permanently" : "temporarily", reason);
            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (Exception e) {
            logger.error("Error blocking IP: {}", ipAddress, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to block IP: " + e.getMessage()));
        }
    }

    @PostMapping("/unblock-ip")
    @Operation(
        summary = "Unblock IP address",
        description = "Remove IP address from block list"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP unblocked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> unblockIP(
            @Parameter(description = "IP address to unblock", required = true)
            @RequestParam String ipAddress,
            
            HttpServletRequest request) {

        try {
            rateLimitingService.unblockIP(ipAddress);
            return ResponseEntity.ok(ApiResponse.success("IP " + ipAddress + " unblocked successfully"));

        } catch (Exception e) {
            logger.error("Error unblocking IP: {}", ipAddress, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to unblock IP: " + e.getMessage()));
        }
    }

    @PostMapping("/whitelist-ip")
    @Operation(
        summary = "Add IP to whitelist",
        description = "Add IP address to whitelist (bypasses rate limiting)"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP whitelisted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> whitelistIP(
            @Parameter(description = "IP address to whitelist", required = true)
            @RequestParam String ipAddress,
            
            HttpServletRequest request) {

        try {
            rateLimitingService.addToWhitelist(ipAddress);
            return ResponseEntity.ok(ApiResponse.success("IP " + ipAddress + " added to whitelist"));

        } catch (Exception e) {
            logger.error("Error whitelisting IP: {}", ipAddress, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to whitelist IP: " + e.getMessage()));
        }
    }

    @DeleteMapping("/whitelist-ip")
    @Operation(
        summary = "Remove IP from whitelist",
        description = "Remove IP address from whitelist"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP removed from whitelist successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> removeFromWhitelist(
            @Parameter(description = "IP address to remove from whitelist", required = true)
            @RequestParam String ipAddress,
            
            HttpServletRequest request) {

        try {
            rateLimitingService.removeFromWhitelist(ipAddress);
            return ResponseEntity.ok(ApiResponse.success("IP " + ipAddress + " removed from whitelist"));

        } catch (Exception e) {
            logger.error("Error removing IP from whitelist: {}", ipAddress, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to remove IP from whitelist: " + e.getMessage()));
        }
    }

    @GetMapping("/ip-status/{ipAddress}")
    @Operation(
        summary = "Get IP blocking status",
        description = "Get detailed blocking status for an IP address"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<RateLimitingService.IPBlockingStatus>> getIPStatus(
            @Parameter(description = "IP address to check", required = true)
            @PathVariable String ipAddress,
            
            HttpServletRequest request) {

        try {
            RateLimitingService.IPBlockingStatus status = rateLimitingService.getIPBlockingStatus(ipAddress);
            return ResponseEntity.ok(ApiResponse.success(status));

        } catch (Exception e) {
            logger.error("Error getting IP status: {}", ipAddress, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get IP status: " + e.getMessage()));
        }
    }

    // Rate Limiting Information

    @GetMapping("/rate-limit-info")
    @Operation(
        summary = "Get rate limit information",
        description = "Get rate limiting information for a specific identifier and operation"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rate limit info retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<RateLimitingService.RateLimitInfo>> getRateLimitInfo(
            @Parameter(description = "Identifier (IP, user ID, etc.)", required = true)
            @RequestParam String identifier,
            
            @Parameter(description = "Operation type", required = true)
            @RequestParam String operation,
            
            HttpServletRequest request) {

        try {
            RateLimitingService.RateLimitInfo info = rateLimitingService.getRateLimitInfo(identifier, operation);
            return ResponseEntity.ok(ApiResponse.success(info));

        } catch (Exception e) {
            logger.error("Error getting rate limit info for identifier: {} operation: {}", identifier, operation, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get rate limit info: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get rate limiting statistics",
        description = "Get comprehensive rate limiting statistics"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitingStatistics(HttpServletRequest request) {
        try {
            Map<String, Object> statistics = rateLimitingService.getRateLimitingStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            logger.error("Error getting rate limiting statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }

    // CAPTCHA Management

    @PostMapping("/captcha/generate")
    @Operation(
        summary = "Generate CAPTCHA challenge",
        description = "Generate a new CAPTCHA challenge for suspicious activity protection"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CAPTCHA generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<CaptchaService.SimpleCaptchaChallenge>> generateCaptcha(
            HttpServletRequest request) {

        try {
            String sessionId = UUID.randomUUID().toString();
            CaptchaService.SimpleCaptchaChallenge challenge = captchaService.generateSimpleCaptcha(sessionId);
            
            if (challenge.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(challenge));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("Failed to generate CAPTCHA"));
            }

        } catch (Exception e) {
            logger.error("Error generating CAPTCHA", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate CAPTCHA: " + e.getMessage()));
        }
    }

    @PostMapping("/captcha/verify")
    @Operation(
        summary = "Verify CAPTCHA response",
        description = "Verify user's CAPTCHA response"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CAPTCHA verification completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCaptcha(
            @Parameter(description = "CAPTCHA session ID", required = true)
            @RequestParam String sessionId,
            
            @Parameter(description = "User's CAPTCHA response", required = true)
            @RequestParam String response,
            
            HttpServletRequest request) {

        try {
            String clientIp = getClientIpAddress(request);
            boolean isValid = captchaService.verifySimpleCaptcha(sessionId, response, clientIp);
            
            Map<String, Object> result = Map.of(
                "valid", isValid,
                "message", isValid ? "CAPTCHA verification successful" : "CAPTCHA verification failed"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("Error verifying CAPTCHA", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to verify CAPTCHA: " + e.getMessage()));
        }
    }

    @GetMapping("/captcha/config")
    @Operation(
        summary = "Get CAPTCHA configuration",
        description = "Get current CAPTCHA configuration settings"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CAPTCHA config retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCaptchaConfig(HttpServletRequest request) {
        try {
            Map<String, Object> config = captchaService.getCaptchaConfiguration();
            return ResponseEntity.ok(ApiResponse.success(config));

        } catch (Exception e) {
            logger.error("Error getting CAPTCHA configuration", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get CAPTCHA configuration: " + e.getMessage()));
        }
    }

    // Utility Methods

    @PostMapping("/cleanup")
    @Operation(
        summary = "Cleanup expired entries",
        description = "Manually trigger cleanup of expired rate limiting entries"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredEntries(HttpServletRequest request) {
        try {
            rateLimitingService.cleanupExpiredEntries();
            captchaService.cleanupExpiredCaptchas();
            
            return ResponseEntity.ok(ApiResponse.success("Cleanup completed successfully"));

        } catch (Exception e) {
            logger.error("Error during cleanup", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to cleanup: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-rate-limit")
    @Operation(
        summary = "Reset rate limit for identifier",
        description = "Reset rate limiting counters for a specific identifier and operation"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rate limit reset successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<String>> resetRateLimit(
            @Parameter(description = "Identifier to reset", required = true)
            @RequestParam String identifier,
            
            @Parameter(description = "Operation type", required = true)
            @RequestParam String operation,
            
            HttpServletRequest request) {

        try {
            rateLimitingService.resetRateLimit(identifier, operation);
            return ResponseEntity.ok(ApiResponse.success("Rate limit reset for " + identifier + ":" + operation));

        } catch (Exception e) {
            logger.error("Error resetting rate limit for identifier: {} operation: {}", identifier, operation, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to reset rate limit: " + e.getMessage()));
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
}