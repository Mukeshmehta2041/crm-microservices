package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.OAuth2AuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuth2AuthorizationCodeRepository extends JpaRepository<OAuth2AuthorizationCode, UUID> {

    /**
     * Find authorization code by code value
     */
    Optional<OAuth2AuthorizationCode> findByCode(String code);

    /**
     * Find authorization code by code and client ID
     */
    Optional<OAuth2AuthorizationCode> findByCodeAndClientId(String code, String clientId);

    /**
     * Find valid (unused and not expired) authorization code
     */
    @Query("SELECT ac FROM OAuth2AuthorizationCode ac WHERE ac.code = :code AND ac.clientId = :clientId AND ac.used = false AND ac.expiresAt > :now")
    Optional<OAuth2AuthorizationCode> findValidCode(@Param("code") String code, 
                                                   @Param("clientId") String clientId, 
                                                   @Param("now") LocalDateTime now);

    /**
     * Delete expired authorization codes
     */
    @Modifying
    @Query("DELETE FROM OAuth2AuthorizationCode ac WHERE ac.expiresAt < :now")
    int deleteExpiredCodes(@Param("now") LocalDateTime now);

    /**
     * Delete used authorization codes older than specified time
     */
    @Modifying
    @Query("DELETE FROM OAuth2AuthorizationCode ac WHERE ac.used = true AND ac.usedAt < :cutoff")
    int deleteUsedCodesOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find authorization codes by user ID
     */
    @Query("SELECT ac FROM OAuth2AuthorizationCode ac WHERE ac.userId = :userId AND ac.tenantId = :tenantId")
    java.util.List<OAuth2AuthorizationCode> findByUserIdAndTenantId(@Param("userId") UUID userId, 
                                                                   @Param("tenantId") UUID tenantId);

    /**
     * Count active codes for a user and client
     */
    @Query("SELECT COUNT(ac) FROM OAuth2AuthorizationCode ac WHERE ac.userId = :userId AND ac.clientId = :clientId AND ac.used = false AND ac.expiresAt > :now")
    long countActiveCodesForUserAndClient(@Param("userId") UUID userId, 
                                        @Param("clientId") String clientId, 
                                        @Param("now") LocalDateTime now);
}