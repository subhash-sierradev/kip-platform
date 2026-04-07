package com.integration.management.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserProfileResponse")
class UserProfileResponseTest {

    @Test
    @DisplayName("builder and all-args constructor produce equivalent response")
    void builderAndAllArgsConstructorProduceEquivalentResponse() {
        UUID id = UUID.randomUUID();
        Instant createdDate = Instant.parse("2026-03-06T00:00:00Z");

        UserProfileResponse built = UserProfileResponse.builder()
                .id(id)
                .keycloakUserId("kc-1")
                .email("user@example.com")
                .displayName("User One")
                .isTenantAdmin(true)
                .createdDate(createdDate)
                .build();

        UserProfileResponse constructed = new UserProfileResponse(
                id,
                "kc-1",
                "user@example.com",
                "User One",
                true,
                createdDate);

        assertThat(built).isEqualTo(constructed);
        assertThat(built.getDisplayName()).isEqualTo("User One");
    }
}
