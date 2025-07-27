package com.crm.platform.deals.repository;

import com.crm.platform.deals.entity.DealStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DealStageHistoryRepository extends JpaRepository<DealStageHistory, UUID> {

    // Deal-based queries
    List<DealStageHistory> findByDealId(UUID dealId);
    
    List<DealStageHistory> findByDealIdOrderByChangedAtAsc(UUID dealId);
    
    List<DealStageHistory> findByDealIdOrderByChangedAtDesc(UUID dealId);
    
    // Pipeline-based queries
    List<DealStageHistory> findByPipelineId(UUID pipelineId);
    
    List<DealStageHistory> findByPipelineIdOrderByChangedAtDesc(UUID pipelineId);
    
    // Stage-based queries
    List<DealStageHistory> findByFromStageId(UUID fromStageId);
    
    List<DealStageHistory> findByToStageId(UUID toStageId);
    
    // User-based queries
    List<DealStageHistory> findByChangedBy(UUID changedBy);
    
    List<DealStageHistory> findByChangedByOrderByChangedAtDesc(UUID changedBy);
    
    // Time-based queries
    List<DealStageHistory> findByChangedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT dsh FROM DealStageHistory dsh JOIN dsh.deal d WHERE d.tenantId = :tenantId AND dsh.changedAt BETWEEN :startDate AND :endDate")
    List<DealStageHistory> findByTenantIdAndChangedAtBetween(@Param("tenantId") UUID tenantId, 
                                                            @Param("startDate") LocalDateTime startDate, 
                                                            @Param("endDate") LocalDateTime endDate);
    
    // Tenant-aware queries
    @Query("SELECT dsh FROM DealStageHistory dsh JOIN dsh.deal d WHERE d.tenantId = :tenantId")
    List<DealStageHistory> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT dsh FROM DealStageHistory dsh JOIN dsh.deal d WHERE d.tenantId = :tenantId AND dsh.dealId = :dealId")
    List<DealStageHistory> findByTenantIdAndDealId(@Param("tenantId") UUID tenantId, @Param("dealId") UUID dealId);
    
    // Analytics queries
    @Query("SELECT dsh.toStageId, COUNT(dsh) FROM DealStageHistory dsh JOIN dsh.deal d " +
           "WHERE d.tenantId = :tenantId AND dsh.changedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY dsh.toStageId")
    List<Object[]> getStageTransitionCounts(@Param("tenantId") UUID tenantId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT dsh.fromStageId, dsh.toStageId, COUNT(dsh), AVG(dsh.durationInPreviousStageHours) " +
           "FROM DealStageHistory dsh JOIN dsh.deal d " +
           "WHERE d.tenantId = :tenantId AND dsh.changedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY dsh.fromStageId, dsh.toStageId")
    List<Object[]> getStageTransitionAnalytics(@Param("tenantId") UUID tenantId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(dsh.durationInPreviousStageHours) FROM DealStageHistory dsh JOIN dsh.deal d " +
           "WHERE d.tenantId = :tenantId AND dsh.fromStageId = :stageId")
    Double getAverageDurationInStage(@Param("tenantId") UUID tenantId, @Param("stageId") UUID stageId);
    
    // Get latest stage change for a deal
    @Query("SELECT dsh FROM DealStageHistory dsh WHERE dsh.dealId = :dealId ORDER BY dsh.changedAt DESC")
    List<DealStageHistory> findLatestByDealId(@Param("dealId") UUID dealId);
    
    // Count queries
    long countByDealId(UUID dealId);
    
    @Query("SELECT COUNT(dsh) FROM DealStageHistory dsh JOIN dsh.deal d WHERE d.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
    
    // Delete operations
    void deleteByDealId(UUID dealId);
    
    @Query("DELETE FROM DealStageHistory dsh WHERE dsh.dealId IN :dealIds")
    void deleteByDealIdIn(@Param("dealIds") List<UUID> dealIds);
}