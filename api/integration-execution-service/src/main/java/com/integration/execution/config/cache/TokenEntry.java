package com.integration.execution.config.cache;

import java.time.Instant;

public record TokenEntry(
        String token,
        Instant expiresAt
) {
}