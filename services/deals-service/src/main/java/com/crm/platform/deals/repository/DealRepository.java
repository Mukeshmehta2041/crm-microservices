package com.crm.platform.deals.repository;

import com.crm.platform.deals.entity.Deal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealRepository extends JpaRepository<Deal, UUID>, JpaSpecificationExecutor<Deal> {

    // Basic tenant-aware queries
    Page<Deal> findByTenantId(UUID tenantId, Pageable pageable);
    
    Optional<Deal> findByIdAndTenantId(UUID id, UUID tenantId);
    
    List<Deal> findByTenantIdAndOwnerId(UUID tenantId, UUID ownerId);
    
    // Pipeline and stage queries
    List<Deal> findByTenantIdAndPipelineId(UUID tenantId, UUID pipelineId);
    
    List<Deal> findByTenantIdAndStageId(UUID tenantId, UUID stageId);
    
    List<Deal> findByTenantIdAndPipelineIdAndStageId(UUID tenantId, UUID pipelineId, UUID stageId);
    
    // Status-based queries
    List<Deal> findByTenantIdAndIsClosed(UUID tenantId, Boolean isClosed);
    
    List<Deal> findByTenantIdAndIsClosedAndIsWon(UUID tenantId, Boolean isClosed, Boolean isWon);
    
    // Date-based queries
    List<Deal> findByTenantIdAndExpectedCloseDateBetween(UUID tenantId, LocalDate startDate, LocalDate endDate);
    
    List<Deal> findByTenantIdAndActualCloseDateBetween(UUID tenantId, LocalDate startDate, LocalDate endDate);
    
    // Account and contact queries
    List<Deal> findByTenantIdAndAccountId(UUID tenantId, UUID accountId);
    
    List<Deal> findByTenantIdAndContactId(UUID tenantId, UUID contactId);
    
    // Forecast queries
    @Query("SELECT d FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false " +
           "AND (:pipelineId IS NULL OR d.pipelineId = :pipelineId) " +
           "AND (:ownerId IS NULL OR d.ownerId = :ownerId)")
    List<Deal> findOpenDealsForForecast(@Param("tenantId") UUID tenantId, 
                                       @Param("pipelineId") UUID pipelineId, 
                                       @Param("ownerId") UUID ownerId);
    
    @Query("SELECT d FROM Deal d WHERE d.tenantId = :tenantId " +
           "AND d.expectedCloseDate BETWEEN :startDate AND :endDate " +
           "AND (:includeClosedDeals = true OR d.isClosed = false)")
    List<Deal> findDealsForForecastPeriod(@Param("tenantId") UUID tenantId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("includeClosedDeals") Boolean includeClosedDeals);
    
    // Analytics queries
    @Query("SELECT SUM(d.amount) FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false")
    BigDecimal getTotalPipelineValue(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT SUM(d.amount * d.probability / 100) FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false")
    BigDecimal getTotalWeightedValue(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false")
    Long getOpenDealsCount(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = true AND d.isWon = true")
    Long getWonDealsCount(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = true AND d.isWon = false")
    Long getLostDealsCount(@Param("tenantId") UUID tenantId);
    
    // Pipeline analytics
    @Query("SELECT d.pipelineId, COUNT(d), SUM(d.amount), SUM(d.amount * d.probability / 100) " +
           "FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false " +
           "GROUP BY d.pipelineId")
    List<Object[]> getPipelineAnalytics(@Param("tenantId") UUID tenantId);
    
    // Stage analytics
    @Query("SELECT d.stageId, COUNT(d), SUM(d.amount), SUM(d.amount * d.probability / 100), AVG(d.probability) " +
           "FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false " +
           "GROUP BY d.stageId")
    List<Object[]> getStageAnalytics(@Param("tenantId") UUID tenantId);
    
    // Owner analytics
    @Query("SELECT d.ownerId, COUNT(d), SUM(d.amount), SUM(d.amount * d.probability / 100) " +
           "FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false " +
           "GROUP BY d.ownerId")
    List<Object[]> getOwnerAnalytics(@Param("tenantId") UUID tenantId);
    
    // Time-based analytics
    @Query("SELECT DATE_TRUNC('month', d.expectedCloseDate), COUNT(d), SUM(d.amount), SUM(d.amount * d.probability / 100) " +
           "FROM Deal d WHERE d.tenantId = :tenantId AND d.isClosed = false " +
           "AND d.expectedCloseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_TRUNC('month', d.expectedCloseDate) " +
           "ORDER BY DATE_TRUNC('month', d.expectedCloseDate)")
    List<Object[]> getMonthlyForecast(@Param("tenantId") UUID tenantId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
    
    // Search by name
    @Query("SELECT d FROM Deal d WHERE d.tenantId = :tenantId AND LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Deal> findByTenantIdAndNameContainingIgnoreCase(@Param("tenantId") UUID tenantId, @Param("name") String name);
    
    // Search by tags
    @Query("SELECT d FROM Deal d WHERE d.tenantId = :tenantId AND d.tags && CAST(:tags AS text[])")
    List<Deal> findByTenantIdAndTagsContaining(@Param("tenantId") UUID tenantId, @Param("tags") String[] tags);
    
    // Custom fields search
    @Query("SELECT d FROM Deal d WHERE d.tenantId = :tenantId AND d.customFields @> CAST(:customFields AS jsonb)")
    List<Deal> findByTenantIdAndCustomFields(@Param("tenantId") UUID tenantId, @Param("customFields") String customFields);
    
    // Bulk operations
    @Query("UPDATE Deal d SET d.stageId = :newStageId, d.updatedBy = :updatedBy, d.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE d.id IN :dealIds AND d.tenantId = :tenantId")
    int bulkUpdateStage(@Param("dealIds") List<UUID> dealIds, 
                       @Param("newStageId") UUID newStageId, 
                       @Param("updatedBy") UUID updatedBy, 
                       @Param("tenantId") UUID tenantId);
    
    @Query("UPDATE Deal d SET d.ownerId = :newOwnerId, d.updatedBy = :updatedBy, d.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE d.id IN :dealIds AND d.tenantId = :tenantId")
    int bulkUpdateOwner(@Param("dealIds") List<UUID> dealIds, 
                       @Param("newOwnerId") UUID newOwnerId, 
                       @Param("updatedBy") UUID updatedBy, 
                       @Param("tenantId") UUID tenantId);
    
    // Delete operations
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
    
    void deleteByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);
}