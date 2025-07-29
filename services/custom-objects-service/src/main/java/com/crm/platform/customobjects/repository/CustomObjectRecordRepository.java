package com.crm.platform.customobjects.repository;

import com.crm.platform.customobjects.entity.CustomObject;
import com.crm.platform.customobjects.entity.CustomObjectRecord;
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
 * Repository interface for CustomObjectRecord entity
 */
@Repository
public interface CustomObjectRecordRepository extends JpaRepository<CustomObjectRecord, UUID>, 
                                                     JpaSpecificationExecutor<CustomObjectRecord> {

    /**
     * Find custom object record by tenant ID and ID
     */
    Optional<CustomObjectRecord> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Find all records by tenant ID and custom object
     */
    List<CustomObjectRecord> findByTenantIdAndCustomObjectOrderByCreatedAtDesc(UUID tenantId, CustomObject customObject);

    /**
     * Find all active records by tenant ID and custom object
     */
    List<CustomObjectRecord> findByTenantIdAndCustomObjectAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, CustomObject customObject);

    /**
     * Find records by tenant ID and custom object with pagination
     */
    Page<CustomObjectRecord> findByTenantIdAndCustomObject(UUID tenantId, CustomObject customObject, Pageable pageable);

    /**
     * Find active records by tenant ID and custom object with pagination
     */
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndIsActiveTrue(UUID tenantId, CustomObject customObject, Pageable pageable);

    /**
     * Find records by tenant ID and owner
     */
    Page<CustomObjectRecord> findByTenantIdAndOwnerId(UUID tenantId, UUID ownerId, Pageable pageable);

    /**
     * Find active records by tenant ID and owner
     */
    Page<CustomObjectRecord> findByTenantIdAndOwnerIdAndIsActiveTrue(UUID tenantId, UUID ownerId, Pageable pageable);

    /**
     * Find records by tenant ID, custom object, and owner
     */
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndOwnerId(UUID tenantId, CustomObject customObject, UUID ownerId, Pageable pageable);

    /**
     * Find active records by tenant ID, custom object, and owner
     */
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndOwnerIdAndIsActiveTrue(UUID tenantId, CustomObject customObject, UUID ownerId, Pageable pageable);

    /**
     * Count records by tenant ID and custom object
     */
    long countByTenantIdAndCustomObject(UUID tenantId, CustomObject customObject);

    /**
     * Count active records by tenant ID and custom object
     */
    long countByTenantIdAndCustomObjectAndIsActiveTrue(UUID tenantId, CustomObject customObject);

    /**
     * Count records by tenant ID and owner
     */
    long countByTenantIdAndOwnerId(UUID tenantId, UUID ownerId);

    /**
     * Count active records by tenant ID and owner
     */
    long countByTenantIdAndOwnerIdAndIsActiveTrue(UUID tenantId, UUID ownerId);

    /**
     * Find records by tenant ID, custom object, and search term in record name
     */
    @Query("SELECT cor FROM CustomObjectRecord cor WHERE cor.tenantId = :tenantId " +
           "AND cor.customObject = :customObject " +
           "AND LOWER(cor.recordName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY cor.createdAt DESC")
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndRecordNameContaining(
            @Param("tenantId") UUID tenantId,
            @Param("customObject") CustomObject customObject,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    /**
     * Find records by tenant ID, custom object, and field value using JSONB queries
     */
    @Query(value = "SELECT * FROM custom_object_records cor " +
                   "WHERE cor.tenant_id = :tenantId " +
                   "AND cor.custom_object_id = :customObjectId " +
                   "AND cor.field_values ->> :fieldName = :fieldValue " +
                   "ORDER BY cor.created_at DESC",
           nativeQuery = true)
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndFieldValue(
            @Param("tenantId") UUID tenantId,
            @Param("customObjectId") UUID customObjectId,
            @Param("fieldName") String fieldName,
            @Param("fieldValue") String fieldValue,
            Pageable pageable);

    /**
     * Find records by tenant ID, custom object, and field value contains (for text search)
     */
    @Query(value = "SELECT * FROM custom_object_records cor " +
                   "WHERE cor.tenant_id = :tenantId " +
                   "AND cor.custom_object_id = :customObjectId " +
                   "AND LOWER(cor.field_values ->> :fieldName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                   "ORDER BY cor.created_at DESC",
           nativeQuery = true)
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndFieldValueContaining(
            @Param("tenantId") UUID tenantId,
            @Param("customObjectId") UUID customObjectId,
            @Param("fieldName") String fieldName,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    /**
     * Check if field value is unique for custom object (excluding specific record)
     */
    @Query(value = "SELECT COUNT(*) FROM custom_object_records cor " +
                   "WHERE cor.tenant_id = :tenantId " +
                   "AND cor.custom_object_id = :customObjectId " +
                   "AND cor.field_values ->> :fieldName = :fieldValue " +
                   "AND (:recordId IS NULL OR cor.id != :recordId) " +
                   "AND cor.is_active = true",
           nativeQuery = true)
    long countByTenantIdAndCustomObjectAndFieldValueAndIdNot(
            @Param("tenantId") UUID tenantId,
            @Param("customObjectId") UUID customObjectId,
            @Param("fieldName") String fieldName,
            @Param("fieldValue") String fieldValue,
            @Param("recordId") UUID recordId);

    /**
     * Find records with complex JSONB field queries
     */
    @Query(value = "SELECT * FROM custom_object_records cor " +
                   "WHERE cor.tenant_id = :tenantId " +
                   "AND cor.custom_object_id = :customObjectId " +
                   "AND cor.field_values @> :fieldQuery::jsonb " +
                   "ORDER BY cor.created_at DESC",
           nativeQuery = true)
    Page<CustomObjectRecord> findByTenantIdAndCustomObjectAndFieldQuery(
            @Param("tenantId") UUID tenantId,
            @Param("customObjectId") UUID customObjectId,
            @Param("fieldQuery") String fieldQuery,
            Pageable pageable);

    /**
     * Delete records by tenant ID (for tenant cleanup)
     */
    void deleteByTenantId(UUID tenantId);

    /**
     * Delete records by custom object
     */
    void deleteByCustomObject(CustomObject customObject);

    /**
     * Soft delete records by setting isActive to false
     */
    @Query("UPDATE CustomObjectRecord cor SET cor.isActive = false, cor.updatedBy = :updatedBy " +
           "WHERE cor.tenantId = :tenantId AND cor.customObject = :customObject")
    void softDeleteByTenantIdAndCustomObject(@Param("tenantId") UUID tenantId,
                                           @Param("customObject") CustomObject customObject,
                                           @Param("updatedBy") UUID updatedBy);
}