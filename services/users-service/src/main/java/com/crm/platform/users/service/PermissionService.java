package com.crm.platform.users.service;

import com.crm.platform.users.entity.Permission;
import com.crm.platform.users.repository.PermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for permission management
 */
@Service
@Transactional
public class PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    // ==================== Permission CRUD Operations ====================

    /**
     * Create a new permission
     */
    public Permission createPermission(String resource, String action, String description, 
                                     String category, UUID createdBy, HttpServletRequest request) {
        logger.info("Creating permission: {}:{}", resource, action);

        // Check if permission already exists
        if (permissionRepository.existsByResourceAndActionAndIsActiveTrue(resource, action)) {
            throw new IllegalArgumentException("Permission already exists for resource: " + resource + " and action: " + action);
        }

        Permission permission = new Permission(resource, action, description, category);
        permission.setCreatedBy(createdBy);

        Permission savedPermission = permissionRepository.save(permission);
        logger.info("Permission created successfully: {} with ID: {}", permission.getPermissionKey(), savedPermission.getId());
        return savedPermission;
    }

    /**
     * Update permission
     */
    public Permission updatePermission(UUID permissionId, String description, String category, 
                                     Integer priority, UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating permission: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));

        if (Boolean.TRUE.equals(permission.getIsSystemPermission())) {
            throw new IllegalArgumentException("Cannot update system permission");
        }

        if (description != null) permission.setDescription(description);
        if (category != null) permission.setCategory(category);
        if (priority != null) permission.setPriority(priority);
        permission.setUpdatedBy(updatedBy);

        Permission updatedPermission = permissionRepository.save(permission);
        logger.info("Permission updated successfully: {}", permissionId);
        return updatedPermission;
    }

    /**
     * Delete permission (soft delete)
     */
    public void deletePermission(UUID permissionId, UUID deletedBy, HttpServletRequest request) {
        logger.info("Deleting permission: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));

        if (Boolean.TRUE.equals(permission.getIsSystemPermission())) {
            throw new IllegalArgumentException("Cannot delete system permission");
        }

        // Check if permission is used by any roles
        if (!permission.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete permission that is assigned to roles. " +
                    "Please remove from all roles first.");
        }

        permission.setIsActive(false);
        permission.setUpdatedBy(deletedBy);
        permissionRepository.save(permission);

        logger.info("Permission deleted successfully: {}", permissionId);
    }

    // ==================== Query Methods ====================

    /**
     * Get all active permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByIsActiveTrueOrderByCategoryAscResourceAscActionAsc();
    }

    /**
     * Get permissions with pagination
     */
    @Transactional(readOnly = true)
    public Page<Permission> getPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable);
    }

    /**
     * Search permissions
     */
    @Transactional(readOnly = true)
    public Page<Permission> searchPermissions(String search, Pageable pageable) {
        return permissionRepository.searchPermissions(search, pageable);
    }

    /**
     * Get permission by ID
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionById(UUID permissionId) {
        return permissionRepository.findById(permissionId);
    }

    /**
     * Get permission by resource and action
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByResourceAndAction(String resource, String action) {
        return permissionRepository.findByResourceAndAction(resource, action);
    }

    /**
     * Get permissions by resource
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResourceAndIsActiveTrueOrderByActionAsc(resource);
    }

    /**
     * Get permissions by category
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategoryAndIsActiveTrueOrderByResourceAscActionAsc(category);
    }

    /**
     * Get system permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getSystemPermissions() {
        return permissionRepository.findByIsSystemPermissionTrueAndIsActiveTrueOrderByCategoryAscResourceAscActionAsc();
    }

    /**
     * Get custom permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getCustomPermissions() {
        return permissionRepository.findByIsSystemPermissionFalseAndIsActiveTrueOrderByCategoryAscResourceAscActionAsc();
    }

    /**
     * Get all resources
     */
    @Transactional(readOnly = true)
    public List<String> getAllResources() {
        return permissionRepository.findAllResources();
    }

    /**
     * Get all actions
     */
    @Transactional(readOnly = true)
    public List<String> getAllActions() {
        return permissionRepository.findAllActions();
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return permissionRepository.findAllCategories();
    }

    /**
     * Get actions for resource
     */
    @Transactional(readOnly = true)
    public List<String> getActionsByResource(String resource) {
        return permissionRepository.findActionsByResource(resource);
    }

    /**
     * Get permissions used by tenant
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsUsedByTenant(UUID tenantId) {
        return permissionRepository.findPermissionsUsedByTenant(tenantId);
    }

    /**
     * Get unused permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getUnusedPermissions() {
        return permissionRepository.findUnusedPermissions();
    }

    // ==================== System Permission Initialization ====================

    /**
     * Initialize system permissions
     */
    public void initializeSystemPermissions() {
        logger.info("Initializing system permissions");

        createSystemPermissionIfNotExists("users", "create", "Create users", "user_management");
        createSystemPermissionIfNotExists("users", "read", "View users", "user_management");
        createSystemPermissionIfNotExists("users", "update", "Update users", "user_management");
        createSystemPermissionIfNotExists("users", "delete", "Delete users", "user_management");
        createSystemPermissionIfNotExists("users", "list", "List users", "user_management");
        createSystemPermissionIfNotExists("users", "export", "Export users", "user_management");
        createSystemPermissionIfNotExists("users", "import", "Import users", "user_management");

        createSystemPermissionIfNotExists("roles", "create", "Create roles", "access_control");
        createSystemPermissionIfNotExists("roles", "read", "View roles", "access_control");
        createSystemPermissionIfNotExists("roles", "update", "Update roles", "access_control");
        createSystemPermissionIfNotExists("roles", "delete", "Delete roles", "access_control");
        createSystemPermissionIfNotExists("roles", "assign", "Assign roles", "access_control");

        createSystemPermissionIfNotExists("permissions", "create", "Create permissions", "access_control");
        createSystemPermissionIfNotExists("permissions", "read", "View permissions", "access_control");
        createSystemPermissionIfNotExists("permissions", "update", "Update permissions", "access_control");
        createSystemPermissionIfNotExists("permissions", "delete", "Delete permissions", "access_control");

        createSystemPermissionIfNotExists("contacts", "create", "Create contacts", "crm");
        createSystemPermissionIfNotExists("contacts", "read", "View contacts", "crm");
        createSystemPermissionIfNotExists("contacts", "update", "Update contacts", "crm");
        createSystemPermissionIfNotExists("contacts", "delete", "Delete contacts", "crm");
        createSystemPermissionIfNotExists("contacts", "list", "List contacts", "crm");
        createSystemPermissionIfNotExists("contacts", "export", "Export contacts", "crm");
        createSystemPermissionIfNotExists("contacts", "import", "Import contacts", "crm");

        createSystemPermissionIfNotExists("deals", "create", "Create deals", "sales");
        createSystemPermissionIfNotExists("deals", "read", "View deals", "sales");
        createSystemPermissionIfNotExists("deals", "update", "Update deals", "sales");
        createSystemPermissionIfNotExists("deals", "delete", "Delete deals", "sales");
        createSystemPermissionIfNotExists("deals", "list", "List deals", "sales");
        createSystemPermissionIfNotExists("deals", "manage", "Manage deals", "sales");

        createSystemPermissionIfNotExists("leads", "create", "Create leads", "sales");
        createSystemPermissionIfNotExists("leads", "read", "View leads", "sales");
        createSystemPermissionIfNotExists("leads", "update", "Update leads", "sales");
        createSystemPermissionIfNotExists("leads", "delete", "Delete leads", "sales");
        createSystemPermissionIfNotExists("leads", "list", "List leads", "sales");
        createSystemPermissionIfNotExists("leads", "convert", "Convert leads", "sales");

        createSystemPermissionIfNotExists("accounts", "create", "Create accounts", "sales");
        createSystemPermissionIfNotExists("accounts", "read", "View accounts", "sales");
        createSystemPermissionIfNotExists("accounts", "update", "Update accounts", "sales");
        createSystemPermissionIfNotExists("accounts", "delete", "Delete accounts", "sales");
        createSystemPermissionIfNotExists("accounts", "list", "List accounts", "sales");

        createSystemPermissionIfNotExists("activities", "create", "Create activities", "activities");
        createSystemPermissionIfNotExists("activities", "read", "View activities", "activities");
        createSystemPermissionIfNotExists("activities", "update", "Update activities", "activities");
        createSystemPermissionIfNotExists("activities", "delete", "Delete activities", "activities");
        createSystemPermissionIfNotExists("activities", "list", "List activities", "activities");

        createSystemPermissionIfNotExists("reports", "create", "Create reports", "analytics");
        createSystemPermissionIfNotExists("reports", "read", "View reports", "analytics");
        createSystemPermissionIfNotExists("reports", "update", "Update reports", "analytics");
        createSystemPermissionIfNotExists("reports", "delete", "Delete reports", "analytics");
        createSystemPermissionIfNotExists("reports", "export", "Export reports", "analytics");

        createSystemPermissionIfNotExists("settings", "read", "View settings", "administration");
        createSystemPermissionIfNotExists("settings", "update", "Update settings", "administration");
        createSystemPermissionIfNotExists("settings", "admin", "Administer system", "administration");

        logger.info("System permissions initialization completed");
    }

    private void createSystemPermissionIfNotExists(String resource, String action, String description, String category) {
        if (!permissionRepository.existsByResourceAndActionAndIsActiveTrue(resource, action)) {
            Permission permission = new Permission(resource, action, description, category);
            permission.setIsSystemPermission(true);
            permissionRepository.save(permission);
            logger.debug("Created system permission: {}:{}", resource, action);
        }
    }

    // ==================== Validation Methods ====================

    /**
     * Validate permission exists
     */
    public boolean permissionExists(String resource, String action) {
        return permissionRepository.existsByResourceAndActionAndIsActiveTrue(resource, action);
    }

    /**
     * Validate permission is active
     */
    public boolean isPermissionActive(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .map(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElse(false);
    }

    /**
     * Get permission statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getPermissionStatistics() {
        long totalPermissions = permissionRepository.count();
        long systemPermissions = permissionRepository.countByIsSystemPermissionTrueAndIsActiveTrue();
        long customPermissions = totalPermissions - systemPermissions;
        long unusedPermissions = permissionRepository.findUnusedPermissions().size();

        return Map.of(
                "total", totalPermissions,
                "system", systemPermissions,
                "custom", customPermissions,
                "unused", unusedPermissions
        );
    }
}