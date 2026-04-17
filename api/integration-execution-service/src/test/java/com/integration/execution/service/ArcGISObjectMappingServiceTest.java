package com.integration.execution.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArcGISObjectMappingService.
 *
 * Spring cache annotations are intercepted by AOP proxies, so we test the plain method logic
 * directly: cache-miss returns null, cache-put returns the supplied objectId, and
 * the abbreviateUrlHash helper branches are all covered.
 */
@DisplayName("ArcGISObjectMappingService")
class ArcGISObjectMappingServiceTest {

    private final ArcGISObjectMappingService service = new ArcGISObjectMappingService();

    @Test
    @DisplayName("getMapping always returns null on a real cache miss")
    void getMapping_cacheMiss_returnsNull() {
        assertThat(service.getMapping("loc-1", "abc12345longerthan8")).isNull();
    }

    @Test
    @DisplayName("getMapping with null urlHash does not throw")
    void getMapping_nullUrlHash_returnsNull() {
        assertThat(service.getMapping("loc-1", null)).isNull();
    }

    @Test
    @DisplayName("getMapping with urlHash exactly 8 chars returns null without error")
    void getMapping_urlHashExactlyEightChars_returnsNull() {
        assertThat(service.getMapping("loc-1", "12345678")).isNull();
    }

    @Test
    @DisplayName("getMapping with urlHash shorter than 8 chars returns null without error")
    void getMapping_shortUrlHash_returnsNull() {
        assertThat(service.getMapping("loc-1", "abc")).isNull();
    }

    @Test
    @DisplayName("putMapping returns the supplied objectId")
    void putMapping_returnsSuppliedObjectId() {
        assertThat(service.putMapping("loc-1", "abc12345longerthan8", 42L)).isEqualTo(42L);
    }

    @Test
    @DisplayName("putMapping with null urlHash returns the supplied objectId")
    void putMapping_nullUrlHash_returnsObjectId() {
        assertThat(service.putMapping("loc-1", null, 99L)).isEqualTo(99L);
    }

    @Test
    @DisplayName("putMapping with short urlHash returns the supplied objectId")
    void putMapping_shortUrlHash_returnsObjectId() {
        assertThat(service.putMapping("loc-1", "short", 7L)).isEqualTo(7L);
    }
}

