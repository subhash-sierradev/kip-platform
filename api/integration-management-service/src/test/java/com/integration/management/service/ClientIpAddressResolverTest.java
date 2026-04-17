package com.integration.management.service;

import com.integration.management.constants.CacheConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientIpAddressResolver")
class ClientIpAddressResolverTest {

    @Mock
    private CacheManager mockCacheManager;

    private ClientIpAddressResolver resolver;

    @BeforeEach
    void setUp() {
        ConcurrentMapCacheManager realCacheManager = new ConcurrentMapCacheManager(CacheConstants.IP_VALIDATION_CACHE);
        resolver = new ClientIpAddressResolver(realCacheManager);
        resolver.clearCache();
    }

    @Test
    @DisplayName("resolveClientIpAddress should use first valid X-Forwarded-For value")
    void resolveClientIpAddress_validXForwardedFor_returnsFirstIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.setRemoteAddr("192.168.1.25");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("resolveClientIpAddress should fall back to X-Real-IP when X-Forwarded-For is absent")
    void resolveClientIpAddress_xRealIp_fallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "198.51.100.5");
        request.setRemoteAddr("10.0.0.1");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("198.51.100.5");
    }

    @Test
    @DisplayName("resolveClientIpAddress should fall back to remoteAddr when no headers present")
    void resolveClientIpAddress_remoteAddr_fallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.setRemoteAddr("172.16.0.1");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("172.16.0.1");
    }

    @Test
    @DisplayName("resolveClientIpAddress should handle blank X-Forwarded-For header")
    void resolveClientIpAddress_blankXForwardedFor_usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("10.0.0.1");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("resolveClientIpAddress should handle IPv6 addresses in brackets")
    void resolveClientIpAddress_bracketedIpv6() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "[2001:db8::1]");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("2001:db8::1");
    }

    @Test
    @DisplayName("resolveClientIpAddress should return null for invalid IP")
    void resolveClientIpAddress_invalidIp_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "not-an-ip");
        request.addHeader("X-Real-IP", "also-not-an-ip");
        request.setRemoteAddr("still-not-an-ip");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isNull();
    }

    @Test
    @DisplayName("resolveClientIpAddress should use cache on second call")
    void resolveClientIpAddress_cachedOnSecondCall() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "203.0.113.50");

        String firstCall = resolver.resolveClientIpAddress(request);
        String secondCall = resolver.resolveClientIpAddress(request);

        assertThat(firstCall).isEqualTo("203.0.113.50");
        assertThat(secondCall).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("clearCache should not throw when cache exists")
    void clearCache_doesNotThrow() {
        resolver.clearCache();
    }

    @Test
    @DisplayName("getNativeCache should return null when cache is not configured")
    void getNativeCache_nullWhenNotCaffeine() {
        // ConcurrentMapCacheManager doesn't use Caffeine, so getNativeCache throws ClassCastException
        // This tests the null-cache path instead
        when(mockCacheManager.getCache(CacheConstants.IP_VALIDATION_CACHE)).thenReturn(null);
        resolver = new ClientIpAddressResolver(mockCacheManager);
        assertThat(resolver.getNativeCache()).isNull();
    }

    @Test
    @DisplayName("constructor should handle null cache from CacheManager")
    void constructor_nullCache_doesNotThrow() {
        when(mockCacheManager.getCache(CacheConstants.IP_VALIDATION_CACHE)).thenReturn(null);
        new ClientIpAddressResolver(mockCacheManager);
    }

    @Test
    @DisplayName("resolveClientIpAddress should handle IP exceeding max length")
    void resolveClientIpAddress_tooLongIp_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        String longIp = "a".repeat(46);
        request.addHeader("X-Forwarded-For", longIp);
        request.setRemoteAddr(longIp);

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isNull();
    }

    @Test
    @DisplayName("resolveClientIpAddress should handle empty brackets")
    void resolveClientIpAddress_emptyBrackets_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "[]");
        request.setRemoteAddr("[]");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isNull();
    }

    @Test
    @DisplayName("resolveClientIpAddress should handle valid IPv6 in X-Real-IP")
    void resolveClientIpAddress_ipv6XRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "::1");

        String resolvedIp = resolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("::1");
    }
}
