package com.crm.platform.auth.controller;

import com.crm.platform.auth.service.SecurityConfigurationService;
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

@RestController
@RequestMapping("/api/v1/auth/security/config")
@Tag(name = "Security Configuration", description = "Security configuration management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SecurityConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigurationController.class);

    private final SecurityConfigurationService securityConfigService;

    @Autowired
    public SecurityConfigurationController(SecurityConfigurationService securityConfigService) {
        this.securityConfigService = securityConfigService;
    }

    @GetMapping
    @Operation(
        summary = "Get security configuration",
        description = "Retrieve all security configuration settings"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security configuration retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityConfiguration(HttpServletRequest request) {

        try {
            logger.debug("Retrieving security configuration");

            Map<String, Object> config = securityConfigService.getSecurityConfiguration();
            
            return ResponseEntity.ok(ApiResponse.success(config));

        } catch (Exception e) {
            logger.error("Error retrieving security configuration", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve security configuration: " + e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Update security configuration setting",
        description = "Update a specific security configuration setting"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid configuration value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateSecurityConfiguration(
            @Parameter(description = "Configuration key", required = true)
            @PathVariable String key,
            
            @Parameter(description = "Configuration value", required = true)
            @RequestBody Object value,
            
            HttpServletRequest request) {

        try {
            logger.debug("Updating security configuration: {} = {}", key, value);

            // Validate the configuration value
            if (!securityConfigService.validateConfiguration(key, value)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid configuration value for key: " + key));
            }

            securityConfigService.updateSecurityConfiguration(key, value);
            
            return ResponseEntity.ok(ApiResponse.success("Configuration updated successfully"));

        } catch (Exception e) {
            logger.error("Error updating security configuration", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update security configuration: " + e.getMessage()));
        }
    }

    @GetMapping("/password-policy")
    @Operation(
        summary = "Get password policy",
        description = "Retrieve current password policy settings"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password policy retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPasswordPolicy(HttpServletRequest request) {

        try {
            logger.debug("Retrieving password policy");

            Map<String, Object> policy = securityConfigService.getPasswordPolicy();
            
            return ResponseEntity.ok(ApiResponse.success(policy));

        } catch (Exception e) {
            logger.error("Error retrieving password policy", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve password policy: " + e.getMessage()));
        }
    }

    @GetMapping("/brute-force-protection")
    @Operation(
        summary = "Get brute force protection settings",
        description = "Retrieve current brute force protection configuration"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brute force protection settings retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBruteForceProtection(HttpServletRequest request) {

        try {
            logger.debug("Retrieving brute force protection settings");

            Map<String, Object> protection = securityConfigService.getBruteForceProtection();
            
            return ResponseEntity.ok(ApiResponse.success(protection));

        } catch (Exception e) {
            logger.error("Error retrieving brute force protection settings", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve brute force protection settings: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-defaults")
    @Operation(
        summary = "Reset configuration to defaults",
        description = "Reset all security configuration settings to their default values"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration reset successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> resetToDefaults(HttpServletRequest request) {

        try {
            logger.debug("Resetting security configuration to defaults");

            securityConfigService.resetToDefaults();
            
            return ResponseEntity.ok(ApiResponse.success("Security configuration reset to defaults"));

        } catch (Exception e) {
            logger.error("Error resetting security configuration", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to reset security configuration: " + e.getMessage()));
        }
    }
}