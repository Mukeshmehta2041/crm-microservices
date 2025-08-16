package com.crm.platform.workflow.repository;

import com.crm.platform.workflow.entity.BusinessRule;
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
 * Repository interface for BusinessRule entities
 */
@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, UUID> {

    /**
     * Find business rules by tenant ID
     */
    Page<BusinessRule> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find active business rules by tenant ID
     */
    Page<BusinessRule> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    /**
     * Find business rules by tenant ID and rule type
     */
    Page<BusinessRule> findByTenantIdAndRuleType(UUID tenantId, BusinessRule.RuleType ruleType, Pageable pageable);

    /**
     * Find business rules by tenant ID and entity type
     */
    Page<BusinessRule> findByTenantIdAndEntityType(UUID tenantId, String entityType, Pageable pageable);

    /**
     * Find active business rules by tenant ID and entity type
     */
    List<BusinessRule> findByTenantIdAndEntityTypeAndIsActiveTrueOrderByPriorityDesc(UUID tenantId, String entityType);

    /**
     * Find active business rules by tenant ID, entity type and rule type
     */
    List<BusinessRule> findByTenantIdAndEntityTypeAndRuleTypeAndIsActiveTrueOrderByPriorityDesc(
            UUID tenantId, String entityType, BusinessRule.RuleType ruleType);

    /**
     * Find business rule by tenant ID and name
     */
    Optional<BusinessRule> findByTenantIdAndName(UUID tenantId, String name);

    /**
     * Check if business rule exists by tenant ID and name
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find business rules by tenant ID and name containing (case insensitive)
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId " +
           "AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<BusinessRule> findByTenantIdAndNameContainingIgnoreCase(@Param("tenantId") UUID tenantId,
                                                                 @Param("name") String name,
                                                                 Pageable pageable);

    /**
     * Find business rules created by user
     */
    Page<BusinessRule> findByTenantIdAndCreatedBy(UUID tenantId, UUID createdBy, Pageable pageable);

    /**
     * Count business rules by tenant ID
     */
    long countByTenantId(UUID tenantId);

    /**
     * Count active business rules by tenant ID
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Count business rules by tenant ID and rule type
     */
    long countByTenantIdAndRuleType(UUID tenantId, BusinessRule.RuleType ruleType);

    /**
     * Find business rules by multiple rule types
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId " +
           "AND r.ruleType IN :ruleTypes AND r.isActive = true " +
           "ORDER BY r.priority DESC")
    List<BusinessRule> findByTenantIdAndRuleTypeInAndIsActiveTrue(@Param("tenantId") UUID tenantId,
                                                                  @Param("ruleTypes") List<BusinessRule.RuleType> ruleTypes);

    /**
     * Find business rules by tenant ID and multiple entity types
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId " +
           "AND r.entityType IN :entityTypes AND r.isActive = true " +
           "ORDER BY r.priority DESC")
    List<BusinessRule> findByTenantIdAndEntityTypeInAndIsActiveTrue(@Param("tenantId") UUID tenantId,
                                                                    @Param("entityTypes") List<String> entityTypes);

    /**
     * Get rule statistics by tenant ID
     */
    @Query("SELECT r.ruleType, COUNT(r) FROM BusinessRule r " +
           "WHERE r.tenantId = :tenantId GROUP BY r.ruleType")
    List<Object[]> getRuleStatsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Get rule statistics by entity type
     */
    @Query("SELECT r.entityType, COUNT(r) FROM BusinessRule r " +
           "WHERE r.tenantId = :tenantId GROUP BY r.entityType")
    List<Object[]> getRuleStatsByEntityType(@Param("tenantId") UUID tenantId);
}