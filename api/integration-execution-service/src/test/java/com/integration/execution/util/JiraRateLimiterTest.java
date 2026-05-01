package com.integration.execution.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiterTest {

    private JiraRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new JiraRateLimiter();
    }

    @Test
    void executeWithRateLimit_successfulCall_returnsResult() throws Exception {
        String result = rateLimiter.executeWithRateLimit(
                "https://mycompany.atlassian.net",
                () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void executeWithRateLimit_sameBaseUrl_reusesBulkheadAndRateLimiter() throws Exception {
        String url = "https://mycompany.atlassian.net";

        String first = rateLimiter.executeWithRateLimit(url, () -> "first");
        String second = rateLimiter.executeWithRateLimit(url, () -> "second");

        assertThat(first).isEqualTo("first");
        assertThat(second).isEqualTo("second");
    }

    @Test
    void executeWithRateLimit_differentBaseUrls_usesSeparateLimiters() throws Exception {
        String result1 = rateLimiter.executeWithRateLimit(
                "https://company-a.atlassian.net", () -> "a");
        String result2 = rateLimiter.executeWithRateLimit(
                "https://company-b.atlassian.net", () -> "b");

        assertThat(result1).isEqualTo("a");
        assertThat(result2).isEqualTo("b");
    }

    @Test
    void executeWithRateLimit_checkedExceptionFromCall_propagatesWrapped() {
        assertThatThrownBy(() -> rateLimiter.executeWithRateLimit(
                "https://company.atlassian.net",
                () -> {
                    throw new Exception("IO failure");
                }))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("IO failure");
    }

    @Test
    void executeWithRateLimit_runtimeExceptionFromCall_propagates() {
        assertThatThrownBy(() -> rateLimiter.executeWithRateLimit(
                "https://company.atlassian.net",
                () -> {
                    throw new RuntimeException("upstream error");
                }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("upstream error");
    }

    @Test
    void executeWithRateLimit_invalidUrl_usesFullUrlAsKey() throws Exception {
        String result = rateLimiter.executeWithRateLimit("not-a-valid-url", () -> "fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void executeWithRateLimit_urlWithNoHost_usesFullUrlAsKey() throws Exception {
        String result = rateLimiter.executeWithRateLimit("file:///local/path", () -> "local");

        assertThat(result).isEqualTo("local");
    }
}
