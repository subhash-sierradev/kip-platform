package com.integration.translation.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationCacheConfigTest {

    private final TranslationCacheConfig config = new TranslationCacheConfig();

    @Test
    @DisplayName("cacheManager() returns a non-null CacheManager")
    void cacheManager_returnsNonNull() {
        assertThat(config.cacheManager()).isNotNull();
    }

    @Test
    @DisplayName("cacheManager() exposes the 'translations' cache")
    void cacheManager_exposesTranslationsCache() {
        CacheManager manager = config.cacheManager();
        assertThat(manager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).isNotNull();
    }

    @Test
    @DisplayName("cacheManager() exposes the 'ollama-health' cache")
    void cacheManager_exposesOllamaHealthCache() {
        CacheManager manager = config.cacheManager();
        assertThat(manager.getCache(TranslationCacheConfig.CACHE_OLLAMA_HEALTH)).isNotNull();
    }

    @Test
    @DisplayName("CACHE_TRANSLATIONS constant matches expected value")
    void cacheTranslationsConstant_hasExpectedValue() {
        assertThat(TranslationCacheConfig.CACHE_TRANSLATIONS).isEqualTo("translations");
    }

    @Test
    @DisplayName("CACHE_OLLAMA_HEALTH constant matches expected value")
    void cacheOllamaHealthConstant_hasExpectedValue() {
        assertThat(TranslationCacheConfig.CACHE_OLLAMA_HEALTH).isEqualTo("ollama-health");
    }

    @Test
    @DisplayName("KEY_GENERATOR constant matches the Spring bean name used in @Cacheable")
    void keyGeneratorConstant_matchesBeanName() {
        assertThat(TranslationCacheConfig.KEY_GENERATOR)
                .isEqualTo("translationCacheKeyGenerator");
    }
}

