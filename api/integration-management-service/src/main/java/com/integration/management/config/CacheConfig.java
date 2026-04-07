package com.integration.management.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = new ArrayList<>();
        caches.addAll(createMasterDataCaches());
        caches.addAll(createJiraIntegrationCaches());
        caches.addAll(createIntegrationConnectionCaches());
        caches.addAll(createArcGISCaches());
        caches.addAll(createConfluenceCaches());
        caches.addAll(createKwCaches());
        caches.addAll(createSiteConfigCaches());
        caches.addAll(createNotificationCaches());
        caches.addAll(createSecurityCaches());
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private List<CaffeineCache> createMasterDataCaches() {
        return List.of(
                // Long-term cache for since they rarely change
                createCache("credentialsCache", 5, 24, TimeUnit.HOURS),
                createCache("credentialTypesCache", 10, 24, TimeUnit.HOURS),
                createCache("languagesCache", 200, 24, TimeUnit.HOURS),

                createCache("allUserProfileCache", 500, 5, TimeUnit.MINUTES),
                createCache("userProfileMapByTenantCache", 500, 5, TimeUnit.MINUTES),
                createCache("userProfileCache", 500, 5, TimeUnit.MINUTES),
                createCache("tenantProfileCache", 100, 24, TimeUnit.HOURS)
        );
    }

    private List<CaffeineCache> createJiraIntegrationCaches() {
        return List.of(
                // Connection-based caches (longer duration for performance)
                createCache("jiraProjectsByConnectionCache", 100, 60, TimeUnit.MINUTES),
                createCache("jiraUsersByConnectionCache", 100, 60, TimeUnit.MINUTES),
                createCache("jiraIssueTypesByConnectionCache", 100, 60, TimeUnit.MINUTES),
                createCache("jiraFieldsByConnectionCache", 500, 60, TimeUnit.MINUTES),
                createCache("jiraFieldDetailsByConnectionCache", 1000, 60, TimeUnit.MINUTES),
                createCache("jiraTeamsByConnectionCache", 200, 60, TimeUnit.MINUTES),
                createCache("jiraSprintsByConnectionCache", 200, 60, TimeUnit.MINUTES),
                createCache("jiraWebhookNormalizedNamesByTenantCache", 50, 5, TimeUnit.MINUTES),
                createCache("jiraWebhookNamesCache", 100, 30, TimeUnit.MINUTES),
                createCache("jiraProjectMetaFieldsByConnectionCache", 1000, 30, TimeUnit.MINUTES),
                createCache("jiraParentIssuesByConnectionCache", 1000, 30, TimeUnit.MINUTES)
        );
    }

    private List<CaffeineCache> createIntegrationConnectionCaches() {
        return List.of(
                createCache("integrationConnectionNamesCache", 100, 24, TimeUnit.HOURS),
                createCache("integrationConnectionCache", 200, 30, TimeUnit.MINUTES)
        );
    }

    private List<CaffeineCache> createArcGISCaches() {
        return List.of(
                createCache("arcgisFeaturesCache", 20, 60, TimeUnit.MINUTES),
                createCache("arcgisNormalizedNamesByTenantCache", 50, 5, TimeUnit.MINUTES)
        );
    }

    private List<CaffeineCache> createConfluenceCaches() {
        return List.of(
                // Confluence spaces and pages — 5 minute TTL (external API data)
                createCache("confluenceSpacesByConnectionCache", 100, 5, TimeUnit.MINUTES),
                createCache("confluencePagesByConnectionCache", 200, 5, TimeUnit.MINUTES)
        );
    }

    private List<CaffeineCache> createKwCaches() {
        return List.of(
                createCache("kwDynamicDocTypeCache", 10, 10, TimeUnit.MINUTES),
                createCache("kwItemSubtypesCache", 5, 24, TimeUnit.HOURS),
                createCache("kwDocFieldsCache", 5, 24, TimeUnit.HOURS),
                createCache("kwLocationFieldMappingCache", 5, 24, TimeUnit.HOURS)
        );
    }

    private List<CaffeineCache> createSiteConfigCaches() {
        return List.of(
                // Site configuration caches - 10 minute TTL for frequently accessed settings
                createCache("siteConfigAllCache", 100, 10, TimeUnit.MINUTES),
                createCache("siteConfigByIdCache", 200, 10, TimeUnit.MINUTES),
                createCache("siteConfigByKeyCache", 200, 10, TimeUnit.MINUTES)
        );
    }

    private List<CaffeineCache> createNotificationCaches() {
        return List.of(
                // Event catalog and templates are read-only — 24h TTL
                createCache("notificationEventsByTenantCache", 50, 24, TimeUnit.HOURS),
                createCache("notificationTemplatesByTenantCache", 100, 24, TimeUnit.HOURS),
                // Rules and policies mutate — evict on write, 5 min safety TTL
                createCache("notificationRulesByTenantCache", 100, 5, TimeUnit.MINUTES),
                createCache("notificationPoliciesByTenantCache", 100, 5, TimeUnit.MINUTES)
        );
    }

    private List<CaffeineCache> createSecurityCaches() {
        return List.of(
                // IP validation cache - high traffic, 2h TTL for IP address changes
                // Only caches VALID IPs to prevent cache pollution from attacks
                // Max 10,000 entries (typical deployments see < 1,000 unique IPs)
                createCache("ipValidationCache", 10_000, 2, TimeUnit.HOURS)
        );
    }

    private CaffeineCache createCache(String name, int maxSize, long duration, TimeUnit timeUnit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(duration, timeUnit)
                .recordStats()
                .build());
    }
}
