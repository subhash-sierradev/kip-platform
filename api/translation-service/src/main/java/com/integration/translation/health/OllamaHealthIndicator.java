package com.integration.translation.health;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.config.TranslationCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Spring Boot {@link HealthIndicator} that reports the reachability of the
 * Ollama sidecar container as part of the {@code GET /actuator/health} response.
 *
 * <p>The liveness check is deliberately lightweight — it pings Ollama's root
 * endpoint ({@code GET /}) and caches the result for 30 seconds (see
 * {@link TranslationCacheConfig#CACHE_OLLAMA_HEALTH}).  This prevents the
 * health endpoint from flooding Ollama with requests under high polling
 * frequency from a Kubernetes readiness probe or monitoring tool.</p>
 *
 * <h3>Sample output when healthy</h3>
 * <pre>{@code
 * GET /actuator/health
 * {
 *   "status": "UP",
 *   "components": {
 *     "ollama": {
 *       "status": "UP",
 *       "details": { "baseUrl": "http://ollama:11434", "model": "mistral" }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>Sample output when Ollama is down</h3>
 * <pre>{@code
 * "ollama": { "status": "DOWN", "details": { "error": "Connection refused" } }
 * }</pre>
 */
@Slf4j
@Component("ollama")
@RequiredArgsConstructor
public class OllamaHealthIndicator implements HealthIndicator {

    private final OllamaClient ollamaClient;

    /**
     * Checks whether the Ollama server is reachable and returns a
     * {@link Health} status accordingly.
     *
     * <p>Results are cached for 30 seconds by the
     * {@code ollama-health} Caffeine cache to minimise sidecar load.</p>
     *
     * @return {@code Health.up()} if Ollama responds; {@code Health.down()} otherwise
     */
    @Override
    @Cacheable(cacheNames = TranslationCacheConfig.CACHE_OLLAMA_HEALTH, key = "'ping'")
    public Health health() {
        try {
            boolean reachable = ollamaClient.isReachable();
            if (reachable) {
                log.debug("Ollama health check: UP");
                return Health.up().build();
            }
            log.warn("Ollama health check: DOWN (not reachable)");
            return Health.down()
                    .withDetail("error", "Ollama did not respond to health ping")
                    .build();
        } catch (Exception ex) {
            log.warn("Ollama health check: DOWN — {}", ex.getMessage());
            return Health.down()
                    .withException(ex)
                    .build();
        }
    }
}


