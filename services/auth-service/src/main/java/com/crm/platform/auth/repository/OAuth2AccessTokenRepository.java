package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.OAuth2AccessToken;
import com.crm.platform.auth.entity.OAuth2Client;
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
public interface OAuth2AccessTokenRepository extends JpaRepository<OAuth2AccessToken, UUID> {

    /**
     * Find access token by token value
     */
    Optional<OAuth2AccessToken> findByAccessToken(String accessToken);

    /**
     * Find access token by refresh token
     */
    Optional<OAuth2AccessToken> findByRefreshToken(String refreshToken);

    /**
     * Find valid (not revoked and not expired) access token
     */
    @Query("SELECT at FROM OAuth2AccessToken at WHERE at.accessToken = :accessToken AND at.revoked = false AND at.expiresAt > :now")
    Optional<OAuth2AccessToken> findValidAccessToken(@Param("accessToken") String accessToken, 
                                                    @Param("now") LocalDateTime now);

    /**
     * Find valid refresh token
     */
    @Query("SELECT at FROM OAuth2AccessToken at WHERE at.refreshToken = :refreshToken AND at.revoked = false AND at.refreshExpiresAt > :now")
    Optional<OAuth2AccessToken> findValidRefreshToken(@Param("refreshToken") String refreshToken, 
                                                     @Param("now") LocalDateTime now);

    /**
     * Find all tokens for a user and client
     */
    @Query("SELECT at FROM OAuth2AccessToken at WHERE at.userId = :userId AND at.clientId = :clientId AND at.tenantId = :tenantId")
    List<OAuth2AccessToken> findByUserIdAndClientIdAndTenantId(@Param("userId") UUID userId, 
                                                              @Param("clientId") String clientId, 
                                                              @Param("tenantId") UUID tenantId);

    /**
     * Find all active tokens for a user
     */
    @Query("SELECT at FROM OAuth2AccessToken at WHERE at.userId = :userId AND at.tenantId = :tenantId AND at.revoked = false AND at.expiresAt > :now")
    List<OAuth2AccessToken> findActiveTokensByUserId(@Param("userId") UUID userId, 
                                                    @Param("tenantId") UUID tenantId, 
                                                    @Param("now") LocalDateTime now);

    /**
     * Find all tokens for a client (including client credentials tokens)
     */
    @Query("SELECT at FROM OAuth2AccessToken at WHERE at.clientId = :clientId AND at.tenantId = :tenantId")
    List<OAuth2AccessToken> findByClientIdAndTenantId(@Param("clientId") String clientId, 
                                                     @Param("tenantId") UUID tenantId);

    /**
     * Revoke all tokens for a user and client
     */
    @Modifying
    @Query("UPDATE OAuth2AccessToken at SET at.revoked = true, at.revokedAt = :now WHERE at.userId = :userId AND at.clientId = :clientId AND at.tenantId = :tenantId AND at.revoked = false")
    int revokeTokensForUserAndClient(@Param("userId") UUID userId, 
                                   @Param("clientId") String clientId, 
                                   @Param("tenantId") UUID tenantId, 
                                   @Param("now") LocalDateTime now);

    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE OAuth2AccessToken at SET at.revoked = true, at.revokedAt = :now WHERE at.userId = :userId AND at.tenantId = :tenantId AND at.revoked = false")
    int revokeAllTokensForUser(@Param("userId") UUID userId, 
                             @Param("tenantId") UUID tenantId, 
                             @Param("now") LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM OAuth2AccessToken at WHERE at.expiresAt < :now AND (at.refreshExpiresAt IS NULL OR at.refreshExpiresAt < :now)")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete revoked tokens older than specified time
     */
    @Modifying
    @Query("DELETE FROM OAuth2AccessToken at WHERE at.revoked = true AND at.revokedAt < :cutoff")
    int deleteRevokedTokensOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count active tokens for a user
     */
    @Query("SELECT COUNT(at) FROM OAuth2AccessToken at WHERE at.userId = :userId AND at.tenantId = :tenantId AND at.revoked = false AND at.expiresAt > :now")
    long countActiveTokensForUser(@Param("userId") UUID userId, 
                                @Param("tenantId") UUID tenantId, 
                                @Param("now") LocalDateTime now);

    /**
     * Find tokens by grant type
     */
    @Query("SELECT at FROM OAuth2AccessToken at WHERE at.grantType = :grantType AND at.tenantId = :tenantId")
    List<OAuth2AccessToken> findByGrantTypeAndTenantId(@Param("grantType") OAuth2Client.GrantType grantType, 
                                                      @Param("tenantId") UUID tenantId);
}