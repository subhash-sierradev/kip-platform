package com.integration.management.model.dto.response;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConnectionDependentItemResponse")
class ConnectionDependentItemResponseTest {

    @Test
    @DisplayName("builder and all-args constructor hold expected values")
    void builderAndAllArgsConstructorHoldExpectedValues() {
        Instant lastRun = Instant.parse("2025-12-19T14:45:00Z");
        ConnectionDependentItemResponse built = ConnectionDependentItemResponse.builder()
                .id("a1")
                .name("ArcGIS Integration 1")
                .isEnabled(true)
                .description("Primary integration")
                .lastRunAt(lastRun)
                .build();

        ConnectionDependentItemResponse constructed = new ConnectionDependentItemResponse(
                "a1",
                "ArcGIS Integration 1",
                true,
                "Primary integration",
                lastRun);

        assertThat(built).isEqualTo(constructed);
        assertThat(built.getDescription()).isEqualTo("Primary integration");
        assertThat(built.getLastRunAt()).isEqualTo(lastRun);
    }

    @Test
    @DisplayName("builder accepts null lastRunAt")
    void builderAcceptsNullLastRunAt() {
        ConnectionDependentItemResponse built = ConnectionDependentItemResponse.builder()
                .id("a2")
                .name("Integration without runs")
                .isEnabled(false)
                .description("Never executed")
                .lastRunAt(null)
                .build();

        assertThat(built.getLastRunAt()).isNull();
        assertThat(built.getName()).isEqualTo("Integration without runs");
    }
}
