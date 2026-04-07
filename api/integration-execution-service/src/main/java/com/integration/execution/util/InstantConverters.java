package com.integration.execution.util;

import java.time.Instant;

public final class InstantConverters {

    private InstantConverters() {
    }

    public static int toEpochSecondsInt(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("Instant must not be null");
        }
        long seconds = instant.getEpochSecond();
        if (seconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Epoch seconds exceed Integer range (Year 2038 problem)"
            );
        }
        return (int) seconds;
    }
}
