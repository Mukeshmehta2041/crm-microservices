package com.crm.platform.accounts.repository;

import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.entity.AccountStatus;
import com.crm.platform.accounts.entity.AccountType;
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
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    // Basic tenant-aware queries
    List<Account> findByTenantId(UUID tenantId);
    
    Page<Account> findByTenantId(UUID tenantId, Pageable pageable);
    
    Optional<Account> findByIdAndTenantId(UUID id, UUID tenantId);
    
    // Name-based queries
    List<Account> findByTenantIdAndNameContainingIgnoreCase(UUID tenantId, String name);
    
    Optional<Account> findByTenantIdAndName(UUID tenantId, String name);
    
    // Account number queries
    Optional<Account> findByTenantIdAndAccountNumber(UUID tenantId, String accountNumber);
    
    boolean existsByTenantIdAndAccountNumber(UUID tenantId, String accountNumber);
    
    // Type and status queries
    List<Account> findByTenantIdAndAccountType(UUID tenantId, AccountType accountType);
    
    List<Account> findByTenantIdAndStatus(UUID tenantId, AccountStatus status);
    
    List<Account> findByTenantIdAndAccountTypeAndStatus(UUID tenantId, AccountType accountType, AccountStatus status);
    
    // Industry queries
    List<Account> findByTenantIdAndIndustry(UUID tenantId, String industry);
    
    @Query("SELECT DISTINCT a.industry FROM Account a WHERE a.tenantId = :tenantId AND a.industry IS NOT NULL ORDER BY a.industry")
    List<String> findDistinctIndustriesByTenantId(@Param("tenantId") UUID tenantId);
    
    // Hierarchy queries
    List<Account> findByTenantIdAndParentAccountIsNull(UUID tenantId);
    
    List<Account> findByTenantIdAndParentAccount(UUID tenantId, Account parentAccount);
    
    List<Account> findByTenantIdAndParentAccountId(UUID tenantId, UUID parentAccountId);
    
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.hierarchyLevel <= :maxLevel")
    List<Account> findByTenantIdAndHierarchyLevelLessThanEqual(@Param("tenantId") UUID tenantId, @Param("maxLevel") Integer maxLevel);
    
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.hierarchyPath LIKE :pathPattern")
    List<Account> findByTenantIdAndHierarchyPathStartingWith(@Param("tenantId") UUID tenantId, @Param("pathPattern") String pathPattern);
    
    // Territory queries
    List<Account> findByTenantIdAndTerritoryId(UUID tenantId, UUID territoryId);
    
    // Owner queries
    List<Account> findByTenantIdAndOwnerId(UUID tenantId, UUID ownerId);
    
    Page<Account> findByTenantIdAndOwnerId(UUID tenantId, UUID ownerId, Pageable pageable);
    
    // Tag queries
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND :tag = ANY(a.tags)")
    List<Account> findByTenantIdAndTag(@Param("tenantId") UUID tenantId, @Param("tag") String tag);
    
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.tags && CAST(:tags AS text[])")
    List<Account> findByTenantIdAndTagsIn(@Param("tenantId") UUID tenantId, @Param("tags") String[] tags);
    
    // Search queries
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.website) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Account> searchByTenantIdAndTerm(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Analytics queries
    @Query("SELECT COUNT(a) FROM Account a WHERE a.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT a.accountType, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId GROUP BY a.accountType")
    List<Object[]> countByTenantIdGroupByAccountType(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT a.status, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId GROUP BY a.status")
    List<Object[]> countByTenantIdGroupByStatus(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT a.industry, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.industry IS NOT NULL GROUP BY a.industry ORDER BY COUNT(a) DESC")
    List<Object[]> countByTenantIdGroupByIndustry(@Param("tenantId") UUID tenantId);
    
    // Hierarchy analytics
    @Query("SELECT a.hierarchyLevel, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId GROUP BY a.hierarchyLevel ORDER BY a.hierarchyLevel")
    List<Object[]> countByTenantIdGroupByHierarchyLevel(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.parentAccount IS NULL")
    long countRootAccountsByTenantId(@Param("tenantId") UUID tenantId);
    
    // Duplicate detection queries
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND " +
           "LOWER(a.name) = LOWER(:name) AND " +
           "(:website IS NULL OR LOWER(a.website) = LOWER(:website)) AND " +
           "(:phone IS NULL OR a.phone = :phone) AND " +
           "a.id != :excludeId")
    List<Account> findPotentialDuplicates(@Param("tenantId") UUID tenantId, 
                                         @Param("name") String name, 
                                         @Param("website") String website, 
                                         @Param("phone") String phone, 
                                         @Param("excludeId") UUID excludeId);
    
    // Custom field queries
    @Query(value = "SELECT * FROM accounts WHERE tenant_id = :tenantId AND custom_fields @> CAST(:customFieldsJson AS jsonb)", nativeQuery = true)
    List<Account> findByTenantIdAndCustomFields(@Param("tenantId") UUID tenantId, @Param("customFieldsJson") String customFieldsJson);
    
    // Bulk operations
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.id IN :ids")
    List<Account> findByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") List<UUID> ids);
    
    void deleteByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);
}