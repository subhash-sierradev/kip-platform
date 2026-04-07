package com.integration.management.util;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.integration.management.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {CacheConfig.class, ClientIpAddressResolver.class})
@DisplayName("ClientIpAddressResolver")
class ClientIpAddressResolverTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ClientIpAddressResolver resolver;

    @BeforeEach
    void setUp() {
        // Clear cache before each test to ensure isolation
        ClientIpAddressResolver.clearCache();
    }

    @Test
    @DisplayName("resolveClientIpAddress should use first valid X-Forwarded-For value")
    void resolveClientIpAddress_validXForwardedFor_returnsFirstIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.setRemoteAddr("192.168.1.25");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("resolveClientIpAddress should fallback to X-Real-IP when X-Forwarded-For is invalid")
    void resolveClientIpAddress_invalidXForwardedFor_usesXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "spoofed-not-an-ip, 203.0.113.1");
        request.addHeader("X-Real-IP", "203.0.113.5");
        request.setRemoteAddr("192.168.1.25");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("resolveClientIpAddress should fallback to remoteAddr when proxy headers are invalid")
    void resolveClientIpAddress_invalidProxyHeaders_usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "spoofed-invalid-value");
        request.addHeader("X-Real-IP", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff:1");
        request.setRemoteAddr("192.168.1.100");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("resolveClientIpAddress should return null when all sources are invalid")
    void resolveClientIpAddress_allInvalid_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "spoofed-invalid-value");
        request.addHeader("X-Real-IP", "also-invalid");
        request.setRemoteAddr("not-an-ip");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isNull();
    }

    @Test
    @DisplayName("resolveClientIpAddress should normalize bracketed IPv6 value")
    void resolveClientIpAddress_bracketedIpv6_returnsUnwrappedValue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "[2001:db8::7]");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("2001:db8::7");
    }

    @Test
    @DisplayName("resolveClientIpAddress should skip blank first forwarded value and use fallback")
    void resolveClientIpAddress_blankFirstForwardedValue_usesFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Forwarded-For", "   , 203.0.113.2");
        request.addHeader("X-Real-IP", "203.0.113.8");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.8");
    }

    @Test
    @DisplayName("resolveClientIpAddress should accept valid full-form IPv6 with 8 segments")
    void resolveClientIpAddress_fullFormIpv6_returnsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv6 with excessive segment length")
    void resolveClientIpAddress_ipv6ExcessiveSegmentLength_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        // Segment with 5 hex characters (max is 4)
        request.addHeader("X-Real-IP", "2001:0db8:85a3:00000:0000:8a2e:0370:7334");
        request.setRemoteAddr("192.168.1.1");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv6 with invalid characters")
    void resolveClientIpAddress_ipv6InvalidChars_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "2001:0db8:85g3:0000:0000:8a2e:0370:7334");
        request.setRemoteAddr("192.168.1.2");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("192.168.1.2");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv6 without colon")
    void resolveClientIpAddress_ipv6NoColon_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "20010db885a3000000008a2e03707334");
        request.setRemoteAddr("192.168.1.3");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("192.168.1.3");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv6 with multiple compressions")
    void resolveClientIpAddress_ipv6MultipleCompressions_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "2001::85a3::7334");
        request.setRemoteAddr("192.168.1.4");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("192.168.1.4");
    }

    @Test
    @DisplayName("resolveClientIpAddress should accept valid compressed IPv6")
    void resolveClientIpAddress_validCompressedIpv6_returnsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "2001:db8:85a3::8a2e:370:7334");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("2001:db8:85a3::8a2e:370:7334");
    }

    @Test
    @DisplayName("resolveClientIpAddress should accept valid IPv4")
    void resolveClientIpAddress_validIpv4_returnsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.168.1.100");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv4 with out-of-range octet")
    void resolveClientIpAddress_ipv4OutOfRange_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.168.1.256");
        request.setRemoteAddr("10.0.0.1");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv4 with negative octet")
    void resolveClientIpAddress_ipv4NegativeOctet_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.-1.1.100");
        request.setRemoteAddr("10.0.0.2");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.2");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv4 with non-numeric characters")
    void resolveClientIpAddress_ipv4NonNumeric_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.168.abc.100");
        request.setRemoteAddr("10.0.0.3");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.3");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv4 with wrong segment count")
    void resolveClientIpAddress_ipv4WrongSegmentCount_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.168.1");
        request.setRemoteAddr("10.0.0.4");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.4");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv4 with blank segment")
    void resolveClientIpAddress_ipv4BlankSegment_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.168..100");
        request.setRemoteAddr("10.0.0.5");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IP exceeding max length")
    void resolveClientIpAddress_exceedsMaxLength_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        // Create string longer than 45 characters
        request.addHeader("X-Real-IP", "2001:0db8:85a3:0000:0000:8a2e:0370:7334:extra");
        request.setRemoteAddr("10.0.0.6");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.6");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject bracketed value that becomes blank after unwrapping")
    void resolveClientIpAddress_bracketedBlank_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "[  ]");
        request.setRemoteAddr("10.0.0.7");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.7");
    }

    @Test
    @DisplayName("resolveClientIpAddress should accept IPv6 with empty segments (compressed)")
    void resolveClientIpAddress_ipv6EmptySegments_returnsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "::1");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("::1");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv6 with too many colons")
    void resolveClientIpAddress_ipv6TooManyColons_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "2001:db8:85a3:0:0:8a2e:370:7334:extra");
        request.setRemoteAddr("10.0.0.8");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.8");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject IPv6 with triple colons")
    void resolveClientIpAddress_ipv6TripleColons_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", ":::1");
        request.setRemoteAddr("10.0.0.9");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.9");
    }

    @Test
    @DisplayName("resolveClientIpAddress should reject compressed IPv6 with excessive segment length")
    void resolveClientIpAddress_compressedIpv6ExcessiveSegmentLength_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        // Segment with 5 hex characters (max is 4) in compressed form
        request.addHeader("X-Real-IP", "2001:db8:00000::1");
        request.setRemoteAddr("10.0.0.10");

        String resolvedIp = ClientIpAddressResolver.resolveClientIpAddress(request);

        assertThat(resolvedIp).isEqualTo("10.0.0.10");
    }

    // Cache-specific tests

    @Test
    @DisplayName("resolveClientIpAddress should utilize cache for repeated valid IP validation")
    void resolveClientIpAddress_repeatedValidIp_usesCacheOnSecondCall() {
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/x");
        request1.addHeader("X-Real-IP", "192.168.1.100");

        // First call - cache miss
        String firstResult = ClientIpAddressResolver.resolveClientIpAddress(request1);
        assertThat(firstResult).isEqualTo("192.168.1.100");

        var nativeCache = ClientIpAddressResolver.getNativeCache();
        CacheStats statsAfterFirstCall = nativeCache != null ? nativeCache.stats() : null;
        long missesAfterFirst = statsAfterFirstCall != null ? statsAfterFirstCall.missCount() : 0;

        // Second call with same IP - should be cache hit
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/x");
        request2.addHeader("X-Real-IP", "192.168.1.100");
        String secondResult = ClientIpAddressResolver.resolveClientIpAddress(request2);

        assertThat(secondResult).isEqualTo("192.168.1.100");

        if (nativeCache != null) {
            CacheStats statsAfterSecondCall = nativeCache.stats();
            assertThat(statsAfterSecondCall.hitCount()).isGreaterThan(0);
            assertThat(statsAfterSecondCall.missCount()).isEqualTo(missesAfterFirst);
        }
    }

    @Test
    @DisplayName("resolveClientIpAddress should cache normalized IPv6 with brackets removed")
    void resolveClientIpAddress_bracketedIpv6_cachesNormalizedValue() {
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/x");
        request1.addHeader("X-Real-IP", "[2001:db8::1]");

        String firstResult = ClientIpAddressResolver.resolveClientIpAddress(request1);
        assertThat(firstResult).isEqualTo("2001:db8::1");

        // Second call with same bracketed IP - should hit cache
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/x");
        request2.addHeader("X-Real-IP", "[2001:db8::1]");
        String secondResult = ClientIpAddressResolver.resolveClientIpAddress(request2);

        assertThat(secondResult).isEqualTo("2001:db8::1");
        var nativeCache = ClientIpAddressResolver.getNativeCache();
        if (nativeCache != null) {
            assertThat(nativeCache.stats().hitCount()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("resolveClientIpAddress should not cache invalid IP addresses")
    void resolveClientIpAddress_invalidIp_doesNotCache() {
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/x");
        request1.addHeader("X-Real-IP", "invalid-ip-address");
        request1.setRemoteAddr("10.0.0.1");

        var nativeCache = ClientIpAddressResolver.getNativeCache();
        long initialSize = nativeCache != null ? nativeCache.estimatedSize() : 0;

        ClientIpAddressResolver.resolveClientIpAddress(request1);

        if (nativeCache != null) {
            // Only valid remoteAddr should be cached, not the invalid "invalid-ip-address"
            // Expected: 1 entry cached (the valid remoteAddr)
            assertThat(nativeCache.estimatedSize()).isEqualTo(initialSize + 1);
        }
    }

    @Test
    @DisplayName("clearCache should clear cached entries")
    void clearCache_afterValidations_clearsEntries() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Real-IP", "192.168.1.50");

        // Validate an IP to populate cache
        ClientIpAddressResolver.resolveClientIpAddress(request);

        var nativeCache = ClientIpAddressResolver.getNativeCache();
        if (nativeCache != null) {
            assertThat(nativeCache.estimatedSize()).isGreaterThan(0);
        }

        // Clear cache
        ClientIpAddressResolver.clearCache();

        if (nativeCache != null) {
            // Cache entries should be cleared (size = 0)
            // Note: Statistics (hitCount, missCount) are cumulative and don't reset
            assertThat(nativeCache.estimatedSize()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("getNativeCache should return valid cache object")
    void getNativeCache_returnsValidCache() {
        var nativeCache = ClientIpAddressResolver.getNativeCache();

        assertThat(nativeCache).isNotNull();
        CacheStats stats = nativeCache.stats();
        assertThat(stats).isNotNull();
        assertThat(stats.hitCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.missCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.evictionCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("resolveClientIpAddress should handle cache hits for multiple different valid IPs")
    void resolveClientIpAddress_multipleDifferentIps_cachesEachIndependently() {
        // First IP
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/x");
        request1.addHeader("X-Real-IP", "192.168.1.10");
        String result1 = ClientIpAddressResolver.resolveClientIpAddress(request1);
        assertThat(result1).isEqualTo("192.168.1.10");

        // Second IP
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/x");
        request2.addHeader("X-Real-IP", "10.0.0.20");
        String result2 = ClientIpAddressResolver.resolveClientIpAddress(request2);
        assertThat(result2).isEqualTo("10.0.0.20");

        // Third IP
        MockHttpServletRequest request3 = new MockHttpServletRequest("GET", "/x");
        request3.addHeader("X-Real-IP", "2001:db8::100");
        String result3 = ClientIpAddressResolver.resolveClientIpAddress(request3);
        assertThat(result3).isEqualTo("2001:db8::100");

        // Repeat first IP - should be cache hit
        MockHttpServletRequest request4 = new MockHttpServletRequest("GET", "/x");
        request4.addHeader("X-Real-IP", "192.168.1.10");
        String result4 = ClientIpAddressResolver.resolveClientIpAddress(request4);
        assertThat(result4).isEqualTo("192.168.1.10");

        var nativeCache = ClientIpAddressResolver.getNativeCache();
        if (nativeCache != null) {
            CacheStats stats = nativeCache.stats();
            assertThat(stats.hitCount()).isGreaterThan(0);
            assertThat(stats.missCount()).isGreaterThanOrEqualTo(3);
        }
    }
}
