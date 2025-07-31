package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.OAuth2AccountLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuth2AccountLink entity
 */
@Repository
public interface OAuth2AccountLinkRepository extends JpaRepository<OAuth2AccountLink, UUID> {

    /**
     * Find account link by provider and provider user ID
     */
    Optional<OAuth2AccountLink> findByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Find account link by user ID and provider
     */
    Optional<OAuth2AccountLink> findByUserIdAndProvider(UUID userId, String provider);

    /**
     * Find all account links for a user
     */
    List<OAuth2AccountLink> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Count linked accounts for a user excluding a specific provider
     */
    long countByUserIdAndProviderNot(UUID userId, String provider);

    /**
     * Find all active account links for a user
     */
    @Query("SELECT oal FROM OAuth2AccountLink oal WHERE oal.userId = :userId AND oal.isActive = true")
    List<OAuth2AccountLink> findActiveAccountLinksByUserId(@Param("userId") UUID userId);

    /**
     * Check if user has any OAuth2 accounts linked
     */
    boolean existsByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find account links by provider email
     */
    List<OAuth2AccountLink> findByProviderEmailAndIsActiveTrue(String providerEmail);
}