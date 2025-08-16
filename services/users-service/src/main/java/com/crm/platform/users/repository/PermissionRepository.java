package com.crm.platform.users.repository;

import com.crm.platform.users.entity.Permission;
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
 * Repository for permission operations
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    /**
     * Find permission by resource and action
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /**
     * Find all active permissions
     */
    List<Permission> findByIsActiveTrueOrderByCategoryAscResourceAscActionAsc();

    /**
     * Find permissions by resource
     */
    List<Permission> findByResourceAndIsActiveTrueOrderByActionAsc(String resource);

    /**
     * Find permissions by category
     */
    List<Permission> findByCategoryAndIsActiveTrueOrderByResourceAscActionAsc(String category);

    /**
     * Find system permissions
     */
    List<Permission> findByIsSystemPermissionTrueAndIsActiveTrueOrderByCategoryAscResourceAscActionAsc();

    /**
     * Find custom permissions (non-system)
     */
    List<Permission> findByIsSystemPermissionFalseAndIsActiveTrueOrderByCategoryAscResourceAscActionAsc();

    /**
     * Search permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.isActive = true AND " +
           "(LOWER(p.resource) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Permission> searchPermissions(@Param("search") String search, Pageable pageable);

    /**
     * Find all distinct resources
     */
    @Query("SELECT DISTINCT p.resource FROM Permission p WHERE p.isActive = true ORDER BY p.resource")
    List<String> findAllResources();

    /**
     * Find all distinct actions
     */
    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.isActive = true ORDER BY p.action")
    List<String> findAllActions();

    /**
     * Find all distinct categories
     */
    @Query("SELECT DISTINCT p.category FROM Permission p WHERE p.isActive = true AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();

    /**
     * Find actions for a specific resource
     */
    @Query("SELECT p.action FROM Permission p WHERE p.resource = :resource AND p.isActive = true ORDER BY p.action")
    List<String> findActionsByResource(@Param("resource") String resource);

    /**
     * Check if permission exists
     */
    boolean existsByResourceAndActionAndIsActiveTrue(String resource, String action);

    /**
     * Count permissions by category
     */
    long countByCategoryAndIsActiveTrue(String category);

    /**
     * Count system permissions
     */
    long countByIsSystemPermissionTrueAndIsActiveTrue();

    /**
     * Find permissions used by roles
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r WHERE r.tenantId = :tenantId AND r.isActive = true")
    List<Permission> findPermissionsUsedByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Find unused permissions (not assigned to any role)
     */
    @Query("SELECT p FROM Permission p WHERE p.isActive = true AND p.roles IS EMPTY")
    List<Permission> findUnusedPermissions();

    /**
     * Find permissions by priority
     */
    List<Permission> findByIsActiveTrueOrderByPriorityDescResourceAscActionAsc();
}