package com.crm.platform.leads.repository;

import com.crm.platform.leads.entity.LeadScoreHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeadScoreHistoryRepository extends JpaRepository<LeadScoreHistory, UUID> {

    // Basic queries
    List<LeadScoreHistory> findByTenantIdAndLeadIdOrderByCreatedAtDesc(UUID tenantId, UUID leadId);
    
    Page<LeadScoreHistory> findByTenantIdAndLeadId(UUID tenantId, UUID leadId, Pageable pageable);
    
    // Recent score changes
    List<LeadScoreHistory> findByTenantIdAndLeadIdAndCreatedAtAfterOrderByCreatedAtDesc(
        UUID tenantId, UUID leadId, LocalDateTime after);
    
    // Score change analysis
    @Query("SELECT h FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.leadId = :leadId AND h.scoreChange > 0 ORDER BY h.createdAt DESC")
    List<LeadScoreHistory> findPositiveScoreChanges(@Param("tenantId") UUID tenantId, @Param("leadId") UUID leadId);
    
    @Query("SELECT h FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.leadId = :leadId AND h.scoreChange < 0 ORDER BY h.createdAt DESC")
    List<LeadScoreHistory> findNegativeScoreChanges(@Param("tenantId") UUID tenantId, @Param("leadId") UUID leadId);
    
    @Query("SELECT h FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.leadId = :leadId AND ABS(h.scoreChange) >= :minChange ORDER BY h.createdAt DESC")
    List<LeadScoreHistory> findSignificantScoreChanges(@Param("tenantId") UUID tenantId, @Param("leadId") UUID leadId, @Param("minChange") Integer minChange);
    
    // Rule-based analysis
    List<LeadScoreHistory> findByTenantIdAndRuleName(UUID tenantId, String ruleName);
    
    List<LeadScoreHistory> findByTenantIdAndRuleCategory(UUID tenantId, String ruleCategory);
    
    @Query("SELECT h.ruleName, COUNT(h), AVG(h.scoreChange) FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.createdAt >= :since GROUP BY h.ruleName ORDER BY COUNT(h) DESC")
    List<Object[]> getRuleEffectivenessStats(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);
    
    // Time-based queries
    List<LeadScoreHistory> findByTenantIdAndCreatedAtBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT DATE(h.createdAt), COUNT(h), AVG(h.scoreChange) FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.createdAt >= :since GROUP BY DATE(h.createdAt) ORDER BY DATE(h.createdAt)")
    List<Object[]> getDailyScoreChangeStats(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);
    
    // Latest score change for a lead
    @Query("SELECT h FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.leadId = :leadId ORDER BY h.createdAt DESC LIMIT 1")
    LeadScoreHistory findLatestScoreChange(@Param("tenantId") UUID tenantId, @Param("leadId") UUID leadId);
    
    // Statistics
    @Query("SELECT COUNT(h) FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.leadId = :leadId")
    long countByTenantIdAndLeadId(@Param("tenantId") UUID tenantId, @Param("leadId") UUID leadId);
    
    @Query("SELECT SUM(h.scoreChange) FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.leadId = :leadId")
    Integer getTotalScoreChangeForLead(@Param("tenantId") UUID tenantId, @Param("leadId") UUID leadId);
    
    @Query("SELECT AVG(h.scoreChange) FROM LeadScoreHistory h WHERE h.tenantId = :tenantId AND h.createdAt >= :since")
    Double getAverageScoreChange(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);
}