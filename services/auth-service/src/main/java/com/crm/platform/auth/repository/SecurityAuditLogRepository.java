package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.SecurityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {

    Page<SecurityAuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    Page<SecurityAuditLog> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);

    List<SecurityAuditLog> findByEventTypeAndTimestampBetween(String eventType, 
                                                             LocalDateTime start, 
                                                             LocalDateTime end);

    @Query("SELECT COUNT(a) FROM SecurityAuditLog a WHERE a.userId = :userId AND a.eventType = :eventType " +
           "AND a.timestamp > :since")
    long countByUserIdAndEventTypeAndTimestampAfter(@Param("userId") UUID userId, 
                                                   @Param("eventType") String eventType,
                                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM SecurityAuditLog a WHERE a.ipAddress = :ipAddress AND a.eventType = :eventType " +
           "AND a.timestamp > :since")
    long countByIpAddressAndEventTypeAndTimestampAfter(@Param("ipAddress") String ipAddress,
                                                      @Param("eventType") String eventType,
                                                      @Param("since") LocalDateTime since);

    List<SecurityAuditLog> findByIpAddressAndEventTypeAndTimestampAfter(String ipAddress, 
                                                                       String eventType, 
                                                                       LocalDateTime since);

    List<SecurityAuditLog> findByEventTypeAndTimestampAfter(String eventType, LocalDateTime since);

    @Query("SELECT DISTINCT a.userId FROM SecurityAuditLog a WHERE a.eventType = :eventType " +
           "AND a.timestamp BETWEEN :start AND :end AND a.userId IS NOT NULL")
    List<UUID> findDistinctUserIdsByEventTypeAndTimestampBetween(@Param("eventType") String eventType,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT a.ipAddress FROM SecurityAuditLog a WHERE a.eventType = :eventType " +
           "AND a.timestamp BETWEEN :start AND :end AND a.ipAddress IS NOT NULL")
    List<String> findDistinctIpAddressesByEventTypeAndTimestampBetween(@Param("eventType") String eventType,
                                                                      @Param("start") LocalDateTime start,
                                                                      @Param("end") LocalDateTime end);

    @Query("SELECT a.eventDescription, COUNT(a) FROM SecurityAuditLog a WHERE a.eventType = :eventType " +
           "AND a.timestamp BETWEEN :start AND :end GROUP BY a.eventDescription ORDER BY COUNT(a) DESC")
    List<Object[]> findEventDescriptionCountsByEventTypeAndTimestampBetween(@Param("eventType") String eventType,
                                                                           @Param("start") LocalDateTime start,
                                                                           @Param("end") LocalDateTime end);

    @Query("SELECT HOUR(a.timestamp), COUNT(a) FROM SecurityAuditLog a WHERE a.eventType = :eventType " +
           "AND a.timestamp BETWEEN :start AND :end GROUP BY HOUR(a.timestamp)")
    List<Object[]> findHourlyCountsByEventTypeAndTimestampBetween(@Param("eventType") String eventType,
                                                                 @Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM SecurityAuditLog a WHERE a.timestamp BETWEEN :start AND :end " +
           "AND (:tenantId IS NULL OR a.tenantId = :tenantId)")
    long countByTimestampBetweenAndTenantId(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM SecurityAuditLog a WHERE a.eventType IN :eventTypes " +
           "AND a.timestamp BETWEEN :start AND :end " +
           "AND (:tenantId IS NULL OR a.tenantId = :tenantId) " +
           "ORDER BY a.timestamp DESC")
    Page<SecurityAuditLog> findByEventTypesAndTimestampBetweenAndTenantId(@Param("eventTypes") List<String> eventTypes,
                                                                         @Param("start") LocalDateTime start,
                                                                         @Param("end") LocalDateTime end,
                                                                         @Param("tenantId") UUID tenantId,
                                                                         Pageable pageable);

    void deleteByTimestampBefore(LocalDateTime before);

    // Tenant-specific queries

    Page<SecurityAuditLog> findByTenantIdAndEventTypeAndTimestampBetween(UUID tenantId, String eventType, 
                                                                        LocalDateTime start, LocalDateTime end, 
                                                                        Pageable pageable);

    Page<SecurityAuditLog> findByTenantIdAndTimestampBetween(UUID tenantId, LocalDateTime start, 
                                                           LocalDateTime end, Pageable pageable);

    Page<SecurityAuditLog> findByTenantIdAndEventType(UUID tenantId, String eventType, Pageable pageable);

    Page<SecurityAuditLog> findByTenantIdAndUserIdOrderByTimestampDesc(UUID tenantId, UUID userId, Pageable pageable);

    List<SecurityAuditLog> findByTenantIdAndTimestampBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);

    List<SecurityAuditLog> findByTenantIdAndEventTypeInAndTimestampBetween(UUID tenantId, List<String> eventTypes,
                                                                          LocalDateTime start, LocalDateTime end);

    List<SecurityAuditLog> findByTenantIdAndTimestampBefore(UUID tenantId, LocalDateTime before);
}