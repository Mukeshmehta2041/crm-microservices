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

    void deleteByTimestampBefore(LocalDateTime before);
}