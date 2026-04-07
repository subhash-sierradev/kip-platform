package com.integration.execution.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenCacheTest {

    private TokenCache tokenCache;

    @BeforeEach
    void setUp() {
        Cache<String, TokenEntry> cache = Caffeine.newBuilder().build();
        tokenCache = new TokenCache(cache);
    }

    @Test
    void store_validTokenAndPositiveTtl_savesToken() {
        tokenCache.store("tenant-a:secret", "token-value", 60L);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isEqualTo("token-value");
    }

    @Test
    void store_blankToken_doesNotCacheToken() {
        tokenCache.store("tenant-a:secret", "   ", 60L);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isNull();
    }

    @Test
    void store_nullToken_doesNotCacheToken() {
        tokenCache.store("tenant-a:secret", null, 60L);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isNull();
    }

    @Test
    void store_nullTtl_usesDefaultAndCachesToken() {
        tokenCache.store("tenant-a:secret", "token-value", null);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isEqualTo("token-value");
    }

    @Test
    void store_zeroTtl_usesDefaultAndCachesToken() {
        tokenCache.store("tenant-a:secret", "token-value", 0L);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isEqualTo("token-value");
    }

    @Test
    void store_negativeTtl_usesDefaultAndCachesToken() {
        tokenCache.store("tenant-a:secret", "token-value", -10L);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isEqualTo("token-value");
    }

    @Test
    void getValidToken_forUnknownKey_returnsNull() {
        assertThat(tokenCache.getValidToken("nonexistent:key")).isNull();
    }

    @Test
    void invalidate_existingToken_removesTokenFromCache() {
        tokenCache.store("tenant-a:secret", "token-value", 60L);

        tokenCache.invalidate("tenant-a:secret");

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isNull();
    }

    @Test
    void store_multipleTenants_isolatesEntries() {
        tokenCache.store("tenant-a:secret", "token-a", 60L);
        tokenCache.store("tenant-b:secret", "token-b", 60L);

        assertThat(tokenCache.getValidToken("tenant-a:secret")).isEqualTo("token-a");
        assertThat(tokenCache.getValidToken("tenant-b:secret")).isEqualTo("token-b");
    }
}
