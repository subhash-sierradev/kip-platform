package com.integration.execution.util;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JiraRateLimiter {

    @FunctionalInterface
    public interface JiraApiCall<T> {
        T execute() throws Exception;
    }

    private static final int MAX_CONCURRENT_CALLS = 3;
    private static final int CALLS_PER_SECOND = 2;           // 1 call per 500 ms
    private static final Duration MAX_WAIT = Duration.ofSeconds(10);

    private final ConcurrentHashMap<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public <T> T executeWithRateLimit(String jiraBaseUrl, JiraApiCall<T> call) throws Exception {
        String key = extractInstanceKey(jiraBaseUrl);
        Bulkhead bulkhead = getOrCreateBulkhead(key);
        RateLimiter rateLimiter = getOrCreateRateLimiter(key);

        try {
            return Bulkhead.decorateCallable(bulkhead,
                    RateLimiter.decorateCallable(rateLimiter, call::execute)).call();
        } catch (BulkheadFullException e) {
            log.warn("Bulkhead full for Jira instance [{}]: {}", key, e.getMessage());
            throw e;
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for Jira instance [{}]: {}", key, e.getMessage());
            throw e;
        }
    }

    private Bulkhead getOrCreateBulkhead(String key) {
        return bulkheads.computeIfAbsent(key, k -> Bulkhead.of(k,
                BulkheadConfig.custom()
                        .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
                        .maxWaitDuration(MAX_WAIT)
                        .build()));
    }

    private RateLimiter getOrCreateRateLimiter(String key) {
        return rateLimiters.computeIfAbsent(key, k -> RateLimiter.of(k,
                RateLimiterConfig.custom()
                        .limitForPeriod(CALLS_PER_SECOND)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(MAX_WAIT)
                        .build()));
    }

    private String extractInstanceKey(String jiraBaseUrl) {
        try {
            URI uri = new URI(jiraBaseUrl);
            String host = uri.getHost();
            if (host == null) {
                log.warn("Failed to extract host from Jira URL: {}, using full URL as key", jiraBaseUrl);
                return jiraBaseUrl.toLowerCase();
            }
            return host.toLowerCase();
        } catch (Exception e) {
            log.warn("Failed to parse Jira URL: {}, using full URL as key", jiraBaseUrl);
            return jiraBaseUrl.toLowerCase();
        }
    }
}
