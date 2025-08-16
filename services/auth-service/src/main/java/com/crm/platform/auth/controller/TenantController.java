package com.crm.platform.auth.controller;

import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.service.TenantAuditService;
import com.crm.platform.auth.service.TenantContextService;
import com.crm.platform.auth.service.TenantValidationService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/tenant")
@Tag(name = "Tenant Management", description = "Tenant isolation, validation, and audit endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    private static final Logger logger = LoggerFactory.getLogger(TenantController.class);

    private final TenantContextService tenantContextService;
    private final TenantValidationService tenantValidationService;
    private final TenantAuditService tenantAuditService;

    @Autowired
    public TenantController(TenantContextService tenantContextService,
                          TenantValidationService tenantValidationService,
                          TenantAuditService tenantAuditService) {
        this.tenantContextService = tenantContextService;
        this.tenantValidationService = tenantValidationService;
        this.tenantAuditService = tenantAuditService;
    }

    @GetMapping("/context")
    @Operation(
        summary = "Get current tenant context",
        description = "Retrieve information about the current tenant context"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant context retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No tenant context set")
    })
    public ResponseEntity<ApiResponse<TenantContextService.TenantContextInfo>> getTenantContext(HttpServletRequest request) {
        try {
            if (!tenantContextService.hasTenantContext()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No tenant context set"));
            }

            TenantContextService.TenantContextInfo contextInfo = tenantContextService.getTenantContextInfo();
            return ResponseEntity.ok(ApiResponse.success(contextInfo));

        } catch (Exception e) {
            logger.error("Error retrieving tenant context", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant context: " + e.getMessage()));
        }
    }

    @PostMapping("/validate/{tenantId}")
    @Operation(
        summary = "Validate tenant access",
        description = "Validate that the specified tenant is accessible and active"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant validation completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Tenant validation failed")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<TenantValidationService.TenantValidationResult>> validateTenant(
            @Parameter(description = "Tenant ID to validate", required = true)
            @PathVariable UUID tenantId,
            
            HttpServletRequest request) {

        try {
            TenantValidationService.TenantValidationResult result = tenantValidationService.validateTenant(tenantId);
            
            if (result.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(result.getMessage()));
            }

        } catch (Exception e) {
            logger.error("Error validating tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to validate tenant: " + e.getMessage()));
        }
    }

    @PostMapping("/validate-subdomain/{subdomain}")
    @Operation(
        summary = "Validate tenant by subdomain",
        description = "Validate that the specified tenant subdomain is accessible and active"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant validation completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Tenant validation failed")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<TenantValidationService.TenantValidationResult>> validateTenantBySubdomain(
            @Parameter(description = "Tenant subdomain to validate", required = true)
            @PathVariable String subdomain,
            
            HttpServletRequest request) {

        try {
            TenantValidationService.TenantValidationResult result = tenantValidationService.validateTenantBySubdomain(subdomain);
            
            if (result.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(result.getMessage()));
            }

        } catch (Exception e) {
            logger.error("Error validating tenant by subdomain: {}", subdomain, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to validate tenant by subdomain: " + e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    @Operation(
        summary = "Get tenant audit logs",
        description = "Retrieve audit logs for the current tenant with optional filtering"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER') or hasRole('AUDITOR')")
    public ResponseEntity<ApiResponse<Page<SecurityAuditLog>>> getTenantAuditLogs(
            @Parameter(description = "Event type filter (optional)")
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
            Pageable pageable = PageRequest.of(page, size);
            Page<SecurityAuditLog> auditLogs = tenantAuditService.getTenantAuditLogs(eventType, startTime, endTime, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(auditLogs));

        } catch (Exception e) {
            logger.error("Error retrieving tenant audit logs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant audit logs: " + e.getMessage()));
        }
    }

    @GetMapping("/audit-logs/user/{userId}")
    @Operation(
        summary = "Get user audit logs within tenant",
        description = "Retrieve audit logs for a specific user within the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User audit logs retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER') or hasRole('AUDITOR')")
    public ResponseEntity<ApiResponse<Page<SecurityAuditLog>>> getUserAuditLogs(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            HttpServletRequest request) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SecurityAuditLog> auditLogs = tenantAuditService.getUserAuditLogs(userId, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(auditLogs));

        } catch (Exception e) {
            logger.error("Error retrieving user audit logs: {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve user audit logs: " + e.getMessage()));
        }
    }

    @GetMapping("/audit-statistics")
    @Operation(
        summary = "Get tenant audit statistics",
        description = "Retrieve audit statistics for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER') or hasRole('AUDITOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantAuditStatistics(
            @Parameter(description = "Start time for statistics period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time for statistics period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            HttpServletRequest request) {

        try {
            Map<String, Object> statistics = tenantAuditService.getTenantAuditStatistics(startTime, endTime);
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            logger.error("Error retrieving tenant audit statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant audit statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/compliance-summary")
    @Operation(
        summary = "Get tenant compliance summary",
        description = "Retrieve compliance summary for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Compliance summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantComplianceSummary(
            @Parameter(description = "Start time for compliance period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time for compliance period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            HttpServletRequest request) {

        try {
            Map<String, Object> summary = tenantAuditService.getTenantComplianceSummary(startTime, endTime);
            return ResponseEntity.ok(ApiResponse.success(summary));

        } catch (Exception e) {
            logger.error("Error retrieving tenant compliance summary", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve tenant compliance summary: " + e.getMessage()));
        }
    }

    @PostMapping("/audit-logs/export")
    @Operation(
        summary = "Export tenant audit logs",
        description = "Export audit logs for the current tenant for compliance purposes"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs exported successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<SecurityAuditLog>>> exportTenantAuditLogs(
            @Parameter(description = "Start time for export period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "End time for export period", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            
            @Parameter(description = "Event types to include (optional)")
            @RequestParam(required = false) List<String> eventTypes,
            
            HttpServletRequest request) {

        try {
            List<SecurityAuditLog> auditLogs = tenantAuditService.exportTenantAuditLogs(startTime, endTime, eventTypes);
            return ResponseEntity.ok(ApiResponse.success(auditLogs));

        } catch (Exception e) {
            logger.error("Error exporting tenant audit logs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to export tenant audit logs: " + e.getMessage()));
        }
    }

    @PostMapping("/cache/clear")
    @Operation(
        summary = "Clear tenant validation cache",
        description = "Clear the validation cache for the current tenant"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<String>> clearTenantCache(HttpServletRequest request) {
        try {
            UUID tenantId = tenantContextService.requireTenantContext();
            String subdomain = tenantContextService.getCurrentTenantSubdomain();
            
            tenantValidationService.clearTenantCache(tenantId);
            if (subdomain != null) {
                tenantValidationService.clearSubdomainCache(subdomain);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Tenant validation cache cleared"));

        } catch (Exception e) {
            logger.error("Error clearing tenant cache", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to clear tenant cache: " + e.getMessage()));
        }
    }

    @PostMapping("/isolation/test")
    @Operation(
        summary = "Test tenant isolation",
        description = "Test tenant isolation enforcement (for debugging)"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant isolation test completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Tenant isolation violation detected")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> testTenantIsolation(
            @Parameter(description = "Tenant ID to test access to")
            @RequestParam(required = false) UUID testTenantId,
            
            HttpServletRequest request) {

        try {
            UUID currentTenantId = tenantContextService.requireTenantContext();
            
            if (testTenantId == null) {
                testTenantId = currentTenantId;
            }
            
            // This should succeed for same tenant, fail for different tenant
            tenantValidationService.enforceTenantIsolation(testTenantId);
            
            String message = testTenantId.equals(currentTenantId) ? 
                "Tenant isolation test passed - same tenant access allowed" :
                "Tenant isolation test failed - cross-tenant access should have been blocked";
            
            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (TenantValidationService.TenantIsolationException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Tenant isolation violation detected: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error testing tenant isolation", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to test tenant isolation: " + e.getMessage()));
        }
    }
}