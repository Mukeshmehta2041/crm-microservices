package com.crm.platform.auth.controller;

import com.crm.platform.auth.service.TenantManagementService;
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

@RestController
@RequestMapping("/api/v1/auth/tenant/management")
@Tag(name = "Tenant Management", description = "Comprehensive tenant management features")
@SecurityRequirement(name = "bearerAuth")
public class TenantManagementController {

    private static final Logger logger = LoggerFactory.getLogger(TenantManagementController.class);

    private final TenantManagementService tenantManagementService;

    @Autowired
    public TenantManagementController(TenantManagementService tenantManagementService) {
        this.tenantManagementService = tenantManagementService;
    }

    // Configuration Management

    @GetMapping("/configuration")
    @Operation(
        summary = "Get tenant configuration",
        description = "Retrieve configuration settings for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantConfiguration(HttpServletRequest request) {
        try {
            Map<String, Object> configuration = tenantManagementService.getTenantConfiguration();
            return ResponseEntity.ok(ApiResponse.success(configuration));

        } catch (Exception e) {
            logger.error("Error retrieving tenant configuration", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant configuration: " + e.getMessage()));
        }
    }

    @PutMapping("/configuration")
    @Operation(
        summary = "Update tenant configuration",
        description = "Update configuration settings for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid configuration data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<String>> updateTenantConfiguration(
            @Parameter(description = "Configuration data", required = true)
            @RequestBody Map<String, Object> configuration,
            
            HttpServletRequest request) {

        try {
            tenantManagementService.updateTenantConfiguration(configuration);
            return ResponseEntity.ok(ApiResponse.success("Tenant configuration updated successfully"));

        } catch (Exception e) {
            logger.error("Error updating tenant configuration", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update tenant configuration: " + e.getMessage()));
        }
    }

    // Security Policies Management

    @GetMapping("/security-policies")
    @Operation(
        summary = "Get tenant security policies",
        description = "Retrieve security policies for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security policies retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<TenantManagementService.TenantSecurityPolicies>> getTenantSecurityPolicies(HttpServletRequest request) {
        try {
            TenantManagementService.TenantSecurityPolicies policies = tenantManagementService.getTenantSecurityPolicies();
            return ResponseEntity.ok(ApiResponse.success(policies));

        } catch (Exception e) {
            logger.error("Error retrieving tenant security policies", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant security policies: " + e.getMessage()));
        }
    }

    @PutMapping("/security-policies")
    @Operation(
        summary = "Update tenant security policies",
        description = "Update security policies for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security policies updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid policy data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<String>> updateTenantSecurityPolicies(
            @Parameter(description = "Security policies", required = true)
            @RequestBody TenantManagementService.TenantSecurityPolicies policies,
            
            HttpServletRequest request) {

        try {
            tenantManagementService.updateTenantSecurityPolicies(policies);
            return ResponseEntity.ok(ApiResponse.success("Tenant security policies updated successfully"));

        } catch (Exception e) {
            logger.error("Error updating tenant security policies", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update tenant security policies: " + e.getMessage()));
        }
    }

    // Usage Monitoring

    @GetMapping("/usage")
    @Operation(
        summary = "Get tenant usage statistics",
        description = "Retrieve usage statistics for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<TenantManagementService.TenantUsageStatistics>> getTenantUsageStatistics(HttpServletRequest request) {
        try {
            TenantManagementService.TenantUsageStatistics usage = tenantManagementService.getTenantUsageStatistics();
            return ResponseEntity.ok(ApiResponse.success(usage));

        } catch (Exception e) {
            logger.error("Error retrieving tenant usage statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant usage statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/limits/check")
    @Operation(
        summary = "Check tenant limits",
        description = "Check if the current tenant is within usage limits"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Limit check completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<TenantManagementService.TenantLimitCheckResult>> checkTenantLimits(HttpServletRequest request) {
        try {
            TenantManagementService.TenantLimitCheckResult result = tenantManagementService.checkTenantLimits();
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("Error checking tenant limits", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to check tenant limits: " + e.getMessage()));
        }
    }

    // Role and Permission Management

    @GetMapping("/roles")
    @Operation(
        summary = "Get tenant roles",
        description = "Retrieve roles specific to the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant roles retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTenantRoles(HttpServletRequest request) {
        try {
            List<Map<String, Object>> roles = tenantManagementService.getTenantRoles();
            return ResponseEntity.ok(ApiResponse.success(roles));

        } catch (Exception e) {
            logger.error("Error retrieving tenant roles", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant roles: " + e.getMessage()));
        }
    }

    @PostMapping("/roles")
    @Operation(
        summary = "Create tenant role",
        description = "Create a new role specific to the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant role created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid role data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTenantRole(
            @Parameter(description = "Role data", required = true)
            @RequestBody Map<String, Object> roleData,
            
            HttpServletRequest request) {

        try {
            Map<String, Object> createdRole = tenantManagementService.createTenantRole(roleData);
            return ResponseEntity.ok(ApiResponse.success(createdRole));

        } catch (Exception e) {
            logger.error("Error creating tenant role", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to create tenant role: " + e.getMessage()));
        }
    }

    // Data Export and Migration

    @PostMapping("/export")
    @Operation(
        summary = "Export tenant data",
        description = "Export tenant data for backup or migration purposes"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data export initiated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid export request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<TenantManagementService.TenantDataExport>> exportTenantData(
            @Parameter(description = "Export request", required = true)
            @RequestBody TenantManagementService.TenantDataExportRequest request,
            
            HttpServletRequest httpRequest) {

        try {
            TenantManagementService.TenantDataExport export = tenantManagementService.exportTenantData(request);
            return ResponseEntity.ok(ApiResponse.success(export));

        } catch (Exception e) {
            logger.error("Error exporting tenant data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to export tenant data: " + e.getMessage()));
        }
    }

    @GetMapping("/export/{exportId}")
    @Operation(
        summary = "Get data export status",
        description = "Retrieve the status and results of a data export"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Export status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Export not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<TenantManagementService.TenantDataExport>> getDataExportStatus(
            @Parameter(description = "Export ID", required = true)
            @PathVariable String exportId,
            
            HttpServletRequest request) {

        try {
            TenantManagementService.TenantDataExport export = tenantManagementService.getDataExportStatus(exportId);
            return ResponseEntity.ok(ApiResponse.success(export));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied to export: " + exportId));
            } else {
                logger.error("Error retrieving export status: {}", exportId, e);
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("Failed to retrieve export status: " + e.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Error retrieving export status: {}", exportId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve export status: " + e.getMessage()));
        }
    }

    // Utility Endpoints

    @GetMapping("/health")
    @Operation(
        summary = "Get tenant health status",
        description = "Get overall health status of the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Health status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantHealth(HttpServletRequest request) {
        try {
            // Combine various health indicators
            TenantManagementService.TenantUsageStatistics usage = tenantManagementService.getTenantUsageStatistics();
            TenantManagementService.TenantLimitCheckResult limits = tenantManagementService.checkTenantLimits();
            
            Map<String, Object> health = Map.of(
                "status", limits.isWithinLimits() ? "HEALTHY" : "WARNING",
                "usage", usage,
                "limits", limits,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(health));

        } catch (Exception e) {
            logger.error("Error retrieving tenant health", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant health: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get tenant summary",
        description = "Get a comprehensive summary of the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantSummary(HttpServletRequest request) {
        try {
            // Combine various tenant information
            Map<String, Object> configuration = tenantManagementService.getTenantConfiguration();
            TenantManagementService.TenantUsageStatistics usage = tenantManagementService.getTenantUsageStatistics();
            TenantManagementService.TenantSecurityPolicies policies = tenantManagementService.getTenantSecurityPolicies();
            List<Map<String, Object>> roles = tenantManagementService.getTenantRoles();
            
            Map<String, Object> summary = Map.of(
                "configuration", configuration,
                "usage", usage,
                "security_policies", policies,
                "roles_count", roles.size(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(summary));

        } catch (Exception e) {
            logger.error("Error retrieving tenant summary", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant summary: " + e.getMessage()));
        }
    }
}