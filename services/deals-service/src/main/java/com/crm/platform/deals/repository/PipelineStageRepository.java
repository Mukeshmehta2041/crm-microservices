package com.crm.platform.deals.repository;

import com.crm.platform.deals.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    // Pipeline-based queries
    List<PipelineStage> findByPipelineId(UUID pipelineId);
    
    List<PipelineStage> findByPipelineIdOrderByDisplayOrderAsc(UUID pipelineId);
    
    List<PipelineStage> findByPipelineIdAndIsActiveTrue(UUID pipelineId);
    
    List<PipelineStage> findByPipelineIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID pipelineId);
    
    // Tenant-aware queries
    @Query("SELECT ps FROM PipelineStage ps JOIN ps.pipeline p WHERE p.tenantId = :tenantId")
    List<PipelineStage> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT ps FROM PipelineStage ps JOIN ps.pipeline p WHERE p.tenantId = :tenantId AND ps.id = :stageId")
    Optional<PipelineStage> findByIdAndTenantId(@Param("stageId") UUID stageId, @Param("tenantId") UUID tenantId);
    
    // Stage status queries
    List<PipelineStage> findByPipelineIdAndIsClosed(UUID pipelineId, Boolean isClosed);
    
    List<PipelineStage> findByPipelineIdAndIsClosedAndIsWon(UUID pipelineId, Boolean isClosed, Boolean isWon);
    
    // Name-based queries
    Optional<PipelineStage> findByPipelineIdAndName(UUID pipelineId, String name);
    
    @Query("SELECT ps FROM PipelineStage ps WHERE ps.pipelineId = :pipelineId AND LOWER(ps.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<PipelineStage> findByPipelineIdAndNameContainingIgnoreCase(@Param("pipelineId") UUID pipelineId, @Param("name") String name);
    
    // Order-based queries
    Optional<PipelineStage> findByPipelineIdAndDisplayOrder(UUID pipelineId, Integer displayOrder);
    
    @Query("SELECT ps FROM PipelineStage ps WHERE ps.pipelineId = :pipelineId AND ps.displayOrder > :displayOrder ORDER BY ps.displayOrder ASC")
    List<PipelineStage> findNextStages(@Param("pipelineId") UUID pipelineId, @Param("displayOrder") Integer displayOrder);
    
    @Query("SELECT ps FROM PipelineStage ps WHERE ps.pipelineId = :pipelineId AND ps.displayOrder < :displayOrder ORDER BY ps.displayOrder DESC")
    List<PipelineStage> findPreviousStages(@Param("pipelineId") UUID pipelineId, @Param("displayOrder") Integer displayOrder);
    
    // Get first and last stages
    Optional<PipelineStage> findFirstByPipelineIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID pipelineId);
    
    Optional<PipelineStage> findFirstByPipelineIdAndIsActiveTrueOrderByDisplayOrderDesc(UUID pipelineId);
    
    // Won and lost stages
    List<PipelineStage> findByPipelineIdAndIsWonTrue(UUID pipelineId);
    
    List<PipelineStage> findByPipelineIdAndIsClosedTrueAndIsWonFalse(UUID pipelineId);
    
    // Check if stage exists
    boolean existsByPipelineIdAndName(UUID pipelineId, String name);
    
    boolean existsByPipelineIdAndNameAndIdNot(UUID pipelineId, String name, UUID id);
    
    boolean existsByPipelineIdAndDisplayOrder(UUID pipelineId, Integer displayOrder);
    
    boolean existsByPipelineIdAndDisplayOrderAndIdNot(UUID pipelineId, Integer displayOrder, UUID id);
    
    // Count queries
    long countByPipelineId(UUID pipelineId);
    
    long countByPipelineIdAndIsActiveTrue(UUID pipelineId);
    
    // Max display order
    @Query("SELECT COALESCE(MAX(ps.displayOrder), 0) FROM PipelineStage ps WHERE ps.pipelineId = :pipelineId")
    Integer getMaxDisplayOrder(@Param("pipelineId") UUID pipelineId);
    
    // Delete operations
    void deleteByPipelineId(UUID pipelineId);
    
    @Query("DELETE FROM PipelineStage ps WHERE ps.id = :stageId AND ps.pipeline.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("stageId") UUID stageId, @Param("tenantId") UUID tenantId);
}