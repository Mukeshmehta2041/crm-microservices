package com.crm.platform.analytics.repository;

import com.crm.platform.analytics.entity.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    Page<Dashboard> findByOrganizationIdAndIsActiveTrue(String organizationId, Pageable pageable);

    Optional<Dashboard> findByIdAndOrganizationId(Long id, String organizationId);

    Optional<Dashboard> findByOrganizationIdAndIsActiveTrueAndIsDefaultTrue(String organizationId);

    @Query("SELECT d FROM Dashboard d WHERE d.organizationId = :organizationId AND d.isActive = true " +
           "AND (:createdBy IS NULL OR d.createdBy = :createdBy) " +
           "AND (:searchTerm IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Dashboard> findDashboardsWithFilters(
            @Param("organizationId") String organizationId,
            @Param("createdBy") String createdBy,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    boolean existsByNameAndOrganizationId(String name, String organizationId);

    List<Dashboard> findByCreatedByAndIsActiveTrue(String createdBy);
}