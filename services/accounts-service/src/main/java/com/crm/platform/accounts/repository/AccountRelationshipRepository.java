package com.crm.platform.accounts.repository;

import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.entity.AccountRelationship;
import com.crm.platform.accounts.entity.RelationshipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRelationshipRepository extends JpaRepository<AccountRelationship, UUID> {

    // Basic tenant-aware queries
    List<AccountRelationship> findByTenantId(UUID tenantId);
    
    Page<AccountRelationship> findByTenantId(UUID tenantId, Pageable pageable);
    
    Optional<AccountRelationship> findByIdAndTenantId(UUID id, UUID tenantId);
    
    // Account-specific relationship queries
    List<AccountRelationship> findByTenantIdAndFromAccount(UUID tenantId, Account fromAccount);
    
    List<AccountRelationship> findByTenantIdAndToAccount(UUID tenantId, Account toAccount);
    
    List<AccountRelationship> findByTenantIdAndFromAccountId(UUID tenantId, UUID fromAccountId);
    
    List<AccountRelationship> findByTenantIdAndToAccountId(UUID tenantId, UUID toAccountId);
    
    // Bidirectional relationship queries
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "(ar.fromAccount.id = :accountId OR ar.toAccount.id = :accountId)")
    List<AccountRelationship> findAllRelationshipsByAccountId(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId);
    
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "(ar.fromAccount.id = :accountId OR ar.toAccount.id = :accountId) AND ar.isActive = true")
    List<AccountRelationship> findActiveRelationshipsByAccountId(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId);
    
    // Relationship type queries
    List<AccountRelationship> findByTenantIdAndRelationshipType(UUID tenantId, RelationshipType relationshipType);
    
    List<AccountRelationship> findByTenantIdAndFromAccountIdAndRelationshipType(UUID tenantId, UUID fromAccountId, RelationshipType relationshipType);
    
    List<AccountRelationship> findByTenantIdAndToAccountIdAndRelationshipType(UUID tenantId, UUID toAccountId, RelationshipType relationshipType);
    
    // Specific relationship queries
    Optional<AccountRelationship> findByTenantIdAndFromAccountIdAndToAccountIdAndRelationshipType(
        UUID tenantId, UUID fromAccountId, UUID toAccountId, RelationshipType relationshipType);
    
    boolean existsByTenantIdAndFromAccountIdAndToAccountIdAndRelationshipType(
        UUID tenantId, UUID fromAccountId, UUID toAccountId, RelationshipType relationshipType);
    
    // Active relationship queries
    List<AccountRelationship> findByTenantIdAndIsActive(UUID tenantId, Boolean isActive);
    
    List<AccountRelationship> findByTenantIdAndFromAccountIdAndIsActive(UUID tenantId, UUID fromAccountId, Boolean isActive);
    
    List<AccountRelationship> findByTenantIdAndToAccountIdAndIsActive(UUID tenantId, UUID toAccountId, Boolean isActive);
    
    // Hierarchical relationship queries
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "ar.relationshipType IN ('PARENT_CHILD', 'SUBSIDIARY') AND ar.isActive = true")
    List<AccountRelationship> findHierarchicalRelationships(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "ar.fromAccount.id = :accountId AND ar.relationshipType IN ('PARENT_CHILD', 'SUBSIDIARY') AND ar.isActive = true")
    List<AccountRelationship> findChildRelationships(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId);
    
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "ar.toAccount.id = :accountId AND ar.relationshipType IN ('PARENT_CHILD', 'SUBSIDIARY') AND ar.isActive = true")
    List<AccountRelationship> findParentRelationships(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId);
    
    // Business partnership queries
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "ar.relationshipType IN ('PARTNER', 'ALLIANCE', 'JOINT_VENTURE', 'STRATEGIC_PARTNER', 'TECHNOLOGY_PARTNER', 'CHANNEL_PARTNER') " +
           "AND ar.isActive = true")
    List<AccountRelationship> findBusinessPartnershipRelationships(@Param("tenantId") UUID tenantId);
    
    // Commercial relationship queries
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "ar.relationshipType IN ('VENDOR', 'CUSTOMER', 'SUPPLIER', 'DISTRIBUTOR', 'RESELLER') " +
           "AND ar.isActive = true")
    List<AccountRelationship> findCommercialRelationships(@Param("tenantId") UUID tenantId);
    
    // Relationship strength queries
    List<AccountRelationship> findByTenantIdAndStrengthGreaterThanEqual(UUID tenantId, Integer minStrength);
    
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "(ar.fromAccount.id = :accountId OR ar.toAccount.id = :accountId) AND " +
           "ar.strength >= :minStrength AND ar.isActive = true " +
           "ORDER BY ar.strength DESC")
    List<AccountRelationship> findStrongRelationshipsByAccountId(@Param("tenantId") UUID tenantId, 
                                                                @Param("accountId") UUID accountId, 
                                                                @Param("minStrength") Integer minStrength);
    
    // Analytics queries
    @Query("SELECT ar.relationshipType, COUNT(ar) FROM AccountRelationship ar WHERE ar.tenantId = :tenantId " +
           "AND ar.isActive = true GROUP BY ar.relationshipType")
    List<Object[]> countActiveRelationshipsByType(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(ar) FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND ar.isActive = true")
    long countActiveRelationships(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(DISTINCT ar.fromAccount.id) FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND ar.isActive = true")
    long countAccountsWithOutgoingRelationships(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(DISTINCT ar.toAccount.id) FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND ar.isActive = true")
    long countAccountsWithIncomingRelationships(@Param("tenantId") UUID tenantId);
    
    // Network analysis queries
    @Query("SELECT ar.fromAccount.id, COUNT(ar) FROM AccountRelationship ar WHERE ar.tenantId = :tenantId " +
           "AND ar.isActive = true GROUP BY ar.fromAccount.id ORDER BY COUNT(ar) DESC")
    List<Object[]> findMostConnectedAccounts(@Param("tenantId") UUID tenantId, Pageable pageable);
    
    @Query("SELECT AVG(CAST(ar.strength AS double)) FROM AccountRelationship ar WHERE ar.tenantId = :tenantId " +
           "AND ar.isActive = true AND ar.strength IS NOT NULL")
    Double getAverageRelationshipStrength(@Param("tenantId") UUID tenantId);
    
    // Bulk operations
    @Query("SELECT ar FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND ar.id IN :ids")
    List<AccountRelationship> findByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") List<UUID> ids);
    
    void deleteByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);
    
    // Cleanup queries
    @Query("DELETE FROM AccountRelationship ar WHERE ar.tenantId = :tenantId AND " +
           "(ar.fromAccount.id = :accountId OR ar.toAccount.id = :accountId)")
    void deleteAllRelationshipsByAccountId(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId);
}