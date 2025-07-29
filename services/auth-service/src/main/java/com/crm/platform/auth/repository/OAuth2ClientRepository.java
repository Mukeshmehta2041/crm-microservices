package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.OAuth2Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, String> {

    /**
     * Find client by client ID and ensure it's active
     */
    @Query("SELECT c FROM OAuth2Client c WHERE c.clientId = :clientId AND c.isActive = true")
    Optional<OAuth2Client> findActiveByClientId(@Param("clientId") String clientId);

    /**
     * Find client by client ID and tenant ID
     */
    Optional<OAuth2Client> findByClientIdAndTenantId(String clientId, UUID tenantId);

    /**
     * Find all active clients for a tenant
     */
    @Query("SELECT c FROM OAuth2Client c WHERE c.tenantId = :tenantId AND c.isActive = true")
    List<OAuth2Client> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find all clients for a tenant (including inactive)
     */
    List<OAuth2Client> findByTenantId(UUID tenantId);

    /**
     * Check if client exists and is active
     */
    @Query("SELECT COUNT(c) > 0 FROM OAuth2Client c WHERE c.clientId = :clientId AND c.isActive = true")
    boolean existsActiveByClientId(@Param("clientId") String clientId);

    /**
     * Find clients by name pattern for a tenant
     */
    @Query("SELECT c FROM OAuth2Client c WHERE c.tenantId = :tenantId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<OAuth2Client> findByTenantIdAndNameContainingIgnoreCase(@Param("tenantId") UUID tenantId, 
                                                                @Param("name") String name);
}