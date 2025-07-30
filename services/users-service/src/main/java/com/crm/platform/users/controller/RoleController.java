package com.crm.platform.users.controller;

import com.crm.platform.users.dto.CreateRoleRequest;
import com.crm.platform.users.dto.RoleAssignmentRequest;
import com.crm.platform.users.entity.Permission;
import com.crm.platform.users.entity.Role;
import com.crm.platform.users.entity.UserRole;
import com.crm.platform.users.service.RoleService;
import com.crm.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for role management operations
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role Management", description = "Role and permission management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // ==================== Role CRUD Operations ====================

    @PostMapping
    @Operation(summary = "Create Role", description = "Create a new role")
    public ResponseEntity<ApiResponse<Role>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID createdBy,
            HttpServletRequest httpRequest) {
        
        Role role = roleService.createRole(
            request.getName(),
            request.getDescription(),
            request.getTenantId(),
            request.getRoleType(),
            request.getParentRoleId(),
            createdBy,
            httpRequest
        );
        
        // Set permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            roleService.setRolePermissions(role.getId(), request.getPermissionIds(), createdBy, httpRequest);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(role));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Role", description = "Get role by ID")
    public ResponseEntity<ApiResponse<Role>> getRoleById(@PathVariable UUID id) {
        return roleService.getRoleById(id)
                .map(role -> ResponseEntity.ok(ApiResponse.success(role)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get Tenant Roles", description = "Get all roles for a tenant")
    public ResponseEntity<ApiResponse<List<Role>>> getTenantRoles(@PathVariable UUID tenantId) {
        List<Role> roles = roleService.getAllRoles(tenantId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/tenant/{tenantId}/paginated")
    @Operation(summary = "Get Tenant Roles Paginated", description = "Get roles for tenant with pagination")
    public ResponseEntity<ApiResponse<Page<Role>>> getTenantRolesPaginated(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        Page<Role> roles = roleService.getRoles(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/tenant/{tenantId}/search")
    @Operation(summary = "Search Roles", description = "Search roles by name")
    public ResponseEntity<ApiResponse<Page<Role>>> searchRoles(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            Pageable pageable) {
        Page<Role> roles = roleService.searchRoles(tenantId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Role", description = "Update role details")
    public ResponseEntity<ApiResponse<Role>> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        Role role = roleService.updateRole(
            id,
            (String) updates.get("name"),
            (String) updates.get("description"),
            (String) updates.get("color"),
            (String) updates.get("icon"),
            (Integer) updates.get("priority"),
            updatedBy,
            request
        );
        
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Role", description = "Delete a role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteRole(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID deletedBy,
            HttpServletRequest request) {
        
        roleService.deleteRole(id, deletedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "deleted", true,
            "roleId", id,
            "deletedAt", LocalDateTime.now()
        )));
    }

    // ==================== Role Assignment Operations ====================

    @PostMapping("/assign")
    @Operation(summary = "Assign Role", description = "Assign role to user")
    public ResponseEntity<ApiResponse<UserRole>> assignRole(
            @Valid @RequestBody RoleAssignmentRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID assignedBy,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            HttpServletRequest httpRequest) {
        
        UserRole userRole = roleService.assignRoleToUser(
            request.getUserId(),
            request.getRoleId(),
            tenantId,
            request.getExpiresAt(),
            request.getAssignmentReason(),
            assignedBy,
            httpRequest
        );
        
        return ResponseEntity.ok(ApiResponse.success(userRole));
    }

    @DeleteMapping("/assign")
    @Operation(summary = "Remove Role", description = "Remove role from user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeRole(
            @RequestParam UUID userId,
            @RequestParam UUID roleId,
            @RequestHeader(value = "X-User-ID", required = false) UUID removedBy,
            HttpServletRequest request) {
        
        roleService.removeRoleFromUser(userId, roleId, removedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "removed", true,
            "userId", userId,
            "roleId", roleId,
            "removedAt", LocalDateTime.now()
        )));
    }

    @PutMapping("/assign/extend")
    @Operation(summary = "Extend Role", description = "Extend role expiration")
    public ResponseEntity<ApiResponse<UserRole>> extendRole(
            @RequestParam UUID userId,
            @RequestParam UUID roleId,
            @RequestParam LocalDateTime newExpiryDate,
            @RequestHeader(value = "X-User-ID", required = false) UUID extendedBy,
            HttpServletRequest request) {
        
        UserRole userRole = roleService.extendRoleExpiration(userId, roleId, newExpiryDate, extendedBy, request);
        return ResponseEntity.ok(ApiResponse.success(userRole));
    }

    // ==================== Permission Management ====================

    @PostMapping("/{id}/permissions")
    @Operation(summary = "Add Permission to Role", description = "Add permission to role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addPermissionToRole(
            @PathVariable UUID id,
            @RequestParam UUID permissionId,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        roleService.addPermissionToRole(id, permissionId, updatedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "added", true,
            "roleId", id,
            "permissionId", permissionId,
            "addedAt", LocalDateTime.now()
        )));
    }

    @DeleteMapping("/{id}/permissions")
    @Operation(summary = "Remove Permission from Role", description = "Remove permission from role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removePermissionFromRole(
            @PathVariable UUID id,
            @RequestParam UUID permissionId,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        roleService.removePermissionFromRole(id, permissionId, updatedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "removed", true,
            "roleId", id,
            "permissionId", permissionId,
            "removedAt", LocalDateTime.now()
        )));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Set Role Permissions", description = "Set all permissions for role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setRolePermissions(
            @PathVariable UUID id,
            @RequestBody Set<UUID> permissionIds,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        roleService.setRolePermissions(id, permissionIds, updatedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "updated", true,
            "roleId", id,
            "permissionCount", permissionIds.size(),
            "updatedAt", LocalDateTime.now()
        )));
    }

    // ==================== Role Hierarchy ====================

    @GetMapping("/tenant/{tenantId}/hierarchy")
    @Operation(summary = "Get Role Hierarchy", description = "Get role hierarchy for tenant")
    public ResponseEntity<ApiResponse<List<Role>>> getRoleHierarchy(@PathVariable UUID tenantId) {
        List<Role> hierarchy = roleService.getRoleHierarchy(tenantId);
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get Child Roles", description = "Get child roles")
    public ResponseEntity<ApiResponse<List<Role>>> getChildRoles(@PathVariable UUID id) {
        List<Role> childRoles = roleService.getChildRoles(id);
        return ResponseEntity.ok(ApiResponse.success(childRoles));
    }

    @GetMapping("/{id}/descendants")
    @Operation(summary = "Get Descendant Roles", description = "Get all descendant roles")
    public ResponseEntity<ApiResponse<List<Role>>> getDescendantRoles(@PathVariable UUID id) {
        List<Role> descendants = roleService.getDescendantRoles(id);
        return ResponseEntity.ok(ApiResponse.success(descendants));
    }

    // ==================== User Role Queries ====================

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Roles", description = "Get roles assigned to user")
    public ResponseEntity<ApiResponse<List<UserRole>>> getUserRoles(@PathVariable UUID userId) {
        List<UserRole> userRoles = roleService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success(userRoles));
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "Get Users with Role", description = "Get users assigned to role")
    public ResponseEntity<ApiResponse<List<UserRole>>> getUsersWithRole(@PathVariable UUID id) {
        List<UserRole> usersWithRole = roleService.getUsersWithRole(id);
        return ResponseEntity.ok(ApiResponse.success(usersWithRole));
    }

    // ==================== Permission Queries ====================

    @GetMapping("/user/{userId}/permissions")
    @Operation(summary = "Get User Permissions", description = "Get effective permissions for user")
    public ResponseEntity<ApiResponse<Set<Permission>>> getUserPermissions(@PathVariable UUID userId) {
        Set<Permission> permissions = roleService.calculateEffectivePermissions(userId);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/user/{userId}/permissions/check")
    @Operation(summary = "Check User Permission", description = "Check if user has specific permission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUserPermission(
            @PathVariable UUID userId,
            @RequestParam String resource,
            @RequestParam String action) {
        
        boolean hasPermission = roleService.userHasPermission(userId, resource, action);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "userId", userId,
            "resource", resource,
            "action", action,
            "hasPermission", hasPermission,
            "checkedAt", LocalDateTime.now()
        )));
    }

    // ==================== Assignable Roles ====================

    @GetMapping("/tenant/{tenantId}/assignable")
    @Operation(summary = "Get Assignable Roles", description = "Get roles that can be assigned")
    public ResponseEntity<ApiResponse<List<Role>>> getAssignableRoles(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "false") boolean includeSystemRoles) {
        
        List<Role> assignableRoles = roleService.getAssignableRoles(tenantId, includeSystemRoles);
        return ResponseEntity.ok(ApiResponse.success(assignableRoles));
    }

    // ==================== Role Statistics ====================

    @GetMapping("/tenant/{tenantId}/statistics")
    @Operation(summary = "Get Role Statistics", description = "Get role assignment statistics")
    public ResponseEntity<ApiResponse<Map<UUID, Long>>> getRoleStatistics(@PathVariable UUID tenantId) {
        Map<UUID, Long> statistics = roleService.getRoleAssignmentStatistics(tenantId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/tenant/{tenantId}/expiring")
    @Operation(summary = "Get Expiring Roles", description = "Get roles expiring soon")
    public ResponseEntity<ApiResponse<List<UserRole>>> getExpiringRoles(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "7") int warningDays) {
        
        List<UserRole> expiringRoles = roleService.getRolesExpiringSoon(tenantId, warningDays);
        return ResponseEntity.ok(ApiResponse.success(expiringRoles));
    }

    // ==================== Maintenance Operations ====================

    @PostMapping("/maintenance/process-expired")
    @Operation(summary = "Process Expired Roles", description = "Process expired role assignments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processExpiredRoles() {
        roleService.processExpiredRoleAssignments();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "processed", true,
            "processedAt", LocalDateTime.now()
        )));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "role-service",
            "timestamp", LocalDateTime.now()
        )));
    }
}