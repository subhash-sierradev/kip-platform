package com.integration.execution.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TokenCacheConfigTest {

    private final TokenCacheConfig config = new TokenCacheConfig();

    @Test
    void caffeineTokenCache_returnsNonNullCache() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();

        assertThat(cache).isNotNull();
    }

    @Test
    void caffeineTokenCache_hasMaximumSizeOf1000() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();

        assertThat(cache.policy().eviction()).isPresent();
        assertThat(cache.policy().eviction().get().getMaximum()).isEqualTo(1_000L);
    }

    @Test
    void caffeineTokenCache_usesVariableExpiry() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();

        assertThat(cache.policy().expireVariably()).isPresent();
    }

    @Test
    void expireAfterCreate_withFutureExpiresAt_assignsPositiveNanoseconds() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();
        TokenEntry entry = new TokenEntry("my-token", Instant.now().plusSeconds(600));

        cache.put("tenant:future-key", entry);

        long remainingNanos = cache.policy().expireVariably().get()
                .getExpiresAfter("tenant:future-key", TimeUnit.NANOSECONDS)
                .orElse(-1L);
        assertThat(remainingNanos).isGreaterThan(0L);
    }

    @Test
    void expireAfterCreate_withPastExpiresAt_evictsEntryOnCleanup() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();
        // expiresAt in the past => TTL = max(negative, 0) = 0 nanoseconds => immediately expired
        TokenEntry entry = new TokenEntry("my-token", Instant.now().minusSeconds(120));

        cache.put("tenant:expired-key", entry);
        cache.cleanUp();

        assertThat(cache.getIfPresent("tenant:expired-key")).isNull();
    }

    @Test
    void expireAfterCreate_withExpiresAtWithinSkew_floorsToZero() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();
        // 15 seconds from now is within the 30s skew window => effectively expired
        TokenEntry entry = new TokenEntry("my-token", Instant.now().plusSeconds(15));

        cache.put("tenant:within-skew", entry);
        cache.cleanUp();

        assertThat(cache.getIfPresent("tenant:within-skew")).isNull();
    }

    @Test
    void expireAfterRead_doesNotExtendTtl() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();
        TokenEntry entry = new TokenEntry("my-token", Instant.now().plusSeconds(300));
        cache.put("tenant:read-test", entry);

        long ttlBeforeRead = cache.policy().expireVariably().get()
                .getExpiresAfter("tenant:read-test", TimeUnit.NANOSECONDS)
                .orElse(-1L);

        // Read the value
        cache.getIfPresent("tenant:read-test");

        long ttlAfterRead = cache.policy().expireVariably().get()
                .getExpiresAfter("tenant:read-test", TimeUnit.NANOSECONDS)
                .orElse(-1L);

        // TTL should not have increased (no sliding expiry on read)
        assertThat(ttlAfterRead).isLessThanOrEqualTo(ttlBeforeRead);
    }

    @Test
    void expireAfterUpdate_doesNotResetTtl() {
        Cache<String, TokenEntry> cache = config.caffeineTokenCache();
        TokenEntry original = new TokenEntry("token-v1", Instant.now().plusSeconds(300));
        cache.put("tenant:update-test", original);

        long ttlBeforeUpdate = cache.policy().expireVariably().get()
                .getExpiresAfter("tenant:update-test", TimeUnit.NANOSECONDS)
                .orElse(-1L);

        // Simulate an update via cache.put (triggers expireAfterUpdate callback)
        TokenEntry updated = new TokenEntry("token-v2", Instant.now().plusSeconds(600));
        cache.put("tenant:update-test", updated);

        long ttlAfterUpdate = cache.policy().expireVariably().get()
                .getExpiresAfter("tenant:update-test", TimeUnit.NANOSECONDS)
                .orElse(-1L);

        // TTL should still be based on the original expiry (currentDuration preserved)
        assertThat(ttlAfterUpdate).isLessThanOrEqualTo(ttlBeforeUpdate);
    }
}
