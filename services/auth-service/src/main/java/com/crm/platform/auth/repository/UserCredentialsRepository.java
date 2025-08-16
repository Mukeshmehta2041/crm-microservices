package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {

    Optional<UserCredentials> findByUsername(String username);

    Optional<UserCredentials> findByEmail(String email);

    Optional<UserCredentials> findByUsernameOrEmail(String username, String email);

    Optional<UserCredentials> findByUsernameAndTenantId(String username, UUID tenantId);

    Optional<UserCredentials> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<UserCredentials> findByUserId(UUID userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    @Modifying
    @Query("UPDATE UserCredentials u SET u.failedLoginAttempts = :attempts WHERE u.id = :credentialsId")
    void updateFailedLoginAttempts(@Param("credentialsId") UUID credentialsId, @Param("attempts") Integer attempts);

    @Modifying
    @Query("UPDATE UserCredentials u SET u.accountLockedUntil = :lockedUntil WHERE u.id = :credentialsId")
    void lockAccount(@Param("credentialsId") UUID credentialsId, @Param("lockedUntil") LocalDateTime lockedUntil);

    @Modifying
    @Query("UPDATE UserCredentials u SET u.accountLockedUntil = null, u.failedLoginAttempts = 0 WHERE u.id = :credentialsId")
    void unlockAccount(@Param("credentialsId") UUID credentialsId);

    @Modifying
    @Query("UPDATE UserCredentials u SET u.lastLoginAt = :loginTime WHERE u.id = :credentialsId")
    void updateLastLoginTime(@Param("credentialsId") UUID credentialsId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE UserCredentials u SET u.passwordHash = :passwordHash, u.passwordChangedAt = :changedAt WHERE u.id = :credentialsId")
    void updatePassword(@Param("credentialsId") UUID credentialsId, @Param("passwordHash") String passwordHash, 
                       @Param("changedAt") LocalDateTime changedAt);

    @Query("SELECT COUNT(u) FROM UserCredentials u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE'")
    long countActiveUsersByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM UserCredentials u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil < :now")
    java.util.List<UserCredentials> findExpiredLockedAccounts(@Param("now") LocalDateTime now);
}