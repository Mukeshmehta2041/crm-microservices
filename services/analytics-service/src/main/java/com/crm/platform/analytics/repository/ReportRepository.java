package com.crm.platform.analytics.repository;

import com.crm.platform.analytics.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByOrganizationIdAndIsActiveTrue(String organizationId, Pageable pageable);

    List<Report> findByOrganizationIdAndIsActiveTrueAndIsScheduledTrue(String organizationId);

    Optional<Report> findByIdAndOrganizationId(Long id, String organizationId);

    @Query("SELECT r FROM Report r WHERE r.organizationId = :organizationId AND r.isActive = true " +
           "AND (:reportType IS NULL OR r.reportType = :reportType) " +
           "AND (:createdBy IS NULL OR r.createdBy = :createdBy) " +
           "AND (:searchTerm IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Report> findReportsWithFilters(
            @Param("organizationId") String organizationId,
            @Param("reportType") String reportType,
            @Param("createdBy") String createdBy,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    boolean existsByNameAndOrganizationId(String name, String organizationId);
}