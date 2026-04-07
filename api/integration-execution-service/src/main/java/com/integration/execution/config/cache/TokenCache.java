package com.integration.execution.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TokenCache {

    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    private final Cache<String, TokenEntry> caffeineTokenCache;

    public String getValidToken(String cacheKey) {
        TokenEntry entry = caffeineTokenCache.getIfPresent(cacheKey);
        return entry != null ? entry.token() : null;
    }

    public void store(String cacheKey, String token, Long ttlSeconds) {
        if (token == null || token.isBlank()) {
            return;
        }
        long effectiveTtl =
                (ttlSeconds != null && ttlSeconds > 0)
                        ? ttlSeconds
                        : DEFAULT_TTL_SECONDS;
        Instant expiresAt = Instant.now().plusSeconds(effectiveTtl);

        caffeineTokenCache.put(cacheKey, new TokenEntry(token, expiresAt));
    }

    public void invalidate(String cacheKey) {
        caffeineTokenCache.invalidate(cacheKey);
    }
}
