package com.crm.platform.users.repository;

import com.crm.platform.users.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for role operations with hierarchical support
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find role by name and tenant
     */
    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    /**
     * Find all roles for a tenant
     */
    List<Role> findByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Find all roles for a tenant with pagination
     */
    Page<Role> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    /**
     * Find roles by type
     */
    List<Role> findByTenantIdAndRoleTypeAndIsActiveTrue(UUID tenantId, Role.RoleType roleType);

    /**
     * Find system roles
     */
    List<Role> findByIsSystemRoleTrueAndIsActiveTrue();

    /**
     * Find child roles of a parent role
     */
    List<Role> findByParentRoleIdAndIsActiveTrue(UUID parentRoleId);

    /**
     * Find all descendant roles using hierarchy path
     */
    @Query("SELECT r FROM Role r WHERE r.hierarchyPath LIKE CONCAT(:hierarchyPath, '%') AND r.isActive = true")
    List<Role> findDescendantRoles(@Param("hierarchyPath") String hierarchyPath);

    /**
     * Find roles by hierarchy level
     */
    List<Role> findByTenantIdAndHierarchyLevelAndIsActiveTrue(UUID tenantId, Integer hierarchyLevel);

    /**
     * Find top-level roles (no parent)
     */
    List<Role> findByTenantIdAndParentRoleIdIsNullAndIsActiveTrue(UUID tenantId);

    /**
     * Search roles by name
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.isActive = true AND " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Role> searchRoles(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    /**
     * Find roles with specific permission
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.permissions p WHERE r.tenantId = :tenantId AND " +
           "r.isActive = true AND p.resource = :resource AND p.action = :action")
    List<Role> findRolesWithPermission(@Param("tenantId") UUID tenantId, 
                                      @Param("resource") String resource, 
                                      @Param("action") String action);

    /**
     * Count roles by tenant
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Count roles by type
     */
    long countByTenantIdAndRoleTypeAndIsActiveTrue(UUID tenantId, Role.RoleType roleType);

    /**
     * Find roles ordered by priority
     */
    List<Role> findByTenantIdAndIsActiveTrueOrderByPriorityDescNameAsc(UUID tenantId);

    /**
     * Check if role name exists in tenant
     */
    boolean existsByNameAndTenantIdAndIsActiveTrue(String name, UUID tenantId);

    /**
     * Find roles that can be assigned (not system roles unless specified)
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.isActive = true AND " +
           "(:includeSystemRoles = true OR r.isSystemRole = false)")
    List<Role> findAssignableRoles(@Param("tenantId") UUID tenantId, 
                                  @Param("includeSystemRoles") boolean includeSystemRoles);

    /**
     * Find roles with expiring assignments
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN UserRole ur ON r.id = ur.roleId " +
           "WHERE r.tenantId = :tenantId AND ur.expiresAt BETWEEN :startDate AND :endDate")
    List<Role> findRolesWithExpiringAssignments(@Param("tenantId") UUID tenantId,
                                               @Param("startDate") java.time.LocalDateTime startDate,
                                               @Param("endDate") java.time.LocalDateTime endDate);
}