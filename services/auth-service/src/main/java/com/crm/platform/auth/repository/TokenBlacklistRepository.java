package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    /**
     * Check if a token is blacklisted by JTI
     */
    boolean existsByJti(String jti);

    /**
     * Find blacklisted token by JTI
     */
    TokenBlacklist findByJti(String jti);

    /**
     * Find all blacklisted tokens for a user
     */
    @Query("SELECT tb FROM TokenBlacklist tb WHERE tb.userId = :userId AND tb.tenantId = :tenantId")
    List<TokenBlacklist> findByUserIdAndTenantId(@Param("userId") UUID userId, 
                                                @Param("tenantId") UUID tenantId);

    /**
     * Find all blacklisted tokens for a user by token type
     */
    @Query("SELECT tb FROM TokenBlacklist tb WHERE tb.userId = :userId AND tb.tenantId = :tenantId AND tb.tokenType = :tokenType")
    List<TokenBlacklist> findByUserIdAndTenantIdAndTokenType(@Param("userId") UUID userId, 
                                                           @Param("tenantId") UUID tenantId,
                                                           @Param("tokenType") TokenBlacklist.TokenType tokenType);

    /**
     * Delete expired blacklisted tokens
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Count blacklisted tokens for a user
     */
    @Query("SELECT COUNT(tb) FROM TokenBlacklist tb WHERE tb.userId = :userId AND tb.tenantId = :tenantId")
    long countByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    /**
     * Find blacklisted tokens by reason
     */
    @Query("SELECT tb FROM TokenBlacklist tb WHERE tb.reason = :reason AND tb.tenantId = :tenantId")
    List<TokenBlacklist> findByReasonAndTenantId(@Param("reason") String reason, 
                                               @Param("tenantId") UUID tenantId);

    /**
     * Find blacklisted tokens that will expire soon (for cleanup warning)
     */
    @Query("SELECT tb FROM TokenBlacklist tb WHERE tb.expiresAt BETWEEN :now AND :threshold")
    List<TokenBlacklist> findTokensExpiringBetween(@Param("now") LocalDateTime now, 
                                                  @Param("threshold") LocalDateTime threshold);
}