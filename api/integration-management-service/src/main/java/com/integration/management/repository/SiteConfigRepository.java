package com.integration.management.repository;

import com.integration.management.entity.SiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteConfigRepository extends JpaRepository<SiteConfig, UUID> {

    @Query(value = """
            SELECT DISTINCT ON (config_key) *
            FROM integration_platform.site_configs
            WHERE is_deleted = false AND (tenant_id = :tenantId OR tenant_id = 'GLOBAL')
            ORDER BY config_key, CASE WHEN tenant_id = :tenantId THEN 1 ELSE 2 END
            """, nativeQuery = true)
    List<SiteConfig> findEffectiveConfigsByTenant(@Param("tenantId") String tenantId);

    @Query("""
            SELECT s FROM SiteConfig s WHERE s.isDeleted = false
            AND (s.id = :id AND (s.tenantId = :tenantId OR s.tenantId = 'GLOBAL'))
            """)
    Optional<SiteConfig> findByTenantWithGlobalFallback(@Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("""
            SELECT s FROM SiteConfig s WHERE s.isDeleted = false AND s.id = :id AND s.tenantId = :tenantId
            """)
    Optional<SiteConfig> findByTenant(@Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("""
            SELECT s.configKey FROM SiteConfig s
            WHERE s.id = :id AND (s.tenantId = :tenantId OR s.tenantId = 'GLOBAL') AND s.isDeleted = false
            """)
    String findConfigKeyById(@Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("""
            SELECT s.configValue FROM SiteConfig s WHERE s.isDeleted = false
            AND s.configKey = :configKey
            AND (s.tenantId = :tenantId OR s.tenantId = 'GLOBAL')
            ORDER BY CASE WHEN s.tenantId = :tenantId THEN 1 ELSE 2 END
            LIMIT 1
            """)
    Optional<String> findByConfigKeyAndTenant(
            @Param("configKey") String configKey,
            @Param("tenantId") String tenantId);
}
