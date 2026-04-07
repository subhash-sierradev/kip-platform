package com.integration.execution.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstantConvertersTest {

    @Test
    void toEpochSecondsInt_validInstant_returnsEpochSecondsAsInt() {
        int result = InstantConverters.toEpochSecondsInt(Instant.ofEpochSecond(1_700_000_000L));

        assertThat(result).isEqualTo(1_700_000_000);
    }

    @Test
    void toEpochSecondsInt_nullInstant_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> InstantConverters.toEpochSecondsInt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Instant must not be null");
    }

    @Test
    void toEpochSecondsInt_epochBeyondIntegerMax_throwsIllegalArgumentException() {
        Instant tooLarge = Instant.ofEpochSecond((long) Integer.MAX_VALUE + 1);

        assertThatThrownBy(() -> InstantConverters.toEpochSecondsInt(tooLarge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Epoch seconds exceed Integer range");
    }
}
