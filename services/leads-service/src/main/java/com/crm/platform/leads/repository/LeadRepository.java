package com.crm.platform.leads.repository;

import com.crm.platform.leads.entity.Lead;
import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;
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
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    // Basic tenant-aware queries
    Page<Lead> findByTenantId(UUID tenantId, Pageable pageable);
    
    List<Lead> findByTenantIdAndStatus(UUID tenantId, LeadStatus status);
    
    List<Lead> findByTenantIdAndQualificationStatus(UUID tenantId, QualificationStatus qualificationStatus);
    
    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);
    
    // Email and phone uniqueness checks
    Optional<Lead> findByTenantIdAndEmail(UUID tenantId, String email);
    
    Optional<Lead> findByTenantIdAndPhone(UUID tenantId, String phone);
    
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);
    
    // Owner-based queries
    Page<Lead> findByTenantIdAndOwnerId(UUID tenantId, UUID ownerId, Pageable pageable);
    
    List<Lead> findByTenantIdAndOwnerIdAndStatus(UUID tenantId, UUID ownerId, LeadStatus status);
    
    // Score-based queries
    List<Lead> findByTenantIdAndLeadScoreGreaterThanEqual(UUID tenantId, Integer minScore);
    
    List<Lead> findByTenantIdAndLeadScoreBetween(UUID tenantId, Integer minScore, Integer maxScore);
    
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.leadScore >= 80 ORDER BY l.leadScore DESC")
    List<Lead> findHotLeadsByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.leadScore >= 50 AND l.leadScore < 80 ORDER BY l.leadScore DESC")
    List<Lead> findWarmLeadsByTenantId(@Param("tenantId") UUID tenantId);
    
    // Follow-up queries
    List<Lead> findByTenantIdAndNextFollowUpAtBefore(UUID tenantId, LocalDateTime dateTime);
    
    List<Lead> findByTenantIdAndNextFollowUpAtBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.nextFollowUpAt IS NOT NULL AND l.nextFollowUpAt < :now")
    List<Lead> findOverdueLeads(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);
    
    // Conversion queries
    List<Lead> findByTenantIdAndConvertedContactIdIsNotNull(UUID tenantId);
    
    List<Lead> findByTenantIdAndConvertedAtBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND (l.convertedContactId IS NOT NULL OR l.convertedAccountId IS NOT NULL OR l.convertedDealId IS NOT NULL)")
    List<Lead> findConvertedLeads(@Param("tenantId") UUID tenantId);
    
    // Activity-based queries
    List<Lead> findByTenantIdAndLastActivityAtBefore(UUID tenantId, LocalDateTime dateTime);
    
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND (l.lastActivityAt IS NULL OR l.lastActivityAt < :cutoffDate)")
    List<Lead> findInactiveLeads(@Param("tenantId") UUID tenantId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Search queries
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND " +
           "(LOWER(l.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Lead> searchLeads(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Company-based queries
    List<Lead> findByTenantIdAndCompanyIgnoreCase(UUID tenantId, String company);
    
    @Query("SELECT DISTINCT l.company FROM Lead l WHERE l.tenantId = :tenantId AND l.company IS NOT NULL ORDER BY l.company")
    List<String> findDistinctCompaniesByTenantId(@Param("tenantId") UUID tenantId);
    
    // Source-based queries
    List<Lead> findByTenantIdAndLeadSource(UUID tenantId, String leadSource);
    
    @Query("SELECT DISTINCT l.leadSource FROM Lead l WHERE l.tenantId = :tenantId AND l.leadSource IS NOT NULL ORDER BY l.leadSource")
    List<String> findDistinctLeadSourcesByTenantId(@Param("tenantId") UUID tenantId);
    
    // Statistics queries
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") LeadStatus status);
    
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.qualificationStatus = :qualificationStatus")
    long countByTenantIdAndQualificationStatus(@Param("tenantId") UUID tenantId, @Param("qualificationStatus") QualificationStatus qualificationStatus);
    
    @Query("SELECT AVG(l.leadScore) FROM Lead l WHERE l.tenantId = :tenantId")
    Double getAverageLeadScoreByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.createdAt >= :startDate")
    long countNewLeadsSince(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.convertedAt >= :startDate")
    long countConvertedLeadsSince(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate);
    
    // Bulk operations
    @Query("UPDATE Lead l SET l.ownerId = :newOwnerId, l.assignedAt = :assignedAt, l.updatedBy = :updatedBy WHERE l.id IN :leadIds AND l.tenantId = :tenantId")
    int bulkUpdateOwner(@Param("tenantId") UUID tenantId, @Param("leadIds") List<UUID> leadIds, 
                       @Param("newOwnerId") UUID newOwnerId, @Param("assignedAt") LocalDateTime assignedAt, 
                       @Param("updatedBy") UUID updatedBy);
    
    @Query("UPDATE Lead l SET l.status = :status, l.updatedBy = :updatedBy WHERE l.id IN :leadIds AND l.tenantId = :tenantId")
    int bulkUpdateStatus(@Param("tenantId") UUID tenantId, @Param("leadIds") List<UUID> leadIds, 
                        @Param("status") LeadStatus status, @Param("updatedBy") UUID updatedBy);
    
    @Query("UPDATE Lead l SET l.qualificationStatus = :qualificationStatus, l.updatedBy = :updatedBy WHERE l.id IN :leadIds AND l.tenantId = :tenantId")
    int bulkUpdateQualificationStatus(@Param("tenantId") UUID tenantId, @Param("leadIds") List<UUID> leadIds, 
                                     @Param("qualificationStatus") QualificationStatus qualificationStatus, 
                                     @Param("updatedBy") UUID updatedBy);
}