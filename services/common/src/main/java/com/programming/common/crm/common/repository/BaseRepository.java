package com.programming.common.crm.common.repository;

import com.programming.common.crm.common.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, UUID> {

    List<T> findByTenantId(UUID tenantId);

    Page<T> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.createdBy = :createdBy")
    List<T> findByTenantIdAndCreatedBy(@Param("tenantId") UUID tenantId, @Param("createdBy") UUID createdBy);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    Optional<T> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}