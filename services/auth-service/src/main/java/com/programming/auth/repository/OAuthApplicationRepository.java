package com.programming.auth.repository;

import com.programming.auth.entity.OAuthApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthApplicationRepository extends JpaRepository<OAuthApplication, UUID> {

    Optional<OAuthApplication> findByClientId(String clientId);

    Optional<OAuthApplication> findByClientIdAndIsActiveTrue(String clientId);

    List<OAuthApplication> findByTenantIdAndIsActiveTrue(UUID tenantId);

    Page<OAuthApplication> findByTenantId(UUID tenantId, Pageable pageable);

    Page<OAuthApplication> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    boolean existsByClientId(String clientId);

    @Query("SELECT oa FROM OAuthApplication oa WHERE oa.tenantId = :tenantId AND " +
           "oa.name ILIKE %:name% AND oa.isActive = true")
    List<OAuthApplication> findByTenantIdAndNameContainingIgnoreCaseAndIsActiveTrue(
            @Param("tenantId") UUID tenantId, 
            @Param("name") String name);

    @Query("SELECT oa FROM OAuthApplication oa WHERE oa.tenantId = :tenantId AND " +
           "oa.applicationType = :applicationType AND oa.isActive = true")
    List<OAuthApplication> findByTenantIdAndApplicationTypeAndIsActiveTrue(
            @Param("tenantId") UUID tenantId, 
            @Param("applicationType") OAuthApplication.ApplicationType applicationType);

    @Query("SELECT COUNT(oa) FROM OAuthApplication oa WHERE oa.tenantId = :tenantId AND oa.isActive = true")
    long countByTenantIdAndIsActiveTrue(@Param("tenantId") UUID tenantId);

    @Query("SELECT oa FROM OAuthApplication oa JOIN FETCH oa.tenant WHERE oa.clientId = :clientId")
    Optional<OAuthApplication> findByClientIdWithTenant(@Param("clientId") String clientId);
}