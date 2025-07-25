package com.programming.auth.repository;

import com.programming.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionId(String sessionId);

    Optional<UserSession> findBySessionIdAndIsActiveTrue(String sessionId);

    List<UserSession> findByUserIdAndIsActiveTrue(UUID userId);

    Page<UserSession> findByUserIdAndIsActiveTrue(UUID userId, Pageable pageable);

    List<UserSession> findByTenantIdAndIsActiveTrue(UUID tenantId);

    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.isActive = true " +
           "ORDER BY us.lastActivityAt DESC")
    List<UserSession> findActiveSessionsByUserIdOrderByLastActivity(@Param("userId") UUID userId);

    @Query("SELECT us FROM UserSession us WHERE us.expiresAt < :now")
    List<UserSession> findExpiredSessions(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.expiresAt < :now")
    int deactivateExpiredSessions(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.userId = :userId AND us.id != :currentSessionId")
    int deactivateOtherUserSessions(@Param("userId") UUID userId, @Param("currentSessionId") UUID currentSessionId);

    @Modifying
    @Query("UPDATE UserSession us SET us.lastActivityAt = :now WHERE us.sessionId = :sessionId")
    int updateLastActivity(@Param("sessionId") String sessionId, @Param("now") Instant now);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.userId = :userId AND us.isActive = true")
    long countActiveSessionsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.tenantId = :tenantId AND us.isActive = true")
    long countActiveSessionsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.lastActivityAt < :cutoff AND us.isActive = true")
    List<UserSession> findInactiveSessionsByUser(@Param("userId") UUID userId, @Param("cutoff") Instant cutoff);
}