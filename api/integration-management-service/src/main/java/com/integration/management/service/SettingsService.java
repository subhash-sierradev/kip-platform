package com.integration.management.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.management.exception.CacheNotFoundException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.mapper.AuditLogMapper;
import com.integration.management.mapper.SiteConfigMapper;
import com.integration.management.model.dto.response.AuditLogResponse;
import com.integration.management.entity.SiteConfig;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.notification.aop.PublishNotification;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.repository.SiteConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.GLOBAL;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {
    private final AuditLogRepository auditLogRepository;
    private final SiteConfigRepository siteConfigRepository;
    private final AuditLogMapper auditLogMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final CacheManager cacheManager;

    @PreAuthorize("hasRole('tenant_admin')")
    public List<AuditLogResponse> getAllAuditLogs(String tenantId) {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        return auditLogRepository.findByTenantIdAndTimestampAfterOrderByTimestampDesc(tenantId, cutoff).stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Cacheable(value = "siteConfigAllCache", key = "#tenantId")
    public List<SiteConfigDto> getAllSiteConfigs(String tenantId) {
        log.debug("Fetching all site configs from database for tenant: {}", tenantId);
        return siteConfigRepository.findEffectiveConfigsByTenant(tenantId)
                .stream()
                .map(siteConfigMapper::toDto)
                .toList();
    }

    @PublishNotification(
            eventKey = NotificationEventKey.SITE_CONFIG_UPDATED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadata = "#result != null ? {'configKey': #result.configKey} : {}")
    @Caching(evict = {
        @CacheEvict(value = "siteConfigAllCache", key = "#tenantId"),
        @CacheEvict(value = "siteConfigByIdCache", key = "#id + '_' + #tenantId"),
        @CacheEvict(value = "siteConfigByKeyCache", allEntries = true)
    })
    public SiteConfigDto updateSiteConfig(UUID id, @Valid SiteConfigDto request, String tenantId, String userId) {
        log.info("Updating site config id: {} for tenant: {}, evicting caches", id, tenantId);
        // Find the config to update
        SiteConfig existing = siteConfigRepository.findByTenantWithGlobalFallback(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Site config not found with id: " + id));
        if (existing.getTenantId().equals(GLOBAL)) {
            // Create a new tenant-specific config overriding the global one
            SiteConfig tenantConfig = SiteConfig.builder()
                    .configKey(existing.getConfigKey())
                    .configValue(request.getConfigValue())
                    .description(existing.getDescription())
                    .type(existing.getType())
                    .tenantId(tenantId)
                    .createdBy(userId)
                    .lastModifiedBy(userId)
                    .build();
            if (isInvalidValue(tenantConfig)) {
                throw new IllegalStateException("Attempted to create SiteConfig with invalid value: " + request);
            }
            return siteConfigMapper.toDto(siteConfigRepository.save(tenantConfig));
        } else {
            // Update existing tenant-specific config
            existing.setConfigValue(request.getConfigValue());
            existing.setLastModifiedBy(userId);
            if (isInvalidValue(existing)) {
                log.warn("Attempted to update SiteConfig with invalid value: {}", request);
                return null;
            }
            SiteConfigDto result = siteConfigMapper.toDto(siteConfigRepository.save(existing));
            return result;
        }
    }

    private boolean isInvalidValue(SiteConfig siteConfig) {
        String value = siteConfig.getConfigValue();
        switch (siteConfig.getType()) {
            case BOOLEAN:
                return value == null
                        || !(value.equalsIgnoreCase("true")
                                || value.equalsIgnoreCase("false"));

            case NUMBER:
                if (value == null)
                    return true;
                try {
                    Double.parseDouble(value);
                    return false;
                } catch (NumberFormatException e) {
                    return true;
                }
            case STRING:
                return value == null || value.isEmpty();
            case TIMESTAMP:
                if (value == null || value.isEmpty())
                    return true;
                try {
                    Instant.parse(value);
                    return false;
                } catch (DateTimeParseException e) {
                    log.warn("Invalid ISO-8601 timestamp format for config {}: {}",
                            siteConfig.getConfigKey(), value, e);
                    return true;
                }
            default:
                return false; // safety net (shouldn't happen)
        }
    }

    @Cacheable(value = "siteConfigByIdCache", key = "#id + '_' + #tenantId")
    public @Nullable SiteConfigDto getSiteConfig(UUID id, String tenantId) {
        log.debug("Fetching site config from database for id: {} and tenant: {}", id, tenantId);
        return siteConfigRepository.findByTenantWithGlobalFallback(id, tenantId)
                .map(siteConfigMapper::toDto)
                .orElseThrow(() -> new RuntimeException("SiteConfig not found with id: " + id));
    }

    @PublishNotification(
            eventKey = NotificationEventKey.SITE_CONFIG_UPDATED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadataProvider = "siteConfigNotificationMetadataProvider",
            entityId = "#id")
    @Caching(evict = {
        @CacheEvict(value = "siteConfigAllCache", key = "#tenantId"),
        @CacheEvict(value = "siteConfigByIdCache", key = "#id + '_' + #tenantId"),
        @CacheEvict(value = "siteConfigByKeyCache", allEntries = true)
    })
    public void deleteSiteConfig(UUID id, String tenantId, String userId) {
        log.info("Deleting site config id: {} for tenant: {}, evicting caches", id, tenantId);
        SiteConfig siteConfig = siteConfigRepository.findByTenant(id, tenantId)
                .orElseThrow(() -> new RuntimeException("SiteConfig not found with id: " + id));
        if (GLOBAL.equals(siteConfig.getTenantId())) {
            throw new IllegalStateException("GLOBAL SiteConfig cannot be deleted");
        }
        siteConfigRepository.delete(siteConfig);
    }

    @PreAuthorize("hasRole('tenant_admin')")
    public String getSiteConfigKeyById(UUID id, String tenantId) {
        String configKey = siteConfigRepository.findConfigKeyById(id, tenantId);
        if (configKey == null) {
            throw new IntegrationNotFoundException("SiteConfig key not found for id: " + id);
        }
        return configKey;
    }

    @Cacheable(value = "siteConfigByKeyCache", key = "#configKey + '_' + #tenantId")
    public Optional<Instant> getConfigValueAsTimestamp(String configKey, String tenantId) {
        log.debug("Fetching config value from database for key: {} and tenant: {}", configKey, tenantId);
        return siteConfigRepository.findByConfigKeyAndTenant(configKey, tenantId)
                .map(configValue -> {
                    try {
                        return Instant.parse(configValue);
                    } catch (DateTimeParseException e) {
                        log.error("Invalid timestamp format for config key '{}' with value '{}': {}",
                                configKey, configValue, e.getMessage());
                        return null;
                    }
                });
    }

    @PreAuthorize("hasRole('app_admin')")
    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = getCacheOrThrow(cacheName);
            cache.clear();
        });
    }

    @PreAuthorize("hasRole('app_admin')")
    public void clearCacheByName(String cacheName) {
        getCacheOrThrow(cacheName).clear();
    }

    @PreAuthorize("hasRole('app_admin')")
    public Object getCacheStats(String cacheName) {
        var cache = getCacheOrThrow(cacheName);
        if (cache instanceof CaffeineCache caffeineCache) {
            return getCacheStatsMap(caffeineCache);
        } else {
            return "Statistics not available for this cache type.";
        }
    }

    @PreAuthorize("hasRole('app_admin')")
    public Map<String, Object> getCacheStatsForAllCaches() {
        Map<String, Object> statsMap = new HashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = getCacheOrThrow(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                statsMap.put(cacheName, getCacheStatsMap(caffeineCache));
            } else {
                statsMap.put(cacheName, "Statistics not available for this cache type.");
            }
        }
        return statsMap;
    }

    private static Map<String, Object> getCacheStatsMap(CaffeineCache caffeineCache) {
        Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        CacheStats stats = nativeCache.stats();
        Map<String, Object> cacheStats = new HashMap<>();

        cacheStats.put("hitRate", stats.hitRate());
        cacheStats.put("missRate", stats.missRate());
        cacheStats.put("requestCount", stats.requestCount());
        cacheStats.put("evictionCount", stats.evictionCount());
        cacheStats.put("size", nativeCache.estimatedSize());
        return cacheStats;
    }

    private org.springframework.cache.Cache getCacheOrThrow(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new CacheNotFoundException(cacheName);
        }
        return cache;
    }
}
