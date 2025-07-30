package com.crm.platform.users.controller;

import com.crm.platform.users.dto.CreatePermissionRequest;
import com.crm.platform.users.entity.Permission;
import com.crm.platform.users.service.PermissionService;
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
import java.util.UUID;

/**
 * Controller for permission management operations
 */
@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permission Management", description = "Permission management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PermissionController {

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // ==================== Permission CRUD Operations ====================

    @PostMapping
    @Operation(summary = "Create Permission", description = "Create a new permission")
    public ResponseEntity<ApiResponse<Permission>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID createdBy,
            HttpServletRequest httpRequest) {
        
        Permission permission = permissionService.createPermission(
            request.getResource(),
            request.getAction(),
            request.getDescription(),
            request.getCategory(),
            createdBy,
            httpRequest
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(permission));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Permission", description = "Get permission by ID")
    public ResponseEntity<ApiResponse<Permission>> getPermissionById(@PathVariable UUID id) {
        return permissionService.getPermissionById(id)
                .map(permission -> ResponseEntity.ok(ApiResponse.success(permission)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get All Permissions", description = "Get all active permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get Permissions Paginated", description = "Get permissions with pagination")
    public ResponseEntity<ApiResponse<Page<Permission>>> getPermissionsPaginated(Pageable pageable) {
        Page<Permission> permissions = permissionService.getPermissions(pageable);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Permissions", description = "Search permissions by resource, action, or description")
    public ResponseEntity<ApiResponse<Page<Permission>>> searchPermissions(
            @RequestParam String query,
            Pageable pageable) {
        Page<Permission> permissions = permissionService.searchPermissions(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Permission", description = "Update permission details")
    public ResponseEntity<ApiResponse<Permission>> updatePermission(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        Permission permission = permissionService.updatePermission(
            id,
            (String) updates.get("description"),
            (String) updates.get("category"),
            (Integer) updates.get("priority"),
            updatedBy,
            request
        );
        
        return ResponseEntity.ok(ApiResponse.success(permission));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Permission", description = "Delete a permission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePermission(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID deletedBy,
            HttpServletRequest request) {
        
        permissionService.deletePermission(id, deletedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "deleted", true,
            "permissionId", id,
            "deletedAt", LocalDateTime.now()
        )));
    }

    // ==================== Permission Queries ====================

    @GetMapping("/resource/{resource}")
    @Operation(summary = "Get Permissions by Resource", description = "Get all permissions for a resource")
    public ResponseEntity<ApiResponse<List<Permission>>> getPermissionsByResource(@PathVariable String resource) {
        List<Permission> permissions = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get Permissions by Category", description = "Get all permissions in a category")
    public ResponseEntity<ApiResponse<List<Permission>>> getPermissionsByCategory(@PathVariable String category) {
        List<Permission> permissions = permissionService.getPermissionsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/system")
    @Operation(summary = "Get System Permissions", description = "Get all system permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getSystemPermissions() {
        List<Permission> permissions = permissionService.getSystemPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/custom")
    @Operation(summary = "Get Custom Permissions", description = "Get all custom permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getCustomPermissions() {
        List<Permission> permissions = permissionService.getCustomPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/unused")
    @Operation(summary = "Get Unused Permissions", description = "Get permissions not assigned to any role")
    public ResponseEntity<ApiResponse<List<Permission>>> getUnusedPermissions() {
        List<Permission> permissions = permissionService.getUnusedPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    // ==================== Permission Metadata ====================

    @GetMapping("/resources")
    @Operation(summary = "Get All Resources", description = "Get all available resources")
    public ResponseEntity<ApiResponse<List<String>>> getAllResources() {
        List<String> resources = permissionService.getAllResources();
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    @GetMapping("/actions")
    @Operation(summary = "Get All Actions", description = "Get all available actions")
    public ResponseEntity<ApiResponse<List<String>>> getAllActions() {
        List<String> actions = permissionService.getAllActions();
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get All Categories", description = "Get all available categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        List<String> categories = permissionService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/resource/{resource}/actions")
    @Operation(summary = "Get Actions for Resource", description = "Get all actions available for a resource")
    public ResponseEntity<ApiResponse<List<String>>> getActionsByResource(@PathVariable String resource) {
        List<String> actions = permissionService.getActionsByResource(resource);
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    // ==================== Permission Validation ====================

    @GetMapping("/exists")
    @Operation(summary = "Check Permission Exists", description = "Check if permission exists")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPermissionExists(
            @RequestParam String resource,
            @RequestParam String action) {
        
        boolean exists = permissionService.permissionExists(resource, action);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "resource", resource,
            "action", action,
            "exists", exists,
            "checkedAt", LocalDateTime.now()
        )));
    }

    @GetMapping("/{id}/active")
    @Operation(summary = "Check Permission Active", description = "Check if permission is active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPermissionActive(@PathVariable UUID id) {
        boolean isActive = permissionService.isPermissionActive(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "permissionId", id,
            "isActive", isActive,
            "checkedAt", LocalDateTime.now()
        )));
    }

    // ==================== Tenant-Specific Queries ====================

    @GetMapping("/tenant/{tenantId}/used")
    @Operation(summary = "Get Permissions Used by Tenant", description = "Get permissions used by roles in tenant")
    public ResponseEntity<ApiResponse<List<Permission>>> getPermissionsUsedByTenant(@PathVariable UUID tenantId) {
        List<Permission> permissions = permissionService.getPermissionsUsedByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    // ==================== Permission Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "Get Permission Statistics", description = "Get permission usage statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPermissionStatistics() {
        Map<String, Long> statistics = permissionService.getPermissionStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    // ==================== System Operations ====================

    @PostMapping("/initialize-system")
    @Operation(summary = "Initialize System Permissions", description = "Initialize default system permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeSystemPermissions() {
        permissionService.initializeSystemPermissions();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "initialized", true,
            "initializedAt", LocalDateTime.now()
        )));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "permission-service",
            "timestamp", LocalDateTime.now()
        )));
    }
}