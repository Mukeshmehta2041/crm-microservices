package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.TokenIntrospectionResponse;
import com.crm.platform.auth.service.TokenManagementService;
import com.crm.platform.auth.service.RateLimitingService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import com.crm.platform.common.monitoring.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Token management controller providing token introspection, revocation, and monitoring endpoints
 */
@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "Token Management", description = "Token introspection, revocation, and management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TokenController {

    @Autowired
    private TokenManagementService tokenManagementService;

    @Autowired
    private RateLimitingService rateLimitingService;

    // ==================== Token Introspection ====================

    @PostMapping("/introspect")
    @Operation(summary = "Token Introspection", description = "Introspect token to get detailed information")
    @Timed(value = "auth.token.introspect", description = "Token introspection operation")
    @Monitored("token-introspect")
    @SecurityLog(operation = "token-introspect", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Token introspection completed"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request"),
        @SwaggerApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<TokenIntrospectionResponse>> introspectToken(
            @Parameter(description = "Token to introspect", required = true)
            @RequestParam("token") @NotBlank String token,
            HttpServletRequest request) {

        // Rate limiting
        String clientIp = getClientIpAddress(request);
        if (!rateLimitingService.isTokenIntrospectionAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many introspection requests"));
        }

        TokenIntrospectionResponse response = tokenManagementService.introspectToken(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Token Revocation ====================

    @PostMapping("/revoke")
    @Operation(summary = "Revoke Token", description = "Revoke a specific token")
    @Monitored("token-revoke")
    @SecurityLog(operation = "token-revoke", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Token revoked successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request"),
        @SwaggerApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> revokeToken(
            @Parameter(description = "Token to revoke", required = true)
            @RequestParam("token") @NotBlank String token,
            @Parameter(description = "Reason for revocation")
            @RequestParam(value = "reason", defaultValue = "user_requested") String reason,
            HttpServletRequest request) {

        // Rate limiting
        String clientIp = getClientIpAddress(request);
        if (!rateLimitingService.isRevokeTokenAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many revocation requests"));
        }

        // For now, we'll use null as revokedBy - in a real implementation, this would be extracted from auth context
        tokenManagementService.revokeToken(token, reason, null);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "revoked", true,
            "reason", reason,
            "timestamp", System.currentTimeMillis()
        )));
    }

    @PostMapping("/revoke-all")
    @Operation(summary = "Revoke All User Tokens", description = "Revoke all tokens for a specific user")
    @SecurityLog(operation = "revoke-all-tokens", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "All tokens revoked successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request"),
        @SwaggerApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> revokeAllUserTokens(
            @Parameter(description = "User ID", required = true)
            @RequestParam("user_id") UUID userId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId,
            @Parameter(description = "Reason for revocation")
            @RequestParam(value = "reason", defaultValue = "admin_requested") String reason,
            HttpServletRequest request) {

        // This endpoint would typically require admin privileges
        // For now, we'll proceed without authorization check

        tokenManagementService.revokeAllUserTokens(userId, tenantId, reason, null);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "revoked_all", true,
            "user_id", userId,
            "tenant_id", tenantId,
            "reason", reason,
            "timestamp", System.currentTimeMillis()
        )));
    }

    // ==================== Token Rotation ====================

    @PostMapping("/rotate")
    @Operation(summary = "Rotate Refresh Token", description = "Generate new access and refresh tokens")
    @Timed(value = "auth.token.rotate", description = "Token rotation operation")
    @Monitored("token-rotate")
    @SecurityLog(operation = "token-rotate", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Tokens rotated successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid refresh token"),
        @SwaggerApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> rotateToken(
            @Parameter(description = "Current refresh token", required = true)
            @RequestParam("refresh_token") @NotBlank String refreshToken,
            HttpServletRequest request) {

        // Rate limiting
        String clientIp = getClientIpAddress(request);
        if (!rateLimitingService.isRefreshTokenAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many refresh requests"));
        }

        Map<String, String> newTokens = tokenManagementService.rotateRefreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(newTokens));
    }

    // ==================== Token Validation ====================

    @PostMapping("/validate")
    @Operation(summary = "Validate Token", description = "Validate token and check if it's active")
    @Timed(value = "auth.token.validate", description = "Token validation operation")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Token validation completed"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @Parameter(description = "Token to validate", required = true)
            @RequestParam("token") @NotBlank String token) {

        boolean isValid = tokenManagementService.validateToken(token);
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", isValid,
            "timestamp", System.currentTimeMillis()
        )));
    }

    // ==================== Token Statistics ====================

    @GetMapping("/stats")
    @Operation(summary = "Token Statistics", description = "Get token usage statistics")
    @SecurityLog(operation = "token-stats", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @SwaggerApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenStatistics(
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        Map<String, Object> stats = tokenManagementService.getTokenStatistics(tenantId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== Rate Limit Information ====================

    @GetMapping("/rate-limit")
    @Operation(summary = "Rate Limit Information", description = "Get current rate limit status")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Rate limit information retrieved")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitInfo(
            @Parameter(description = "Operation type")
            @RequestParam(value = "operation", defaultValue = "oauth2_token") String operation,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);
        RateLimitingService.RateLimitInfo rateLimitInfo = rateLimitingService.getRateLimitInfo(clientIp, operation);

        Map<String, Object> response = Map.of(
            "remaining_tokens", rateLimitInfo.getRemainingTokens(),
            "max_tokens", rateLimitInfo.getMaxTokens(),
            "limit_exceeded", rateLimitInfo.isLimitExceeded(),
            "timestamp", rateLimitInfo.getTimestamp()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Cleanup Operations ====================

    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup Expired Tokens", description = "Manually trigger cleanup of expired tokens")
    @SecurityLog(operation = "token-cleanup", type = SecurityLog.SecurityType.MAINTENANCE, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
        @SwaggerApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupExpiredTokens() {
        
        // This endpoint would typically require admin privileges
        tokenManagementService.cleanupExpiredBlacklistedTokens();

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "cleanup_completed", true,
            "timestamp", System.currentTimeMillis()
        )));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Token Service Health", description = "Check token management service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "token-management",
            "timestamp", System.currentTimeMillis()
        )));
    }

    // Helper methods

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