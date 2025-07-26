package com.crm.platform.tenant.repository;

import com.crm.platform.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySubdomain(String subdomain);

    Optional<Tenant> findByCustomDomain(String customDomain);

    boolean existsBySubdomain(String subdomain);

    boolean existsByCustomDomain(String customDomain);

    List<Tenant> findByStatus(Tenant.TenantStatus status);

    List<Tenant> findByPlanType(Tenant.PlanType planType);

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE' AND t.subscriptionExpiresAt < :now")
    List<Tenant> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE' AND t.isTrial = true AND t.trialEndsAt < :now")
    List<Tenant> findExpiredTrials(@Param("now") LocalDateTime now);

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE' AND " +
           "(t.subscriptionExpiresAt BETWEEN :start AND :end OR t.trialEndsAt BETWEEN :start AND :end)")
    List<Tenant> findExpiringSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'ACTIVE'")
    long countActiveTenants();

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.planType = :planType AND t.status = 'ACTIVE'")
    long countByPlanTypeAndActive(@Param("planType") Tenant.PlanType planType);

    @Query("SELECT t FROM Tenant t WHERE t.name ILIKE %:searchTerm% OR t.subdomain ILIKE %:searchTerm%")
    List<Tenant> searchByNameOrSubdomain(@Param("searchTerm") String searchTerm);
}