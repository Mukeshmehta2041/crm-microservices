package com.crm.platform.workflow.repository;

import com.crm.platform.workflow.entity.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WorkflowDefinition entities
 */
@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    /**
     * Find workflow definitions by tenant ID
     */
    Page<WorkflowDefinition> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find active workflow definitions by tenant ID
     */
    Page<WorkflowDefinition> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    /**
     * Find published workflow definitions by tenant ID
     */
    Page<WorkflowDefinition> findByTenantIdAndIsPublishedTrue(UUID tenantId, Pageable pageable);

    /**
     * Find workflow definitions by tenant ID and category
     */
    Page<WorkflowDefinition> findByTenantIdAndCategory(UUID tenantId, String category, Pageable pageable);

    /**
     * Find workflow definition by tenant ID, name and version
     */
    Optional<WorkflowDefinition> findByTenantIdAndNameAndVersion(UUID tenantId, String name, Integer version);

    /**
     * Find latest version of workflow definition by tenant ID and name
     */
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.name = :name " +
           "ORDER BY w.version DESC")
    List<WorkflowDefinition> findLatestVersionByTenantIdAndName(@Param("tenantId") UUID tenantId, 
                                                               @Param("name") String name, 
                                                               Pageable pageable);

    /**
     * Find all versions of workflow definition by tenant ID and name
     */
    List<WorkflowDefinition> findByTenantIdAndNameOrderByVersionDesc(UUID tenantId, String name);

    /**
     * Check if workflow definition exists by tenant ID and name
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find workflow definitions by tenant ID and name containing (case insensitive)
     */
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId " +
           "AND LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<WorkflowDefinition> findByTenantIdAndNameContainingIgnoreCase(@Param("tenantId") UUID tenantId,
                                                                       @Param("name") String name,
                                                                       Pageable pageable);

    /**
     * Count workflow definitions by tenant ID
     */
    long countByTenantId(UUID tenantId);

    /**
     * Count active workflow definitions by tenant ID
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Count published workflow definitions by tenant ID
     */
    long countByTenantIdAndIsPublishedTrue(UUID tenantId);

    /**
     * Find workflow definitions created by user
     */
    Page<WorkflowDefinition> findByTenantIdAndCreatedBy(UUID tenantId, UUID createdBy, Pageable pageable);

    /**
     * Find workflow definitions by multiple categories
     */
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId " +
           "AND w.category IN :categories")
    Page<WorkflowDefinition> findByTenantIdAndCategoryIn(@Param("tenantId") UUID tenantId,
                                                         @Param("categories") List<String> categories,
                                                         Pageable pageable);
}