package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.EmailVerificationToken;
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
 * Repository for email verification token operations
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Find a valid (unverified and not expired) email verification token
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.token = :token " +
           "AND evt.isVerified = false AND evt.expiresAt > :now AND evt.attempts < evt.maxAttempts")
    Optional<EmailVerificationToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Find email verification token by token value
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Find all email verification tokens for a user
     */
    List<EmailVerificationToken> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find email verification tokens by email
     */
    List<EmailVerificationToken> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Find active (unverified and not expired) tokens for a user
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.userId = :userId " +
           "AND evt.isVerified = false AND evt.expiresAt > :now AND evt.attempts < evt.maxAttempts " +
           "ORDER BY evt.createdAt DESC")
    List<EmailVerificationToken> findActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find active tokens for an email address
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.email = :email " +
           "AND evt.isVerified = false AND evt.expiresAt > :now AND evt.attempts < evt.maxAttempts " +
           "ORDER BY evt.createdAt DESC")
    List<EmailVerificationToken> findActiveTokensByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Count active tokens for a user within a time period
     */
    @Query("SELECT COUNT(evt) FROM EmailVerificationToken evt WHERE evt.userId = :userId " +
           "AND evt.isVerified = false AND evt.expiresAt > :now AND evt.createdAt > :since")
    long countActiveTokensByUserIdSince(@Param("userId") UUID userId, @Param("now") LocalDateTime now, 
                                       @Param("since") LocalDateTime since);

    /**
     * Count active tokens for an email within a time period
     */
    @Query("SELECT COUNT(evt) FROM EmailVerificationToken evt WHERE evt.email = :email " +
           "AND evt.isVerified = false AND evt.expiresAt > :now AND evt.createdAt > :since")
    long countActiveTokensByEmailSince(@Param("email") String email, @Param("now") LocalDateTime now, 
                                      @Param("since") LocalDateTime since);

    /**
     * Mark all active tokens for a user as verified (used when email is verified)
     */
    @Modifying
    @Query("UPDATE EmailVerificationToken evt SET evt.isVerified = true, evt.verifiedAt = :now " +
           "WHERE evt.userId = :userId AND evt.isVerified = false AND evt.expiresAt > :now")
    void markAllUserTokensAsVerified(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Mark all active tokens for an email as verified
     */
    @Modifying
    @Query("UPDATE EmailVerificationToken evt SET evt.isVerified = true, evt.verifiedAt = :now " +
           "WHERE evt.email = :email AND evt.isVerified = false AND evt.expiresAt > :now")
    void markAllEmailTokensAsVerified(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete tokens older than specified date
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.createdAt < :cutoffDate")
    void deleteTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find tokens by verification type
     */
    List<EmailVerificationToken> findByVerificationTypeAndUserIdOrderByCreatedAtDesc(
        EmailVerificationToken.VerificationType verificationType, UUID userId);

    /**
     * Find the most recent token for a user and verification type
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.userId = :userId " +
           "AND evt.verificationType = :verificationType ORDER BY evt.createdAt DESC LIMIT 1")
    Optional<EmailVerificationToken> findMostRecentTokenByUserIdAndType(
        @Param("userId") UUID userId, 
        @Param("verificationType") EmailVerificationToken.VerificationType verificationType);
}