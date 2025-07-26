package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") UUID userId, @Param("attempts") Integer attempts);

    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") UUID userId, @Param("lockedUntil") LocalDateTime lockedUntil);

    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = null, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockAccount(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLoginTime(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.passwordChangedAt = :changedAt WHERE u.id = :userId")
    void updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash, 
                       @Param("changedAt") LocalDateTime changedAt);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE'")
    long countActiveUsersByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil < :now")
    java.util.List<User> findExpiredLockedAccounts(@Param("now") LocalDateTime now);
}