package com.crm.platform.customobjects.repository;

import com.crm.platform.customobjects.entity.CustomObject;
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

/**
 * Repository interface for CustomObject entity
 */
@Repository
public interface CustomObjectRepository extends JpaRepository<CustomObject, UUID>, 
                                               JpaSpecificationExecutor<CustomObject> {

    /**
     * Find custom object by tenant ID and ID
     */
    Optional<CustomObject> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Find custom object by tenant ID and name
     */
    Optional<CustomObject> findByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find custom object by tenant ID and API name
     */
    Optional<CustomObject> findByTenantIdAndApiName(UUID tenantId, String apiName);

    /**
     * Find all custom objects by tenant ID
     */
    List<CustomObject> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Find all active custom objects by tenant ID
     */
    List<CustomObject> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Find custom objects by tenant ID with pagination
     */
    Page<CustomObject> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find active custom objects by tenant ID with pagination
     */
    Page<CustomObject> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    /**
     * Check if custom object name exists for tenant
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Check if custom object API name exists for tenant
     */
    boolean existsByTenantIdAndApiName(UUID tenantId, String apiName);

    /**
     * Check if custom object name exists for tenant excluding specific ID
     */
    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);

    /**
     * Check if custom object API name exists for tenant excluding specific ID
     */
    boolean existsByTenantIdAndApiNameAndIdNot(UUID tenantId, String apiName, UUID id);

    /**
     * Find custom objects by tenant ID and search term
     */
    @Query("SELECT co FROM CustomObject co WHERE co.tenantId = :tenantId " +
           "AND (LOWER(co.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(co.label) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(co.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY co.createdAt DESC")
    Page<CustomObject> findByTenantIdAndSearchTerm(@Param("tenantId") UUID tenantId,
                                                   @Param("searchTerm") String searchTerm,
                                                   Pageable pageable);

    /**
     * Count custom objects by tenant ID
     */
    long countByTenantId(UUID tenantId);

    /**
     * Count active custom objects by tenant ID
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Find custom objects that allow reports
     */
    List<CustomObject> findByTenantIdAndAllowReportsTrueAndIsActiveTrueOrderByName(UUID tenantId);

    /**
     * Find custom objects that allow activities
     */
    List<CustomObject> findByTenantIdAndAllowActivitiesTrueAndIsActiveTrueOrderByName(UUID tenantId);

    /**
     * Delete custom objects by tenant ID (for tenant cleanup)
     */
    void deleteByTenantId(UUID tenantId);
}