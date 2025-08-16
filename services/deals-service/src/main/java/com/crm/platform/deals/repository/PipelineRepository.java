package com.crm.platform.deals.repository;

import com.crm.platform.deals.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    // Basic tenant-aware queries
    List<Pipeline> findByTenantId(UUID tenantId);
    
    List<Pipeline> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);
    
    Optional<Pipeline> findByIdAndTenantId(UUID id, UUID tenantId);
    
    // Active pipelines
    List<Pipeline> findByTenantIdAndIsActiveTrue(UUID tenantId);
    
    List<Pipeline> findByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
    
    // Default pipeline
    Optional<Pipeline> findByTenantIdAndIsDefaultTrue(UUID tenantId);
    
    // Name-based queries
    Optional<Pipeline> findByTenantIdAndName(UUID tenantId, String name);
    
    @Query("SELECT p FROM Pipeline p WHERE p.tenantId = :tenantId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Pipeline> findByTenantIdAndNameContainingIgnoreCase(@Param("tenantId") UUID tenantId, @Param("name") String name);
    
    // Check if pipeline exists
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    
    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);
    
    // Count queries
    long countByTenantId(UUID tenantId);
    
    long countByTenantIdAndIsActiveTrue(UUID tenantId);
    
    // Delete operations
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}