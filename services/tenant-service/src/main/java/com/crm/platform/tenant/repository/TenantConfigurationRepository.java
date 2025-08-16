package com.crm.platform.tenant.repository;

import com.crm.platform.tenant.entity.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, UUID> {

    Optional<TenantConfiguration> findByTenantIdAndConfigKey(UUID tenantId, String configKey);

    List<TenantConfiguration> findByTenantId(UUID tenantId);

    List<TenantConfiguration> findByTenantIdAndCategory(UUID tenantId, String category);

    List<TenantConfiguration> findByTenantIdAndIsSystemFalse(UUID tenantId);

    List<TenantConfiguration> findByTenantIdAndIsEditableTrue(UUID tenantId);

    @Modifying
    @Query("DELETE FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId AND tc.configKey = :configKey")
    void deleteByTenantIdAndConfigKey(@Param("tenantId") UUID tenantId, @Param("configKey") String configKey);

    @Modifying
    @Query("DELETE FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId AND tc.category = :category")
    void deleteByTenantIdAndCategory(@Param("tenantId") UUID tenantId, @Param("category") String category);

    @Query("SELECT tc FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId AND tc.configKey IN :keys")
    List<TenantConfiguration> findByTenantIdAndConfigKeyIn(@Param("tenantId") UUID tenantId, 
                                                          @Param("keys") List<String> keys);

    boolean existsByTenantIdAndConfigKey(UUID tenantId, String configKey);

    @Query("SELECT COUNT(tc) FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}