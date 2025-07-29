package com.crm.platform.customobjects.repository;

import com.crm.platform.customobjects.entity.CustomObject;
import com.crm.platform.customobjects.entity.CustomObjectRelationship;
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
 * Repository interface for CustomObjectRelationship entity
 */
@Repository
public interface CustomObjectRelationshipRepository extends JpaRepository<CustomObjectRelationship, UUID>, 
                                                           JpaSpecificationExecutor<CustomObjectRelationship> {

    /**
     * Find relationship by tenant ID and ID
     */
    Optional<CustomObjectRelationship> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Find relationship by tenant ID, from object, to object, and relationship name
     */
    Optional<CustomObjectRelationship> findByTenantIdAndFromObjectAndToObjectAndRelationshipName(
            UUID tenantId, CustomObject fromObject, CustomObject toObject, String relationshipName);

    /**
     * Find all relationships by tenant ID and from object
     */
    List<CustomObjectRelationship> findByTenantIdAndFromObjectOrderByCreatedAtDesc(UUID tenantId, CustomObject fromObject);

    /**
     * Find all active relationships by tenant ID and from object
     */
    List<CustomObjectRelationship> findByTenantIdAndFromObjectAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, CustomObject fromObject);

    /**
     * Find all relationships by tenant ID and to object
     */
    List<CustomObjectRelationship> findByTenantIdAndToObjectOrderByCreatedAtDesc(UUID tenantId, CustomObject toObject);

    /**
     * Find all active relationships by tenant ID and to object
     */
    List<CustomObjectRelationship> findByTenantIdAndToObjectAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, CustomObject toObject);

    /**
     * Find all relationships involving a custom object (either from or to)
     */
    @Query("SELECT cor FROM CustomObjectRelationship cor WHERE cor.tenantId = :tenantId " +
           "AND (cor.fromObject = :customObject OR cor.toObject = :customObject) " +
           "ORDER BY cor.createdAt DESC")
    List<CustomObjectRelationship> findByTenantIdAndCustomObject(@Param("tenantId") UUID tenantId,
                                                                @Param("customObject") CustomObject customObject);

    /**
     * Find all active relationships involving a custom object (either from or to)
     */
    @Query("SELECT cor FROM CustomObjectRelationship cor WHERE cor.tenantId = :tenantId " +
           "AND (cor.fromObject = :customObject OR cor.toObject = :customObject) " +
           "AND cor.isActive = true " +
           "ORDER BY cor.createdAt DESC")
    List<CustomObjectRelationship> findByTenantIdAndCustomObjectAndIsActiveTrue(@Param("tenantId") UUID tenantId,
                                                                               @Param("customObject") CustomObject customObject);

    /**
     * Find relationships by tenant ID with pagination
     */
    Page<CustomObjectRelationship> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find active relationships by tenant ID with pagination
     */
    Page<CustomObjectRelationship> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    /**
     * Find relationships by tenant ID and relationship type
     */
    List<CustomObjectRelationship> findByTenantIdAndRelationshipTypeAndIsActiveTrueOrderByCreatedAtDesc(
            UUID tenantId, CustomObjectRelationship.RelationshipType relationshipType);

    /**
     * Check if relationship name exists between two objects
     */
    boolean existsByTenantIdAndFromObjectAndToObjectAndRelationshipName(
            UUID tenantId, CustomObject fromObject, CustomObject toObject, String relationshipName);

    /**
     * Check if relationship name exists between two objects excluding specific ID
     */
    boolean existsByTenantIdAndFromObjectAndToObjectAndRelationshipNameAndIdNot(
            UUID tenantId, CustomObject fromObject, CustomObject toObject, String relationshipName, UUID id);

    /**
     * Count relationships by tenant ID and from object
     */
    long countByTenantIdAndFromObject(UUID tenantId, CustomObject fromObject);

    /**
     * Count active relationships by tenant ID and from object
     */
    long countByTenantIdAndFromObjectAndIsActiveTrue(UUID tenantId, CustomObject fromObject);

    /**
     * Count relationships by tenant ID and to object
     */
    long countByTenantIdAndToObject(UUID tenantId, CustomObject toObject);

    /**
     * Count active relationships by tenant ID and to object
     */
    long countByTenantIdAndToObjectAndIsActiveTrue(UUID tenantId, CustomObject toObject);

    /**
     * Find required relationships by tenant ID and from object
     */
    List<CustomObjectRelationship> findByTenantIdAndFromObjectAndIsRequiredTrueAndIsActiveTrueOrderByCreatedAtDesc(
            UUID tenantId, CustomObject fromObject);

    /**
     * Find cascade delete relationships by tenant ID and from object
     */
    List<CustomObjectRelationship> findByTenantIdAndFromObjectAndCascadeDeleteTrueAndIsActiveTrueOrderByCreatedAtDesc(
            UUID tenantId, CustomObject fromObject);

    /**
     * Find relationships by tenant ID and search term
     */
    @Query("SELECT cor FROM CustomObjectRelationship cor WHERE cor.tenantId = :tenantId " +
           "AND (LOWER(cor.relationshipName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(cor.relationshipLabel) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(cor.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY cor.createdAt DESC")
    Page<CustomObjectRelationship> findByTenantIdAndSearchTerm(@Param("tenantId") UUID tenantId,
                                                              @Param("searchTerm") String searchTerm,
                                                              Pageable pageable);

    /**
     * Delete relationships by tenant ID (for tenant cleanup)
     */
    void deleteByTenantId(UUID tenantId);

    /**
     * Delete relationships by from object
     */
    void deleteByFromObject(CustomObject fromObject);

    /**
     * Delete relationships by to object
     */
    void deleteByToObject(CustomObject toObject);

    /**
     * Delete relationships involving a custom object (either from or to)
     */
    @Query("DELETE FROM CustomObjectRelationship cor WHERE cor.fromObject = :customObject OR cor.toObject = :customObject")
    void deleteByCustomObject(@Param("customObject") CustomObject customObject);
}