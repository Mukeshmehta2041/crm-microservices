package com.programming.auth.repository;

import com.programming.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByDomain(String domain);

    Optional<Tenant> findByDomainAndIsActiveTrue(String domain);

    List<Tenant> findByIsActiveTrue();

    boolean existsByDomain(String domain);

    @Query("SELECT t FROM Tenant t WHERE t.name ILIKE %:name% AND t.isActive = true")
    List<Tenant> findByNameContainingIgnoreCaseAndIsActiveTrue(@Param("name") String name);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.isActive = true")
    long countActiveTenants();
}