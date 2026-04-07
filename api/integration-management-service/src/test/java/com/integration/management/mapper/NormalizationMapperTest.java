package com.integration.management.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NormalizationMapper")
class NormalizationMapperTest {

    private final NormalizationMapper mapper = new NormalizationMapper() {
    };

    @Test
    @DisplayName("normalize returns null for null/blank")
    void normalize_nullOrBlank_returnsNull() {
        assertThat(mapper.normalize(null)).isNull();
        assertThat(mapper.normalize(" ")).isNull();
    }

    @Test
    @DisplayName("normalize lowercases, trims, and underscores")
    void normalize_transforms() {
        assertThat(mapper.normalize("  Hello, World!!  ")).isEqualTo("hello_world");
        assertThat(mapper.normalize("__Already__underscored__")).isEqualTo("already_underscored");
    }
}
