package com.crm.platform.customobjects.repository;

import com.crm.platform.customobjects.entity.CustomField;
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
 * Repository interface for CustomField entity
 */
@Repository
public interface CustomFieldRepository extends JpaRepository<CustomField, UUID>, 
                                              JpaSpecificationExecutor<CustomField> {

    /**
     * Find custom field by tenant ID and ID
     */
    Optional<CustomField> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Find custom field by tenant ID, object type, and field name
     */
    Optional<CustomField> findByTenantIdAndObjectTypeAndFieldName(UUID tenantId, String objectType, String fieldName);

    /**
     * Find custom field by tenant ID, custom object, and field name
     */
    Optional<CustomField> findByTenantIdAndCustomObjectAndFieldName(UUID tenantId, CustomObject customObject, String fieldName);

    /**
     * Find all custom fields by tenant ID and object type
     */
    List<CustomField> findByTenantIdAndObjectTypeOrderByFieldOrderAsc(UUID tenantId, String objectType);

    /**
     * Find all active custom fields by tenant ID and object type
     */
    List<CustomField> findByTenantIdAndObjectTypeAndIsActiveTrueOrderByFieldOrderAsc(UUID tenantId, String objectType);

    /**
     * Find all custom fields by custom object
     */
    List<CustomField> findByCustomObjectOrderByFieldOrderAsc(CustomObject customObject);

    /**
     * Find all active custom fields by custom object
     */
    List<CustomField> findByCustomObjectAndIsActiveTrueOrderByFieldOrderAsc(CustomObject customObject);

    /**
     * Find custom fields by tenant ID with pagination
     */
    Page<CustomField> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find custom fields by tenant ID and object type with pagination
     */
    Page<CustomField> findByTenantIdAndObjectType(UUID tenantId, String objectType, Pageable pageable);

    /**
     * Find custom fields by custom object with pagination
     */
    Page<CustomField> findByCustomObject(CustomObject customObject, Pageable pageable);

    /**
     * Check if field name exists for tenant and object type
     */
    boolean existsByTenantIdAndObjectTypeAndFieldName(UUID tenantId, String objectType, String fieldName);

    /**
     * Check if field name exists for custom object
     */
    boolean existsByCustomObjectAndFieldName(CustomObject customObject, String fieldName);

    /**
     * Check if field name exists for tenant and object type excluding specific ID
     */
    boolean existsByTenantIdAndObjectTypeAndFieldNameAndIdNot(UUID tenantId, String objectType, String fieldName, UUID id);

    /**
     * Check if field name exists for custom object excluding specific ID
     */
    boolean existsByCustomObjectAndFieldNameAndIdNot(CustomObject customObject, String fieldName, UUID id);

    /**
     * Find required fields by tenant ID and object type
     */
    List<CustomField> findByTenantIdAndObjectTypeAndIsRequiredTrueAndIsActiveTrueOrderByFieldOrderAsc(UUID tenantId, String objectType);

    /**
     * Find required fields by custom object
     */
    List<CustomField> findByCustomObjectAndIsRequiredTrueAndIsActiveTrueOrderByFieldOrderAsc(CustomObject customObject);

    /**
     * Find unique fields by tenant ID and object type
     */
    List<CustomField> findByTenantIdAndObjectTypeAndIsUniqueTrueAndIsActiveTrueOrderByFieldOrderAsc(UUID tenantId, String objectType);

    /**
     * Find unique fields by custom object
     */
    List<CustomField> findByCustomObjectAndIsUniqueTrueAndIsActiveTrueOrderByFieldOrderAsc(CustomObject customObject);

    /**
     * Find indexed fields by tenant ID and object type
     */
    List<CustomField> findByTenantIdAndObjectTypeAndIsIndexedTrueAndIsActiveTrueOrderByFieldOrderAsc(UUID tenantId, String objectType);

    /**
     * Find indexed fields by custom object
     */
    List<CustomField> findByCustomObjectAndIsIndexedTrueAndIsActiveTrueOrderByFieldOrderAsc(CustomObject customObject);

    /**
     * Find custom fields by field type
     */
    List<CustomField> findByTenantIdAndFieldTypeAndIsActiveTrueOrderByFieldOrderAsc(UUID tenantId, CustomField.FieldType fieldType);

    /**
     * Count custom fields by tenant ID and object type
     */
    long countByTenantIdAndObjectType(UUID tenantId, String objectType);

    /**
     * Count custom fields by custom object
     */
    long countByCustomObject(CustomObject customObject);

    /**
     * Count active custom fields by tenant ID and object type
     */
    long countByTenantIdAndObjectTypeAndIsActiveTrue(UUID tenantId, String objectType);

    /**
     * Count active custom fields by custom object
     */
    long countByCustomObjectAndIsActiveTrue(CustomObject customObject);

    /**
     * Find custom fields by tenant ID and search term
     */
    @Query("SELECT cf FROM CustomField cf WHERE cf.tenantId = :tenantId " +
           "AND (LOWER(cf.fieldName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(cf.fieldLabel) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(cf.helpText) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY cf.fieldOrder ASC")
    Page<CustomField> findByTenantIdAndSearchTerm(@Param("tenantId") UUID tenantId,
                                                 @Param("searchTerm") String searchTerm,
                                                 Pageable pageable);

    /**
     * Delete custom fields by tenant ID (for tenant cleanup)
     */
    void deleteByTenantId(UUID tenantId);

    /**
     * Delete custom fields by custom object
     */
    void deleteByCustomObject(CustomObject customObject);
}