package com.integration.translation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the Translation Service.
 *
 * <p>Uses <a href="https://github.com/ben-manes/caffeine">Caffeine</a> as the in-process
 * cache implementation.  Two caches are pre-configured:</p>
 *
 * <table border="1">
 *   <caption>Cache definitions</caption>
 *   <tr><th>Cache name</th><th>Purpose</th><th>Max entries</th><th>TTL</th></tr>
 *   <tr>
 *     <td>{@code translations}</td>
 *     <td>Stores completed translation results keyed by
 *         {@code (text, sourceLang, targetLang)}. Avoids redundant Ollama calls
 *         for repeated requests (e.g. translating the same Confluence template
 *         page multiple times during a session).</td>
 *     <td>2 000</td>
 *     <td>24 h after write</td>
 *   </tr>
 *   <tr>
 *     <td>{@code ollama-health}</td>
 *     <td>Short-lived flag caching the result of the Ollama ping so that the
 *         Spring Boot {@code /actuator/health} endpoint can answer quickly under
 *         steady-state load without hammering the sidecar.</td>
 *     <td>1</td>
 *     <td>30 s after write</td>
 *   </tr>
 * </table>
 *
 * <p>Both caches track per-entry statistics (hit ratio, miss count, eviction
 * count) which are printed in DEBUG logs and exported via Micrometer if the
 * actuator and metrics are present on the classpath.</p>
 */
@Configuration
public class TranslationCacheConfig {

    /** Cache name constant used by {@code @Cacheable} annotations in the service layer. */
    public static final String CACHE_TRANSLATIONS = "translations";

    /** Cache name constant used by {@link com.integration.translation.health.OllamaHealthIndicator}. */
    public static final String CACHE_OLLAMA_HEALTH = "ollama-health";

    /**
     * Returns the primary {@link CacheManager} backed by Caffeine.
     *
     * @return configured {@code CacheManager}
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(java.util.List.of(CACHE_TRANSLATIONS, CACHE_OLLAMA_HEALTH));
        manager.registerCustomCache(CACHE_TRANSLATIONS,
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .recordStats()
                        .build());
        manager.registerCustomCache(CACHE_OLLAMA_HEALTH,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .recordStats()
                        .build());
        return manager;
    }
}

