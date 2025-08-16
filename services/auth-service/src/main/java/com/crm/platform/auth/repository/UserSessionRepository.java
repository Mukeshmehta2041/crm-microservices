package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByTokenId(String tokenId);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByUserIdAndStatus(UUID userId, UserSession.SessionStatus status);

    List<UserSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.status = :status WHERE s.tokenId = :tokenId")
    void updateSessionStatus(@Param("tokenId") String tokenId, @Param("status") UserSession.SessionStatus status);

    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'EXPIRED' WHERE s.expiresAt < :now AND s.status = 'ACTIVE'")
    void expireOldSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'LOGGED_OUT' WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    void logoutAllUserSessions(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :accessTime WHERE s.tokenId = :tokenId")
    void updateLastAccessed(@Param("tokenId") String tokenId, @Param("accessTime") LocalDateTime accessTime);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    long countActiveSessionsByUser(@Param("userId") UUID userId);

    @Query("SELECT s FROM UserSession s WHERE s.refreshExpiresAt < :now")
    List<UserSession> findExpiredRefreshTokens(@Param("now") LocalDateTime now);

    void deleteByStatusAndCreatedAtBefore(UserSession.SessionStatus status, LocalDateTime before);

    /**
     * Find active sessions by user ID and tenant ID
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.tenantId = :tenantId AND s.status = 'ACTIVE'")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    /**
     * Count active sessions by tenant
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.tenantId = :tenantId AND s.status = 'ACTIVE'")
    long countActiveSessionsByTenant(@Param("tenantId") UUID tenantId);
}