package com.crm.platform.users.controller;

import com.crm.platform.users.dto.*;
import com.crm.platform.users.entity.User;
import com.crm.platform.users.service.UserService;
import com.crm.platform.users.service.UserAuditService;
import com.crm.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive User Management controller with full CRUD operations,
 * advanced search, filtering, bulk operations, and analytics
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Comprehensive user management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;
    private final UserAuditService userAuditService;

    @Autowired
    public UserController(UserService userService, UserAuditService userAuditService) {
        this.userService = userService;
        this.userAuditService = userAuditService;
    }

    @PostMapping
    @Operation(summary = "Create User", description = "Create a new user account")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByTenant(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/tenant/{tenantId}/paginated")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsersByTenantPaginated(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        Page<UserResponse> users = userService.getUsersByTenant(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            Pageable pageable) {
        Page<UserResponse> users = userService.searchUsers(tenantId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(ApiResponse.success(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/activity")
    public ResponseEntity<ApiResponse<Void>> updateLastActivity(@PathVariable UUID id) {
        try {
            userService.updateLastActivity(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam User.UserStatus status) {
        try {
            userService.updateUserStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<ApiResponse<Long>> countActiveUsers(@PathVariable UUID tenantId) {
        long count = userService.countActiveUsersByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/tenant/{tenantId}/onboarding-incomplete")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersWithIncompleteOnboarding(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersWithIncompleteOnboarding(tenantId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // ==================== Enhanced Search and Filtering ====================

    @PostMapping("/tenant/{tenantId}/search")
    @Operation(summary = "Advanced User Search", description = "Search users with advanced filtering options")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsersWithFilters(
            @PathVariable UUID tenantId,
            @RequestBody UserFilterRequest filterRequest,
            Pageable pageable) {
        
        Page<UserResponse> users;
        if (filterRequest.getSearchQuery() != null && !filterRequest.getSearchQuery().trim().isEmpty()) {
            users = userService.searchUsersAdvanced(tenantId, filterRequest.getSearchQuery(), pageable);
        } else {
            users = userService.findUsersWithFilters(
                tenantId, 
                filterRequest.getStatus(),
                filterRequest.getDepartment(),
                filterRequest.getJobTitle(),
                filterRequest.getManagerId(),
                filterRequest.getTeamId(),
                filterRequest.getOfficeLocation(),
                filterRequest.getOnboardingCompleted(),
                pageable
            );
        }
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // ==================== User Activation and Deactivation ====================

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate User", description = "Activate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest request) {
        UserResponse user = userService.activateUser(id, performedBy, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate User", description = "Deactivate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest request) {
        UserResponse user = userService.deactivateUser(id, performedBy, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend User", description = "Suspend a user account")
    public ResponseEntity<ApiResponse<UserResponse>> suspendUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest request) {
        UserResponse user = userService.suspendUser(id, performedBy, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    // ==================== User Statistics and Analytics ====================

    @GetMapping("/tenant/{tenantId}/statistics")
    @Operation(summary = "Get User Statistics", description = "Get comprehensive user statistics and analytics")
    public ResponseEntity<ApiResponse<UserStatisticsResponse>> getUserStatistics(@PathVariable UUID tenantId) {
        UserStatisticsResponse statistics = userService.getUserStatistics(tenantId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    // ==================== Bulk Operations ====================

    @PostMapping("/bulk-operations")
    @Operation(summary = "Bulk User Operations", description = "Perform bulk operations on multiple users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUserOperations(
            @Valid @RequestBody BulkUserOperationRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest httpRequest) {
        
        switch (request.getOperation()) {
            case UPDATE_STATUS:
                userService.bulkUpdateStatus(request.getUserIds(), request.getStatus(), performedBy, httpRequest);
                break;
            case UPDATE_DEPARTMENT:
                userService.bulkUpdateDepartment(request.getUserIds(), request.getDepartment(), performedBy, httpRequest);
                break;
            case UPDATE_MANAGER:
                userService.bulkUpdateManager(request.getUserIds(), request.getManagerId(), performedBy, httpRequest);
                break;
            case UPDATE_TEAM:
                userService.bulkUpdateTeam(request.getUserIds(), request.getTeamId(), performedBy, httpRequest);
                break;
            case ACTIVATE:
                userService.bulkUpdateStatus(request.getUserIds(), User.UserStatus.ACTIVE, performedBy, httpRequest);
                break;
            case DEACTIVATE:
                userService.bulkUpdateStatus(request.getUserIds(), User.UserStatus.INACTIVE, performedBy, httpRequest);
                break;
            case SUSPEND:
                userService.bulkUpdateStatus(request.getUserIds(), User.UserStatus.SUSPENDED, performedBy, httpRequest);
                break;
            default:
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid bulk operation"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "operation", request.getOperation(),
            "affectedUsers", request.getUserIds().size(),
            "completedAt", java.time.LocalDateTime.now()
        )));
    }

    // ==================== User Hierarchy and Relationships ====================

    @GetMapping("/{id}/direct-reports")
    @Operation(summary = "Get Direct Reports", description = "Get users who report directly to this manager")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getDirectReports(@PathVariable UUID id) {
        List<UserResponse> directReports = userService.getDirectReports(id);
        return ResponseEntity.ok(ApiResponse.success(directReports));
    }

    @GetMapping("/team/{teamId}/members")
    @Operation(summary = "Get Team Members", description = "Get all members of a specific team")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getTeamMembers(@PathVariable UUID teamId) {
        List<UserResponse> teamMembers = userService.getTeamMembers(teamId);
        return ResponseEntity.ok(ApiResponse.success(teamMembers));
    }

    @GetMapping("/{id}/direct-reports/count")
    @Operation(summary = "Count Direct Reports", description = "Get count of direct reports for a manager")
    public ResponseEntity<ApiResponse<Long>> countDirectReports(@PathVariable UUID id) {
        long count = userService.countDirectReports(id);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/team/{teamId}/members/count")
    @Operation(summary = "Count Team Members", description = "Get count of team members")
    public ResponseEntity<ApiResponse<Long>> countTeamMembers(@PathVariable UUID teamId) {
        long count = userService.countTeamMembers(teamId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== Skills and Certifications ====================

    @GetMapping("/tenant/{tenantId}/skills")
    @Operation(summary = "Get All Skills", description = "Get all skills across users in tenant")
    public ResponseEntity<ApiResponse<List<String>>> getAllSkills(@PathVariable UUID tenantId) {
        List<String> skills = userService.getAllSkills(tenantId);
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/tenant/{tenantId}/certifications")
    @Operation(summary = "Get All Certifications", description = "Get all certifications across users in tenant")
    public ResponseEntity<ApiResponse<List<String>>> getAllCertifications(@PathVariable UUID tenantId) {
        List<String> certifications = userService.getAllCertifications(tenantId);
        return ResponseEntity.ok(ApiResponse.success(certifications));
    }

    @GetMapping("/tenant/{tenantId}/by-skill")
    @Operation(summary = "Find Users by Skill", description = "Find users with specific skill")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findUsersBySkill(
            @PathVariable UUID tenantId,
            @RequestParam String skill) {
        List<UserResponse> users = userService.findUsersBySkill(tenantId, skill);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/tenant/{tenantId}/by-certification")
    @Operation(summary = "Find Users by Certification", description = "Find users with specific certification")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findUsersByCertification(
            @PathVariable UUID tenantId,
            @RequestParam String certification) {
        List<UserResponse> users = userService.findUsersByCertification(tenantId, certification);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // ==================== Custom Fields Management ====================

    @PutMapping("/{id}/custom-fields")
    @Operation(summary = "Update Custom Fields", description = "Update user custom fields")
    public ResponseEntity<ApiResponse<UserResponse>> updateCustomFields(
            @PathVariable UUID id,
            @Valid @RequestBody CustomFieldsRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest httpRequest) {
        UserResponse user = userService.updateCustomFields(id, request.getCustomFields(), performedBy, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/{id}/custom-fields")
    @Operation(summary = "Get Custom Fields", description = "Get user custom fields")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomFields(@PathVariable UUID id) {
        Map<String, Object> customFields = userService.getCustomFields(id);
        return ResponseEntity.ok(ApiResponse.success(customFields));
    }

    // ==================== GDPR and Compliance ====================

    @PostMapping("/{id}/consent")
    @Operation(summary = "Update User Consent", description = "Update GDPR or marketing consent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateConsent(
            @PathVariable UUID id,
            @Valid @RequestBody ConsentUpdateRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest httpRequest) {
        
        if (request.getConsentType() == ConsentUpdateRequest.ConsentType.GDPR) {
            userService.updateGdprConsent(id, request.getConsentGiven(), performedBy, httpRequest);
        } else {
            userService.updateMarketingConsent(id, request.getConsentGiven(), performedBy, httpRequest);
        }
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "consentType", request.getConsentType(),
            "consentGiven", request.getConsentGiven(),
            "updatedAt", java.time.LocalDateTime.now()
        )));
    }

    @GetMapping("/tenant/{tenantId}/without-gdpr-consent")
    @Operation(summary = "Get Users Without GDPR Consent", description = "Get users who haven't given GDPR consent")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersWithoutGdprConsent(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersWithoutGdprConsent(tenantId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{id}/data-export")
    @Operation(summary = "Request Data Export", description = "Request user data export for GDPR compliance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestDataExport(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest request) {
        userService.requestDataExport(id, performedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "dataExportRequested", true,
            "requestedAt", java.time.LocalDateTime.now()
        )));
    }

    @PostMapping("/{id}/data-deletion")
    @Operation(summary = "Request Data Deletion", description = "Request user data deletion for GDPR compliance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestDataDeletion(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID performedBy,
            HttpServletRequest request) {
        userService.requestDataDeletion(id, performedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "dataDeletionRequested", true,
            "requestedAt", java.time.LocalDateTime.now(),
            "scheduledDeletionDate", java.time.LocalDateTime.now().plusDays(30)
        )));
    }

    // ==================== Audit Trail ====================

    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "Get User Audit Logs", description = "Get audit trail for a specific user")
    public ResponseEntity<ApiResponse<Page<com.crm.platform.users.entity.UserAuditLog>>> getUserAuditLogs(
            @PathVariable UUID id,
            Pageable pageable) {
        Page<com.crm.platform.users.entity.UserAuditLog> auditLogs = userAuditService.getUserAuditLogs(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping("/tenant/{tenantId}/audit-logs")
    @Operation(summary = "Get Tenant Audit Logs", description = "Get audit trail for all users in tenant")
    public ResponseEntity<ApiResponse<Page<com.crm.platform.users.entity.UserAuditLog>>> getTenantAuditLogs(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        Page<com.crm.platform.users.entity.UserAuditLog> auditLogs = userAuditService.getTenantAuditLogs(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "users-service",
            "timestamp", java.time.LocalDateTime.now()
        )));
    }
}