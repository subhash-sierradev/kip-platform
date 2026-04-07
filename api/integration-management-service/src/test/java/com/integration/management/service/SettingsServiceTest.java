package com.integration.management.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.execution.contract.model.enums.ConfigValueType;
import com.integration.management.entity.SiteConfig;
import com.integration.management.exception.CacheNotFoundException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.mapper.AuditLogMapper;
import com.integration.management.mapper.SiteConfigMapper;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.repository.SiteConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import com.integration.management.entity.AuditLog;
import com.integration.management.model.dto.response.AuditLogResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsService")
class SettingsServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SiteConfigRepository siteConfigRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private SiteConfigMapper siteConfigMapper;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private SettingsService settingsService;

    @Test
    @DisplayName("getAllAuditLogs should return mapped list for last 30 days")
    void getAllAuditLogs_withResults_returnsMappedList() {
        AuditLog log1 = AuditLog.builder().tenantId("tenant1").build();
        AuditLogResponse response1 = new AuditLogResponse();
        response1.setTenantId("tenant1");

        when(auditLogRepository.findByTenantIdAndTimestampAfterOrderByTimestampDesc(
                any(String.class), any(Instant.class)))
                .thenReturn(List.of(log1));
        when(auditLogMapper.toResponse(log1)).thenReturn(response1);

        List<AuditLogResponse> result = settingsService.getAllAuditLogs("tenant1");

        assertThat(result).hasSize(1).containsExactly(response1);
    }

    @Test
    @DisplayName("getAllAuditLogs should return empty list when no logs in last 30 days")
    void getAllAuditLogs_noLogs_returnsEmptyList() {
        when(auditLogRepository.findByTenantIdAndTimestampAfterOrderByTimestampDesc(
                any(String.class), any(Instant.class)))
                .thenReturn(List.of());

        List<AuditLogResponse> result = settingsService.getAllAuditLogs("tenant1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateSiteConfig should create tenant override when existing is GLOBAL")
    void updateSiteConfig_global_createsTenantOverride() {
        UUID id = UUID.randomUUID();
        SiteConfig global = SiteConfig.builder()
                .id(id)
                .tenantId("GLOBAL")
                .createdBy("sys")
                .lastModifiedBy("sys")
                .configKey("k")
                .configValue("false")
                .type(ConfigValueType.BOOLEAN)
                .description("d")
                .build();

        SiteConfigDto request = SiteConfigDto.builder()
                .id(id)
                .configKey("k")
                .configValue("true")
                .type(ConfigValueType.BOOLEAN)
                .build();

        SiteConfig saved = SiteConfig.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .configKey("k")
                .configValue("true")
                .type(ConfigValueType.BOOLEAN)
                .description("d")
                .build();

        SiteConfigDto mapped = SiteConfigDto.builder()
                .id(saved.getId())
                .tenantId("tenant1")
                .configKey("k")
                .configValue("true")
                .type(ConfigValueType.BOOLEAN)
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1"))
                .thenReturn(Optional.of(global));
        when(siteConfigRepository.save(any(SiteConfig.class))).thenReturn(saved);
        when(siteConfigMapper.toDto(saved)).thenReturn(mapped);

        SiteConfigDto result = settingsService.updateSiteConfig(id, request, "tenant1", "u");

        assertThat(result).isSameAs(mapped);
        verify(siteConfigRepository).save(any(SiteConfig.class));
    }

    @Test
    @DisplayName("updateSiteConfig should return null when invalid value on tenant update")
    void updateSiteConfig_tenant_invalid_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig tenant = SiteConfig.builder()
                .id(id)
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .configKey("k")
                .configValue("true")
                .type(ConfigValueType.NUMBER)
                .description("d")
                .build();

        SiteConfigDto request = SiteConfigDto.builder()
                .id(id)
                .configKey("k")
                .configValue("not-a-number")
                .type(ConfigValueType.NUMBER)
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1"))
                .thenReturn(Optional.of(tenant));

        SiteConfigDto result = settingsService.updateSiteConfig(id, request, "tenant1", "u2");

        assertThat(result).isNull();
        verify(siteConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSiteConfig should throw when invalid value on GLOBAL override create")
    void updateSiteConfig_global_invalid_throws() {
        UUID id = UUID.randomUUID();
        SiteConfig global = SiteConfig.builder()
                .id(id)
                .tenantId("GLOBAL")
                .createdBy("sys")
                .lastModifiedBy("sys")
                .configKey("k")
                .configValue("true")
                .type(ConfigValueType.TIMESTAMP)
                .description("d")
                .build();

        // Invalid ISO timestamp (triggers TIMESTAMP invalid branch)
        SiteConfigDto request = SiteConfigDto.builder()
                .id(id)
                .configKey("k")
                .configValue("not-a-timestamp")
                .type(ConfigValueType.TIMESTAMP)
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1"))
                .thenReturn(Optional.of(global));

        assertThatThrownBy(() -> settingsService.updateSiteConfig(id, request, "tenant1", "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid value");
    }

    @Test
    @DisplayName("updateSiteConfig should return null for STRING with empty value")
    void updateSiteConfig_tenant_invalidStringEmpty_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig tenant = SiteConfig.builder()
                .id(id)
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .configKey("k")
                .configValue("ok")
                .type(ConfigValueType.STRING)
                .build();

        SiteConfigDto request = SiteConfigDto.builder()
                .id(id)
                .configKey("k")
                .configValue("")
                .type(ConfigValueType.STRING)
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1"))
                .thenReturn(Optional.of(tenant));

        assertThat(settingsService.updateSiteConfig(id, request, "tenant1", "u2")).isNull();
        verify(siteConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSiteConfig should return null for BOOLEAN with null value")
    void updateSiteConfig_tenant_invalidBooleanNull_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig tenant = SiteConfig.builder()
                .id(id)
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .configKey("k")
                .configValue("true")
                .type(ConfigValueType.BOOLEAN)
                .build();

        SiteConfigDto request = SiteConfigDto.builder()
                .id(id)
                .configKey("k")
                .configValue(null)
                .type(ConfigValueType.BOOLEAN)
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1"))
                .thenReturn(Optional.of(tenant));

        assertThat(settingsService.updateSiteConfig(id, request, "tenant1", "u2")).isNull();
        verify(siteConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSiteConfig should return null for TIMESTAMP with empty value")
    void updateSiteConfig_tenant_invalidTimestampEmpty_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig tenant = SiteConfig.builder()
                .id(id)
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .configKey("k")
                .configValue("2025-01-01T00:00:00Z")
                .type(ConfigValueType.TIMESTAMP)
                .build();

        SiteConfigDto request = SiteConfigDto.builder()
                .id(id)
                .configKey("k")
                .configValue(" ")
                .type(ConfigValueType.TIMESTAMP)
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1"))
                .thenReturn(Optional.of(tenant));

        assertThat(settingsService.updateSiteConfig(id, request, "tenant1", "u2")).isNull();
        verify(siteConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllSiteConfigs should map repository results")
    void getAllSiteConfigs_mapsResults() {
        SiteConfig sc = SiteConfig.builder().configKey("k").configValue("v").tenantId("tenant1").build();
        SiteConfigDto dto = SiteConfigDto.builder().configKey("k").configValue("v").tenantId("tenant1").build();

        when(siteConfigRepository.findEffectiveConfigsByTenant("tenant1")).thenReturn(List.of(sc));
        when(siteConfigMapper.toDto(sc)).thenReturn(dto);

        assertThat(settingsService.getAllSiteConfigs("tenant1")).containsExactly(dto);
    }

    @Test
    @DisplayName("getSiteConfig should return mapped value when found")
    void getSiteConfig_found_returnsMapped() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("tenant1").configKey("k").configValue("v").build();
        SiteConfigDto dto = SiteConfigDto.builder().id(id).tenantId("tenant1").configKey("k").configValue("v")
                .build();

        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1")).thenReturn(Optional.of(sc));
        when(siteConfigMapper.toDto(sc)).thenReturn(dto);

        assertThat(settingsService.getSiteConfig(id, "tenant1")).isSameAs(dto);
    }

    @Test
    @DisplayName("getSiteConfig should throw when not found")
    void getSiteConfig_missing_throws() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "tenant1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.getSiteConfig(id, "tenant1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SiteConfig not found");
    }

    @Test
    @DisplayName("deleteSiteConfig should throw when deleting GLOBAL config")
    void deleteSiteConfig_global_throws() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("GLOBAL").configKey("k").build();
        when(siteConfigRepository.findByTenant(id, "tenant1")).thenReturn(Optional.of(sc));

        assertThatThrownBy(() -> settingsService.deleteSiteConfig(id, "tenant1", "user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be deleted");
        verify(siteConfigRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteSiteConfig should delete tenant config")
    void deleteSiteConfig_tenant_deletes() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("tenant1").configKey("k").build();
        when(siteConfigRepository.findByTenant(id, "tenant1")).thenReturn(Optional.of(sc));

        settingsService.deleteSiteConfig(id, "tenant1", "user");

        verify(siteConfigRepository).delete(sc);
    }

    @Test
    @DisplayName("getSiteConfigKeyById should throw IntegrationNotFoundException when missing")
    void getSiteConfigKeyById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findConfigKeyById(id, "tenant1")).thenReturn(null);

        assertThatThrownBy(() -> settingsService.getSiteConfigKeyById(id, "tenant1"))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("getConfigValueAsTimestamp should return Optional.empty for invalid timestamp")
    void getConfigValueAsTimestamp_invalid_returnsEmpty() {
        when(siteConfigRepository.findByConfigKeyAndTenant("k", "tenant1"))
                .thenReturn(Optional.of("not-a-ts"));

        assertThat(settingsService.getConfigValueAsTimestamp("k", "tenant1")).isEmpty();
    }

    @Test
    @DisplayName("getConfigValueAsTimestamp should return parsed Instant for valid timestamp")
    void getConfigValueAsTimestamp_valid_returnsInstant() {
        when(siteConfigRepository.findByConfigKeyAndTenant("k", "tenant1"))
                .thenReturn(Optional.of("2025-01-01T00:00:00Z"));

        assertThat(settingsService.getConfigValueAsTimestamp("k", "tenant1"))
                .contains(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("clearAllCaches clears each configured cache")
    void clearAllCaches_clearsEachCache() {
        Cache c1 = org.mockito.Mockito.mock(Cache.class);
        Cache c2 = org.mockito.Mockito.mock(Cache.class);

        when(cacheManager.getCacheNames()).thenReturn(Set.of("c1", "c2"));
        when(cacheManager.getCache("c1")).thenReturn(c1);
        when(cacheManager.getCache("c2")).thenReturn(c2);

        settingsService.clearAllCaches();

        verify(c1).clear();
        verify(c2).clear();
    }

    @Test
    @DisplayName("clearCacheByName should throw CacheNotFoundException when cache missing")
    void clearCacheByName_missing_throws() {
        when(cacheManager.getCache("missing")).thenReturn(null);

        assertThatThrownBy(() -> settingsService.clearCacheByName("missing"))
                .isInstanceOf(CacheNotFoundException.class);
    }

    @Test
    @DisplayName("getCacheStats should return map for Caffeine cache")
    void getCacheStats_caffeine_returnsMap() {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = Caffeine.newBuilder()
                .recordStats()
                .build();
        nativeCache.put("k", "v");

        CaffeineCache caffeineCache = new CaffeineCache("c1", nativeCache, false);
        when(cacheManager.getCache("c1")).thenReturn(caffeineCache);

        Object result = settingsService.getCacheStats("c1");

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertThat(map).containsKeys("hitRate", "missRate", "requestCount", "evictionCount", "size");
    }

    @Test
    @DisplayName("getCacheStats should return message for non-Caffeine cache")
    void getCacheStats_nonCaffeine_returnsMessage() {
        Cache cache = new ConcurrentMapCache("m1");
        when(cacheManager.getCache("m1")).thenReturn(cache);

        Object result = settingsService.getCacheStats("m1");

        assertThat(result).isInstanceOf(String.class);
        assertThat((String) result).contains("Statistics not available");
    }

    @Test
    @DisplayName("getCacheStatsForAllCaches returns stats for caffeine caches and message otherwise")
    void getCacheStatsForAllCaches_mixedCaches() {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = Caffeine.newBuilder()
                .recordStats()
                .build();
        nativeCache.getIfPresent("missing"); // record a miss

        CaffeineCache caffeineCache = new CaffeineCache("c1", nativeCache, false);
        Cache otherCache = new ConcurrentMapCache("m1");

        when(cacheManager.getCacheNames()).thenReturn(Set.of("c1", "m1"));
        when(cacheManager.getCache("c1")).thenReturn(caffeineCache);
        when(cacheManager.getCache("m1")).thenReturn(otherCache);

        Map<String, Object> result = settingsService.getCacheStatsForAllCaches();

        assertThat(result).containsKeys("c1", "m1");
        assertThat(result.get("c1")).isInstanceOf(Map.class);
        assertThat(result.get("m1")).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("updateSiteConfig: valid BOOLEAN 'false' on tenant saves successfully")
    void updateSiteConfig_tenant_validBooleanFalse_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("t1").createdBy("u")
                .lastModifiedBy("u").configKey("k").configValue("true")
                .type(ConfigValueType.BOOLEAN).build();
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue("false").type(ConfigValueType.BOOLEAN).build();
        SiteConfigDto mapped = SiteConfigDto.builder().id(id).configValue("false").build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.of(sc));
        when(siteConfigRepository.save(any(SiteConfig.class))).thenReturn(sc);
        when(siteConfigMapper.toDto(sc)).thenReturn(mapped);
        assertThat(settingsService.updateSiteConfig(id, req, "t1", "u")).isSameAs(mapped);
    }

    @Test
    @DisplayName("updateSiteConfig: valid NUMBER saves successfully")
    void updateSiteConfig_tenant_validNumber_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("t1").createdBy("u")
                .lastModifiedBy("u").configKey("k").configValue("1.0")
                .type(ConfigValueType.NUMBER).build();
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue("42.5").type(ConfigValueType.NUMBER).build();
        SiteConfigDto mapped = SiteConfigDto.builder().id(id).configValue("42.5").build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.of(sc));
        when(siteConfigRepository.save(any(SiteConfig.class))).thenReturn(sc);
        when(siteConfigMapper.toDto(sc)).thenReturn(mapped);
        assertThat(settingsService.updateSiteConfig(id, req, "t1", "u")).isSameAs(mapped);
    }

    @Test
    @DisplayName("updateSiteConfig: null NUMBER value returns null (invalid)")
    void updateSiteConfig_tenant_nullNumber_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("t1").createdBy("u")
                .lastModifiedBy("u").configKey("k").configValue("1")
                .type(ConfigValueType.NUMBER).build();
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue(null).type(ConfigValueType.NUMBER).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.of(sc));
        assertThat(settingsService.updateSiteConfig(id, req, "t1", "u")).isNull();
    }

    @Test
    @DisplayName("updateSiteConfig: valid TIMESTAMP saves successfully")
    void updateSiteConfig_tenant_validTimestamp_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("t1").createdBy("u")
                .lastModifiedBy("u").configKey("k").configValue("2025-01-01T00:00:00Z")
                .type(ConfigValueType.TIMESTAMP).build();
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue("2026-01-01T00:00:00Z").type(ConfigValueType.TIMESTAMP).build();
        SiteConfigDto mapped = SiteConfigDto.builder().id(id).configValue("2026-01-01T00:00:00Z").build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.of(sc));
        when(siteConfigRepository.save(any(SiteConfig.class))).thenReturn(sc);
        when(siteConfigMapper.toDto(sc)).thenReturn(mapped);
        assertThat(settingsService.updateSiteConfig(id, req, "t1", "u")).isSameAs(mapped);
    }

    @Test
    @DisplayName("updateSiteConfig: invalid TIMESTAMP on tenant returns null")
    void updateSiteConfig_tenant_invalidTimestampBadValue_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("t1").createdBy("u")
                .lastModifiedBy("u").configKey("k").configValue("2025-01-01T00:00:00Z")
                .type(ConfigValueType.TIMESTAMP).build();
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue("bad-ts").type(ConfigValueType.TIMESTAMP).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.of(sc));
        assertThat(settingsService.updateSiteConfig(id, req, "t1", "u")).isNull();
    }

    @Test
    @DisplayName("updateSiteConfig: valid STRING saves successfully")
    void updateSiteConfig_tenant_validString_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig sc = SiteConfig.builder().id(id).tenantId("t1").createdBy("u")
                .lastModifiedBy("u").configKey("k").configValue("old")
                .type(ConfigValueType.STRING).build();
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue("newVal").type(ConfigValueType.STRING).build();
        SiteConfigDto mapped = SiteConfigDto.builder().id(id).configValue("newVal").build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.of(sc));
        when(siteConfigRepository.save(any(SiteConfig.class))).thenReturn(sc);
        when(siteConfigMapper.toDto(sc)).thenReturn(mapped);
        assertThat(settingsService.updateSiteConfig(id, req, "t1", "u")).isSameAs(mapped);
    }

    @Test
    @DisplayName("updateSiteConfig: throws when config not found")
    void updateSiteConfig_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1")).thenReturn(Optional.empty());
        SiteConfigDto req = SiteConfigDto.builder().id(id).configKey("k")
                .configValue("v").type(ConfigValueType.STRING).build();
        assertThatThrownBy(() -> settingsService.updateSiteConfig(id, req, "t1", "u"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getConfigValueAsTimestamp returns empty Optional when key absent")
    void getConfigValueAsTimestamp_absent_returnsEmpty() {
        when(siteConfigRepository.findByConfigKeyAndTenant("key", "t1")).thenReturn(Optional.empty());
        assertThat(settingsService.getConfigValueAsTimestamp("key", "t1")).isEmpty();
    }

    @Test
    @DisplayName("getSiteConfigKeyById returns key when present")
    void getSiteConfigKeyById_present_returnsKey() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findConfigKeyById(id, "t1")).thenReturn("my-key");
        assertThat(settingsService.getSiteConfigKeyById(id, "t1")).isEqualTo("my-key");
    }

    @Test
    @DisplayName("deleteSiteConfig throws RuntimeException when config not found")
    void deleteSiteConfig_notFoundInRepo_throws() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findByTenant(id, "t1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> settingsService.deleteSiteConfig(id, "t1", "u"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("not found");
    }
}
