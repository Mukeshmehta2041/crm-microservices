package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.PasswordResetToken;
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
 * Repository for password reset token operations
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find a valid (unused and not expired) password reset token
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token " +
           "AND prt.isUsed = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Find password reset token by token value
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find all password reset tokens for a user
     */
    List<PasswordResetToken> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find active (unused and not expired) tokens for a user
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.userId = :userId " +
           "AND prt.isUsed = false AND prt.expiresAt > :now ORDER BY prt.createdAt DESC")
    List<PasswordResetToken> findActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Count active tokens for a user
     */
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.userId = :userId " +
           "AND prt.isUsed = false AND prt.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Mark all active tokens for a user as used
     */
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.isUsed = true, prt.usedAt = :now " +
           "WHERE prt.userId = :userId AND prt.isUsed = false AND prt.expiresAt > :now")
    void markAllUserTokensAsUsed(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete tokens older than specified date
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.createdAt < :cutoffDate")
    void deleteTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}