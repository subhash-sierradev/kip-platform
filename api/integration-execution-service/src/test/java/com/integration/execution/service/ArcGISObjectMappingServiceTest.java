package com.integration.execution.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ArcGISObjectMappingServiceTest.TestConfig.class)
class ArcGISObjectMappingServiceTest {

    private static final String CACHE_NAME = "arcgisObjectMappingCache";

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CACHE_NAME);
        }

        @Bean
        ArcGISObjectMappingService arcGISObjectMappingService() {
            return Mockito.spy(new ArcGISObjectMappingService());
        }
    }

    @Autowired
    private ArcGISObjectMappingService service;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void tearDown() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }

        ArcGISObjectMappingService target = AopTestUtils.getTargetObject(service);
        Mockito.reset(target);
    }

    @Test
    void getMapping_cacheMiss_returnsNull_andDoesNotCacheNull() {
        String externalLocationId = "ext-loc-1";
        String urlHash = "0123456789abcdef";
        String cacheKey = externalLocationId + "::" + urlHash;

        Long result = service.getMapping(externalLocationId, urlHash);

        assertThat(result).isNull();

        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(cacheKey)).isNull();

        ArcGISObjectMappingService target = AopTestUtils.getTargetObject(service);
        verify(target, times(1)).getMapping(externalLocationId, urlHash);
    }

    @Test
    void putMapping_populatesCache_andGetMapping_hitsCache_withoutCallingTargetMethod() {
        String externalLocationId = "ext-loc-2";
        String urlHash = "fedcba9876543210";
        String cacheKey = externalLocationId + "::" + urlHash;
        Long objectId = 12345L;

        Long putResult = service.putMapping(externalLocationId, urlHash, objectId);

        assertThat(putResult).isEqualTo(objectId);

        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(cacheKey, Long.class)).isEqualTo(objectId);

        Long getResult = service.getMapping(externalLocationId, urlHash);

        assertThat(getResult).isEqualTo(objectId);

        ArcGISObjectMappingService target = AopTestUtils.getTargetObject(service);
        verify(target, never()).getMapping(externalLocationId, urlHash);
    }

    @Test
    void putMapping_overwritesExistingEntry_forSameKey() {
        String externalLocationId = "ext-loc-3";
        String urlHash = "aaaaaaaaaaaaaaaa";
        String cacheKey = externalLocationId + "::" + urlHash;

        service.putMapping(externalLocationId, urlHash, 111L);
        service.putMapping(externalLocationId, urlHash, 222L);

        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(cacheKey, Long.class)).isEqualTo(222L);

        assertThat(service.getMapping(externalLocationId, urlHash)).isEqualTo(222L);
    }
}
