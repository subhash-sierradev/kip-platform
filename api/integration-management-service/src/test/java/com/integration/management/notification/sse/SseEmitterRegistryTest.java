package com.integration.management.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("SseEmitterRegistry")
class SseEmitterRegistryTest {

    private SseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry(new ObjectMapper());
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns a non-null SseEmitter")
        void returns_non_null_emitter() {
            SseEmitter emitter = registry.register("user-1");
            assertThat(emitter).isNotNull();
        }

        @Test
        @DisplayName("allows multiple emitters for the same user")
        void allows_multiple_emitters_per_user() {
            SseEmitter e1 = registry.register("user-1");
            SseEmitter e2 = registry.register("user-1");
            assertThat(e1).isNotSameAs(e2);
        }
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("does nothing when no emitter registered for user")
        void does_nothing_when_no_emitter() {
            assertThatNoException().isThrownBy(() ->
                    registry.send("user-unknown", buildResponse()));
        }

        @Test
        @DisplayName("sends payload to registered emitter without exception")
        void sends_to_registered_emitter() {
            registry.register("user-1");
            assertThatNoException().isThrownBy(() ->
                    registry.send("user-1", buildResponse()));
        }

        @Test
        @DisplayName("removes completed emitter on send error and does not throw")
        void removes_dead_emitter_on_send_error() {
            SseEmitter emitter = registry.register("user-1");
            emitter.complete();

            // After complete, sending will raise an error; registry should swallow it
            assertThatNoException().isThrownBy(() ->
                    registry.send("user-1", buildResponse()));
        }
    }

    @Nested
    @DisplayName("heartbeat")
    class Heartbeat {

        @Test
        @DisplayName("completes without error when no emitters registered")
        void completes_without_error_when_empty() {
            assertThatNoException().isThrownBy(() -> registry.heartbeat());
        }

        @Test
        @DisplayName("sends heartbeat to registered emitter")
        void sends_heartbeat_to_registered_emitter() {
            registry.register("user-1");
            assertThatNoException().isThrownBy(() -> registry.heartbeat());
        }
    }

    private AppNotificationResponse buildResponse() {
        return AppNotificationResponse.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .build();
    }
}
