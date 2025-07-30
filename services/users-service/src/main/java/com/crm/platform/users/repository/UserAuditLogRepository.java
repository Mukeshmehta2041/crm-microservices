package com.crm.platform.users.repository;

import com.crm.platform.users.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for user audit log operations
 */
@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, UUID> {

    /**
     * Find audit logs for a specific user
     */
    Page<UserAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find audit logs for a tenant
     */
    Page<UserAuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Find audit logs by action type
     */
    List<UserAuditLog> findByUserIdAndActionOrderByCreatedAtDesc(UUID userId, String action);

    /**
     * Find audit logs by action type for tenant
     */
    Page<UserAuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(UUID tenantId, String action, Pageable pageable);

    /**
     * Find audit logs within date range
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.userId = :userId " +
           "AND ual.createdAt BETWEEN :startDate AND :endDate ORDER BY ual.createdAt DESC")
    List<UserAuditLog> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Find audit logs within date range for tenant
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.tenantId = :tenantId " +
           "AND ual.createdAt BETWEEN :startDate AND :endDate ORDER BY ual.createdAt DESC")
    Page<UserAuditLog> findByTenantIdAndDateRange(@Param("tenantId") UUID tenantId, 
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate, 
                                                 Pageable pageable);

    /**
     * Find audit logs by severity
     */
    List<UserAuditLog> findByTenantIdAndSeverityOrderByCreatedAtDesc(UUID tenantId, UserAuditLog.AuditSeverity severity);

    /**
     * Find audit logs by field name (for tracking specific field changes)
     */
    List<UserAuditLog> findByUserIdAndFieldNameOrderByCreatedAtDesc(UUID userId, String fieldName);

    /**
     * Find audit logs performed by a specific user
     */
    Page<UserAuditLog> findByPerformedByOrderByCreatedAtDesc(UUID performedBy, Pageable pageable);

    /**
     * Count audit logs by action for a user
     */
    @Query("SELECT COUNT(ual) FROM UserAuditLog ual WHERE ual.userId = :userId AND ual.action = :action")
    long countByUserIdAndAction(@Param("userId") UUID userId, @Param("action") String action);

    /**
     * Count audit logs by action for a tenant
     */
    @Query("SELECT COUNT(ual) FROM UserAuditLog ual WHERE ual.tenantId = :tenantId AND ual.action = :action")
    long countByTenantIdAndAction(@Param("tenantId") UUID tenantId, @Param("action") String action);

    /**
     * Find recent login activities
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.tenantId = :tenantId AND ual.action = 'LOGIN' " +
           "AND ual.createdAt > :since ORDER BY ual.createdAt DESC")
    List<UserAuditLog> findRecentLogins(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);

    /**
     * Find suspicious activities (multiple failed logins, etc.)
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.tenantId = :tenantId " +
           "AND ual.severity IN ('HIGH', 'CRITICAL') AND ual.createdAt > :since ORDER BY ual.createdAt DESC")
    List<UserAuditLog> findSuspiciousActivities(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);

    /**
     * Delete old audit logs (for data retention)
     */
    @Modifying
    @Query("DELETE FROM UserAuditLog ual WHERE ual.createdAt < :cutoffDate")
    void deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find audit logs by IP address (for security analysis)
     */
    List<UserAuditLog> findByTenantIdAndIpAddressOrderByCreatedAtDesc(UUID tenantId, String ipAddress);

    /**
     * Get audit statistics by action
     */
    @Query("SELECT ual.action, COUNT(ual) FROM UserAuditLog ual WHERE ual.tenantId = :tenantId " +
           "AND ual.createdAt > :since GROUP BY ual.action ORDER BY COUNT(ual) DESC")
    List<Object[]> getAuditStatsByAction(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);

    /**
     * Get audit statistics by user
     */
    @Query("SELECT ual.userId, COUNT(ual) FROM UserAuditLog ual WHERE ual.tenantId = :tenantId " +
           "AND ual.createdAt > :since GROUP BY ual.userId ORDER BY COUNT(ual) DESC")
    List<Object[]> getAuditStatsByUser(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);
}