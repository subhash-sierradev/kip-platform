package com.integration.management.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IdGenerator")
class IdGeneratorTest {

    @Test
    @DisplayName("randomBase62 returns requested length")
    void randomBase62_length() {
        assertThat(IdGenerator.randomBase62(0)).isEmpty();
        assertThat(IdGenerator.randomBase62(1)).hasSize(1);
        assertThat(IdGenerator.randomBase62(16)).hasSize(16);
    }

    @Test
    @DisplayName("randomBase62 uses only base62 alphabet")
    void randomBase62_alphabet() {
        String value = IdGenerator.randomBase62(128);
        assertThat(value).matches("^[0-9a-zA-Z]+$");
    }

    @Test
    @DisplayName("randomBase62 throws for negative length")
    void randomBase62_negative_throws() {
        assertThatThrownBy(() -> IdGenerator.randomBase62(-1))
                .isInstanceOf(NegativeArraySizeException.class);
    }

    @Test
    @DisplayName("randomBase62 is not constant")
    void randomBase62_notConstant() {
        String a = IdGenerator.randomBase62(32);
        String b = IdGenerator.randomBase62(32);
        assertThat(a).isNotEqualTo(b);
    }
}
