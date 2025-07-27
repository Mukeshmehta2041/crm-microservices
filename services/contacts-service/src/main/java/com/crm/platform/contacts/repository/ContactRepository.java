package com.crm.platform.contacts.repository;

import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.entity.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {

    // Basic tenant-aware queries
    Page<Contact> findByTenantId(UUID tenantId, Pageable pageable);
    
    List<Contact> findByTenantIdAndContactStatus(UUID tenantId, ContactStatus status);
    
    Optional<Contact> findByTenantIdAndId(UUID tenantId, UUID id);
    
    // Email-based queries
    Optional<Contact> findByTenantIdAndEmail(UUID tenantId, String email);
    
    List<Contact> findByTenantIdAndEmailIn(UUID tenantId, List<String> emails);
    
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    
    // Name-based queries
    List<Contact> findByTenantIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
        UUID tenantId, String firstName, String lastName);
    
    // Account-based queries
    List<Contact> findByTenantIdAndAccountId(UUID tenantId, UUID accountId);
    
    // Owner-based queries
    List<Contact> findByTenantIdAndOwnerId(UUID tenantId, UUID ownerId);
    
    // Lead score queries
    List<Contact> findByTenantIdAndLeadScoreGreaterThanEqual(UUID tenantId, Integer minScore);
    
    List<Contact> findByTenantIdAndLeadScoreBetween(UUID tenantId, Integer minScore, Integer maxScore);
    
    // Date-based queries
    List<Contact> findByTenantIdAndCreatedAtBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);
    
    List<Contact> findByTenantIdAndUpdatedAtAfter(UUID tenantId, LocalDateTime after);
    
    // Tag-based queries
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND :tag = ANY(c.tags)")
    List<Contact> findByTenantIdAndTag(@Param("tenantId") UUID tenantId, @Param("tag") String tag);
    
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.tags && CAST(:tags AS text[])")
    List<Contact> findByTenantIdAndTagsIn(@Param("tenantId") UUID tenantId, @Param("tags") List<String> tags);
    
    // Full-text search queries
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.department) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Contact> searchContacts(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);
    
    // Custom field queries
    @Query(value = "SELECT * FROM contacts WHERE tenant_id = :tenantId AND " +
                   "custom_fields @> CAST(:customFieldJson AS jsonb)", nativeQuery = true)
    List<Contact> findByTenantIdAndCustomField(@Param("tenantId") UUID tenantId, 
                                               @Param("customFieldJson") String customFieldJson);
    
    // Duplicate detection queries
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND " +
           "LOWER(c.firstName) = LOWER(:firstName) AND LOWER(c.lastName) = LOWER(:lastName)")
    List<Contact> findPotentialDuplicatesByName(@Param("tenantId") UUID tenantId, 
                                                @Param("firstName") String firstName, 
                                                @Param("lastName") String lastName);
    
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND " +
           "(c.email = :email OR c.phone = :phone OR c.mobile = :mobile)")
    List<Contact> findPotentialDuplicatesByContact(@Param("tenantId") UUID tenantId, 
                                                   @Param("email") String email, 
                                                   @Param("phone") String phone, 
                                                   @Param("mobile") String mobile);
    
    // Statistics queries
    @Query("SELECT COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.contactStatus = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") ContactStatus status);
    
    @Query("SELECT COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.createdAt >= :since")
    Long countByTenantIdAndCreatedSince(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);
    
    @Query("SELECT AVG(c.leadScore) FROM Contact c WHERE c.tenantId = :tenantId AND c.leadScore > 0")
    Double getAverageLeadScore(@Param("tenantId") UUID tenantId);
    
    // Bulk operations
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.id IN :ids")
    List<Contact> findByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") List<UUID> ids);
    
    void deleteByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);
}