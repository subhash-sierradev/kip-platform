package com.integration.management.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheConfig")
class CacheConfigTest {

    @Test
    @DisplayName("cacheManager should expose expected cache names")
    void cacheManager_containsExpectedCaches() {
        CacheConfig config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        assertThat(cacheManager).isInstanceOf(SimpleCacheManager.class);
        ((SimpleCacheManager) cacheManager).afterPropertiesSet();

        Set<String> names = new HashSet<>(cacheManager.getCacheNames());
        assertThat(names).contains(
                "credentialTypesCache",
                "jiraWebhookNormalizedNamesByTenantCache",
                "kwDynamicDocTypeCache",
                "siteConfigAllCache");
    }
}
