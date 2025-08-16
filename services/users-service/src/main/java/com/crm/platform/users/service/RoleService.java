package com.crm.platform.users.service;

import com.crm.platform.users.entity.Permission;
import com.crm.platform.users.entity.Role;
import com.crm.platform.users.entity.UserRole;
import com.crm.platform.users.repository.PermissionRepository;
import com.crm.platform.users.repository.RoleRepository;
import com.crm.platform.users.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for role management including assignment, removal, hierarchy, and permission calculation
 */
@Service
@Transactional
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserAuditService userAuditService;

    @Autowired
    public RoleService(RoleRepository roleRepository,
                      PermissionRepository permissionRepository,
                      UserRoleRepository userRoleRepository,
                      UserAuditService userAuditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userAuditService = userAuditService;
    }

    // ==================== Role CRUD Operations ====================

    /**
     * Create a new role
     */
    public Role createRole(String name, String description, UUID tenantId, Role.RoleType roleType, 
                          UUID parentRoleId, UUID createdBy, HttpServletRequest request) {
        logger.info("Creating role: {} for tenant: {}", name, tenantId);

        // Check if role name already exists in tenant
        if (roleRepository.existsByNameAndTenantIdAndIsActiveTrue(name, tenantId)) {
            throw new IllegalArgumentException("Role with name '" + name + "' already exists in this tenant");
        }

        Role role = new Role(name, description, tenantId, roleType);
        role.setCreatedBy(createdBy);

        // Handle hierarchy
        if (parentRoleId != null) {
            Role parentRole = roleRepository.findById(parentRoleId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent role not found"));
            
            if (!parentRole.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Parent role must be in the same tenant");
            }

            role.setParentRoleId(parentRoleId);
            role.setHierarchyLevel(parentRole.getHierarchyLevel() + 1);
            role.setHierarchyPath(parentRole.getHierarchyPath() + role.getId() + "/");
        } else {
            role.setHierarchyLevel(0);
        }

        Role savedRole = roleRepository.save(role);

        // Update hierarchy path after save (when ID is available)
        if (parentRoleId == null) {
            savedRole.setHierarchyPath("/" + savedRole.getId() + "/");
            savedRole = roleRepository.save(savedRole);
        }

        logger.info("Role created successfully: {} with ID: {}", name, savedRole.getId());
        return savedRole;
    }

    /**
     * Update role
     */
    public Role updateRole(UUID roleId, String name, String description, String color, String icon, 
                          Integer priority, UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        // Check if new name conflicts with existing roles
        if (name != null && !name.equals(role.getName())) {
            if (roleRepository.existsByNameAndTenantIdAndIsActiveTrue(name, role.getTenantId())) {
                throw new IllegalArgumentException("Role with name '" + name + "' already exists in this tenant");
            }
            role.setName(name);
        }

        if (description != null) role.setDescription(description);
        if (color != null) role.setColor(color);
        if (icon != null) role.setIcon(icon);
        if (priority != null) role.setPriority(priority);
        role.setUpdatedBy(updatedBy);

        Role updatedRole = roleRepository.save(role);
        logger.info("Role updated successfully: {}", roleId);
        return updatedRole;
    }

    /**
     * Delete role (soft delete)
     */
    public void deleteRole(UUID roleId, UUID deletedBy, HttpServletRequest request) {
        logger.info("Deleting role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalArgumentException("Cannot delete system role");
        }

        // Check if role has active assignments
        long activeAssignments = userRoleRepository.countUsersWithRole(roleId, LocalDateTime.now());
        if (activeAssignments > 0) {
            throw new IllegalArgumentException("Cannot delete role with active user assignments. " +
                    "Please remove all user assignments first.");
        }

        // Check if role has child roles
        List<Role> childRoles = roleRepository.findByParentRoleIdAndIsActiveTrue(roleId);
        if (!childRoles.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete role with child roles. " +
                    "Please delete or reassign child roles first.");
        }

        role.setIsActive(false);
        role.setUpdatedBy(deletedBy);
        roleRepository.save(role);

        logger.info("Role deleted successfully: {}", roleId);
    }

    // ==================== Role Assignment Operations ====================

    /**
     * Assign role to user
     */
    public UserRole assignRoleToUser(UUID userId, UUID roleId, UUID tenantId, LocalDateTime expiresAt, 
                                    String assignmentReason, UUID assignedBy, HttpServletRequest request) {
        logger.info("Assigning role: {} to user: {}", roleId, userId);

        // Validate role exists and is active
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new IllegalArgumentException("Cannot assign inactive role");
        }

        if (!role.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Role must be in the same tenant as user");
        }

        // Check if user already has this role
        if (userRoleRepository.userHasRole(userId, roleId, LocalDateTime.now())) {
            throw new IllegalArgumentException("User already has this role assigned");
        }

        UserRole userRole = new UserRole(userId, roleId, tenantId, expiresAt, assignedBy);
        userRole.setAssignmentReason(assignmentReason);
        userRole.setAssignmentType(expiresAt != null ? 
                UserRole.AssignmentType.TEMPORARY : UserRole.AssignmentType.DIRECT);

        UserRole savedUserRole = userRoleRepository.save(userRole);

        // Log role assignment
        userAuditService.logUserUpdate(null, null, assignedBy, request);

        logger.info("Role assigned successfully: {} to user: {}", roleId, userId);
        return savedUserRole;
    }

    /**
     * Remove role from user
     */
    public void removeRoleFromUser(UUID userId, UUID roleId, UUID removedBy, HttpServletRequest request) {
        logger.info("Removing role: {} from user: {}", roleId, userId);

        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("User role assignment not found"));

        if (!userRole.isValid()) {
            throw new IllegalArgumentException("Role assignment is already inactive or expired");
        }

        userRole.setIsActive(false);
        userRoleRepository.save(userRole);

        // Log role removal
        userAuditService.logUserUpdate(null, null, removedBy, request);

        logger.info("Role removed successfully: {} from user: {}", roleId, userId);
    }

    /**
     * Extend role expiration
     */
    public UserRole extendRoleExpiration(UUID userId, UUID roleId, LocalDateTime newExpiryDate, 
                                        UUID extendedBy, HttpServletRequest request) {
        logger.info("Extending role expiration: {} for user: {} until: {}", roleId, userId, newExpiryDate);

        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("User role assignment not found"));

        if (!userRole.isValid()) {
            throw new IllegalArgumentException("Cannot extend expired or inactive role assignment");
        }

        userRole.extend(newExpiryDate);
        UserRole updatedUserRole = userRoleRepository.save(userRole);

        logger.info("Role expiration extended successfully: {} for user: {}", roleId, userId);
        return updatedUserRole;
    }

    // ==================== Permission Management ====================

    /**
     * Add permission to role
     */
    public void addPermissionToRole(UUID roleId, UUID permissionId, UUID updatedBy, HttpServletRequest request) {
        logger.info("Adding permission: {} to role: {}", permissionId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));

        if (!Boolean.TRUE.equals(permission.getIsActive())) {
            throw new IllegalArgumentException("Cannot add inactive permission to role");
        }

        role.addPermission(permission);
        role.setUpdatedBy(updatedBy);
        roleRepository.save(role);

        logger.info("Permission added successfully: {} to role: {}", permissionId, roleId);
    }

    /**
     * Remove permission from role
     */
    public void removePermissionFromRole(UUID roleId, UUID permissionId, UUID updatedBy, HttpServletRequest request) {
        logger.info("Removing permission: {} from role: {}", permissionId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));

        role.removePermission(permission);
        role.setUpdatedBy(updatedBy);
        roleRepository.save(role);

        logger.info("Permission removed successfully: {} from role: {}", permissionId, roleId);
    }

    /**
     * Set role permissions (replace all)
     */
    public void setRolePermissions(UUID roleId, Set<UUID> permissionIds, UUID updatedBy, HttpServletRequest request) {
        logger.info("Setting permissions for role: {} with {} permissions", roleId, permissionIds.size());

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        Set<Permission> permissions = new HashSet<>();
        for (UUID permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + permissionId));
            
            if (!Boolean.TRUE.equals(permission.getIsActive())) {
                throw new IllegalArgumentException("Cannot add inactive permission: " + permissionId);
            }
            
            permissions.add(permission);
        }

        role.setPermissions(permissions);
        role.setUpdatedBy(updatedBy);
        roleRepository.save(role);

        logger.info("Permissions set successfully for role: {}", roleId);
    }

    // ==================== Effective Permissions Calculation ====================

    /**
     * Calculate effective permissions for a user (including inherited permissions)
     */
    public Set<Permission> calculateEffectivePermissions(UUID userId) {
        logger.debug("Calculating effective permissions for user: {}", userId);

        List<UserRole> activeUserRoles = userRoleRepository.findActiveUserRoles(userId, LocalDateTime.now());
        Set<Permission> effectivePermissions = new HashSet<>();

        for (UserRole userRole : activeUserRoles) {
            Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role != null && Boolean.TRUE.equals(role.getIsActive())) {
                // Add direct permissions
                effectivePermissions.addAll(role.getPermissions());
                
                // Add inherited permissions from parent roles
                effectivePermissions.addAll(getInheritedPermissions(role));
            }
        }

        logger.debug("Calculated {} effective permissions for user: {}", effectivePermissions.size(), userId);
        return effectivePermissions;
    }

    /**
     * Get inherited permissions from role hierarchy
     */
    private Set<Permission> getInheritedPermissions(Role role) {
        Set<Permission> inheritedPermissions = new HashSet<>();
        
        if (role.getParentRoleId() != null) {
            Role parentRole = roleRepository.findById(role.getParentRoleId()).orElse(null);
            if (parentRole != null && Boolean.TRUE.equals(parentRole.getIsActive())) {
                inheritedPermissions.addAll(parentRole.getPermissions());
                inheritedPermissions.addAll(getInheritedPermissions(parentRole)); // Recursive
            }
        }
        
        return inheritedPermissions;
    }

    /**
     * Check if user has specific permission
     */
    public boolean userHasPermission(UUID userId, String resource, String action) {
        Set<Permission> effectivePermissions = calculateEffectivePermissions(userId);
        return effectivePermissions.stream()
                .anyMatch(p -> p.matches(resource, action));
    }

    /**
     * Check if user has any permission for resource
     */
    public boolean userHasResourceAccess(UUID userId, String resource) {
        Set<Permission> effectivePermissions = calculateEffectivePermissions(userId);
        return effectivePermissions.stream()
                .anyMatch(p -> p.getResource().equals(resource));
    }

    // ==================== Role Hierarchy Management ====================

    /**
     * Get role hierarchy for tenant
     */
    public List<Role> getRoleHierarchy(UUID tenantId) {
        List<Role> allRoles = roleRepository.findByTenantIdAndIsActiveTrue(tenantId);
        return buildRoleHierarchy(allRoles);
    }

    /**
     * Build hierarchical structure from flat role list
     */
    private List<Role> buildRoleHierarchy(List<Role> roles) {
        Map<UUID, Role> roleMap = roles.stream()
                .collect(Collectors.toMap(Role::getId, role -> role));

        return roles.stream()
                .filter(role -> role.getParentRoleId() == null)
                .sorted(Comparator.comparing(Role::getName))
                .collect(Collectors.toList());
    }

    /**
     * Get child roles
     */
    public List<Role> getChildRoles(UUID roleId) {
        return roleRepository.findByParentRoleIdAndIsActiveTrue(roleId);
    }

    /**
     * Get descendant roles (all levels)
     */
    public List<Role> getDescendantRoles(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
        
        return roleRepository.findDescendantRoles(role.getHierarchyPath());
    }

    // ==================== Role Conflict Detection ====================

    /**
     * Detect role conflicts for user
     */
    public List<String> detectRoleConflicts(UUID userId) {
        List<String> conflicts = new ArrayList<>();
        List<UserRole> activeUserRoles = userRoleRepository.findActiveUserRoles(userId, LocalDateTime.now());

        // Check for conflicting roles (this is business logic specific)
        // Example: Admin and Regular User roles might conflict
        Set<String> roleNames = activeUserRoles.stream()
                .map(ur -> {
                    Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
                    return role != null ? role.getName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Add specific conflict detection logic here
        if (roleNames.contains("ADMIN") && roleNames.contains("GUEST")) {
            conflicts.add("Admin and Guest roles cannot be assigned to the same user");
        }

        return conflicts;
    }

    // ==================== Query Methods ====================

    /**
     * Get all roles for tenant
     */
    @Transactional(readOnly = true)
    public List<Role> getAllRoles(UUID tenantId) {
        return roleRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    /**
     * Get roles with pagination
     */
    @Transactional(readOnly = true)
    public Page<Role> getRoles(UUID tenantId, Pageable pageable) {
        return roleRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);
    }

    /**
     * Search roles
     */
    @Transactional(readOnly = true)
    public Page<Role> searchRoles(UUID tenantId, String search, Pageable pageable) {
        return roleRepository.searchRoles(tenantId, search, pageable);
    }

    /**
     * Get user roles
     */
    @Transactional(readOnly = true)
    public List<UserRole> getUserRoles(UUID userId) {
        return userRoleRepository.findActiveUserRoles(userId, LocalDateTime.now());
    }

    /**
     * Get users with role
     */
    @Transactional(readOnly = true)
    public List<UserRole> getUsersWithRole(UUID roleId) {
        return userRoleRepository.findUsersWithRole(roleId, LocalDateTime.now());
    }

    /**
     * Get role by ID
     */
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(UUID roleId) {
        return roleRepository.findById(roleId);
    }

    /**
     * Get assignable roles (excluding system roles unless specified)
     */
    @Transactional(readOnly = true)
    public List<Role> getAssignableRoles(UUID tenantId, boolean includeSystemRoles) {
        return roleRepository.findAssignableRoles(tenantId, includeSystemRoles);
    }

    // ==================== Cleanup and Maintenance ====================

    /**
     * Process expired role assignments
     */
    public void processExpiredRoleAssignments() {
        logger.info("Processing expired role assignments");
        userRoleRepository.deactivateExpiredRoles(LocalDateTime.now());
        logger.info("Expired role assignments processed");
    }

    /**
     * Get roles expiring soon
     */
    @Transactional(readOnly = true)
    public List<UserRole> getRolesExpiringSoon(UUID tenantId, int warningDays) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningDate = now.plusDays(warningDays);
        return userRoleRepository.findRolesExpiringSoon(tenantId, now, warningDate);
    }

    /**
     * Get role assignment statistics
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> getRoleAssignmentStatistics(UUID tenantId) {
        List<Object[]> stats = userRoleRepository.getRoleAssignmentStats(tenantId, LocalDateTime.now());
        return stats.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }
}