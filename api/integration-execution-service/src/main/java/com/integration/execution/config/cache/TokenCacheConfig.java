package com.integration.execution.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Configuration
public class TokenCacheConfig {

    private static final long EXPIRY_SKEW_SECONDS = 30; // seconds to subtract from actual expiry time
    private static final long MAX_CACHE_SIZE = 1_000; // maximum number of entries in the cache

    @Bean
    public Cache<String, TokenEntry> caffeineTokenCache() {

        return Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfter(new Expiry<String, TokenEntry>() {

                    @Override
                    public long expireAfterCreate(
                            String key,
                            TokenEntry value,
                            long currentTime) {

                        Instant expiresAt = value.expiresAt().minusSeconds(EXPIRY_SKEW_SECONDS);
                        Duration ttl = Duration.between(Instant.now(), expiresAt);
                        long ttlNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(ttl.toMillis(), 0));

                        // return duration in nanoseconds (not an absolute timestamp)
                        return ttlNanos;
                    }

                    @Override
                    public long expireAfterUpdate(
                            String key,
                            TokenEntry value,
                            long currentTime,
                            long currentDuration) {

                        // keep existing expiration
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(
                            String key,
                            TokenEntry value,
                            long currentTime,
                            long currentDuration) {

                        // do NOT extend TTL on read
                        return currentDuration;
                    }
                })
                .build();
    }
}