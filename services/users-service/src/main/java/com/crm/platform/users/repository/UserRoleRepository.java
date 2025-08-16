package com.crm.platform.users.repository;

import com.crm.platform.users.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user-role association operations
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Find user-role association
     */
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Find all active roles for a user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.isActive = true AND " +
           "(ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findActiveUserRoles(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find all roles for a user (including expired)
     */
    List<UserRole> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find users with a specific role
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.roleId = :roleId AND ur.isActive = true AND " +
           "(ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findUsersWithRole(@Param("roleId") UUID roleId, @Param("now") LocalDateTime now);

    /**
     * Find user roles by tenant
     */
    List<UserRole> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Find user roles by tenant with pagination
     */
    Page<UserRole> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find expiring user roles
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.tenantId = :tenantId AND ur.isActive = true AND " +
           "ur.expiresAt BETWEEN :startDate AND :endDate")
    List<UserRole> findExpiringUserRoles(@Param("tenantId") UUID tenantId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find expired user roles
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.tenantId = :tenantId AND ur.isActive = true AND " +
           "ur.expiresAt < :now")
    List<UserRole> findExpiredUserRoles(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);

    /**
     * Find inherited user roles
     */
    List<UserRole> findByUserIdAndIsInheritedTrue(UUID userId);

    /**
     * Find direct (non-inherited) user roles
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.isInherited = false AND ur.isActive = true AND " +
           "(ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findDirectUserRoles(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Count active roles for a user
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.userId = :userId AND ur.isActive = true AND " +
           "(ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    long countActiveUserRoles(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Count users with a specific role
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.roleId = :roleId AND ur.isActive = true AND " +
           "(ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    long countUsersWithRole(@Param("roleId") UUID roleId, @Param("now") LocalDateTime now);

    /**
     * Check if user has role
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId AND " +
           "ur.isActive = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    boolean userHasRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId, @Param("now") LocalDateTime now);

    /**
     * Deactivate user role
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.isActive = false WHERE ur.userId = :userId AND ur.roleId = :roleId")
    void deactivateUserRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * Deactivate all user roles
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.isActive = false WHERE ur.userId = :userId")
    void deactivateAllUserRoles(@Param("userId") UUID userId);

    /**
     * Deactivate expired roles
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.isActive = false WHERE ur.expiresAt < :now AND ur.isActive = true")
    void deactivateExpiredRoles(@Param("now") LocalDateTime now);

    /**
     * Extend role expiration
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.expiresAt = :newExpiryDate WHERE ur.userId = :userId AND ur.roleId = :roleId")
    void extendRoleExpiration(@Param("userId") UUID userId, @Param("roleId") UUID roleId, 
                             @Param("newExpiryDate") LocalDateTime newExpiryDate);

    /**
     * Find user roles by assignment type
     */
    List<UserRole> findByUserIdAndAssignmentType(UUID userId, UserRole.AssignmentType assignmentType);

    /**
     * Find user roles assigned by specific user
     */
    List<UserRole> findByAssignedByOrderByCreatedAtDesc(UUID assignedBy);

    /**
     * Get role assignment statistics for tenant
     */
    @Query("SELECT ur.roleId, COUNT(ur) FROM UserRole ur WHERE ur.tenantId = :tenantId AND ur.isActive = true AND " +
           "(ur.expiresAt IS NULL OR ur.expiresAt > :now) GROUP BY ur.roleId ORDER BY COUNT(ur) DESC")
    List<Object[]> getRoleAssignmentStats(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);

    /**
     * Find roles that will expire soon
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.tenantId = :tenantId AND ur.isActive = true AND " +
           "ur.expiresAt BETWEEN :now AND :warningDate ORDER BY ur.expiresAt ASC")
    List<UserRole> findRolesExpiringSoon(@Param("tenantId") UUID tenantId, 
                                        @Param("now") LocalDateTime now,
                                        @Param("warningDate") LocalDateTime warningDate);

    /**
     * Delete old user role records
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.isActive = false AND ur.updatedAt < :cutoffDate")
    void deleteOldInactiveUserRoles(@Param("cutoffDate") LocalDateTime cutoffDate);
}