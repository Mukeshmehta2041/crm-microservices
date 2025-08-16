package com.crm.platform.pipelines.repository;

import com.crm.platform.pipelines.entity.Pipeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, UUID>, JpaSpecificationExecutor<Pipeline> {

    /**
     * Find all pipelines for a tenant
     */
    List<Pipeline> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);

    /**
     * Find active pipelines for a tenant
     */
    List<Pipeline> findByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID tenantId);

    /**
     * Find pipeline by tenant and name
     */
    Optional<Pipeline> findByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find default pipeline for a tenant
     */
    Optional<Pipeline> findByTenantIdAndIsDefaultTrue(UUID tenantId);

    /**
     * Find pipelines by template
     */
    List<Pipeline> findByTemplateId(UUID templateId);

    /**
     * Check if pipeline name exists for tenant
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Check if pipeline name exists for tenant excluding specific pipeline
     */
    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);

    /**
     * Find pipelines with stages count
     */
    @Query("SELECT p FROM Pipeline p LEFT JOIN FETCH p.stages WHERE p.tenantId = :tenantId ORDER BY p.displayOrder ASC")
    List<Pipeline> findByTenantIdWithStages(@Param("tenantId") UUID tenantId);

    /**
     * Find pipeline with stages and automation rules
     */
    @Query("SELECT p FROM Pipeline p " +
           "LEFT JOIN FETCH p.stages s " +
           "LEFT JOIN FETCH p.automationRules ar " +
           "WHERE p.id = :pipelineId AND p.tenantId = :tenantId")
    Optional<Pipeline> findByIdAndTenantIdWithStagesAndRules(@Param("pipelineId") UUID pipelineId, 
                                                           @Param("tenantId") UUID tenantId);

    /**
     * Search pipelines by name
     */
    @Query("SELECT p FROM Pipeline p WHERE p.tenantId = :tenantId AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY p.displayOrder ASC")
    Page<Pipeline> searchByName(@Param("tenantId") UUID tenantId, 
                               @Param("searchTerm") String searchTerm, 
                               Pageable pageable);

    /**
     * Count active pipelines for tenant
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Find pipelines created by user
     */
    List<Pipeline> findByTenantIdAndCreatedByOrderByCreatedAtDesc(UUID tenantId, UUID createdBy);

    /**
     * Find pipelines with analytics in date range
     */
    @Query("SELECT DISTINCT p FROM Pipeline p " +
           "LEFT JOIN FETCH p.analytics a " +
           "WHERE p.tenantId = :tenantId AND " +
           "a.periodStart >= :startDate AND a.periodEnd <= :endDate")
    List<Pipeline> findByTenantIdWithAnalyticsInDateRange(@Param("tenantId") UUID tenantId,
                                                         @Param("startDate") java.time.LocalDateTime startDate,
                                                         @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Get pipeline performance metrics
     */
    @Query("SELECT p.id, p.name, COUNT(s.id) as stageCount, " +
           "COUNT(ar.id) as automationRuleCount " +
           "FROM Pipeline p " +
           "LEFT JOIN p.stages s " +
           "LEFT JOIN p.automationRules ar " +
           "WHERE p.tenantId = :tenantId " +
           "GROUP BY p.id, p.name " +
           "ORDER BY p.displayOrder ASC")
    List<Object[]> getPipelineMetrics(@Param("tenantId") UUID tenantId);

    /**
     * Find templates (pipelines that can be used as templates)
     */
    @Query("SELECT p FROM Pipeline p WHERE p.tenantId = :tenantId AND " +
           "(p.templateId IS NULL OR p.id IN (SELECT DISTINCT p2.templateId FROM Pipeline p2 WHERE p2.tenantId = :tenantId)) " +
           "ORDER BY p.name ASC")
    List<Pipeline> findTemplates(@Param("tenantId") UUID tenantId);
}