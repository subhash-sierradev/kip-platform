package com.integration.execution.it;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.mock;

/**
 * Base class for all IES integration tests.
 *
 * <p>Starts a shared RabbitMQ container (reused across the suite) and wires
 * its connection details into the Spring context via {@link DynamicPropertySource}.
 *
 * <p>IES is stateless (no database), so only RabbitMQ infrastructure is required.
 * External HTTP dependencies (ArcGIS, Jira, Kaseware) are mocked via WireMock in individual tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Testcontainers
public abstract class AbstractIesIT {

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine")
                    .withReuse(true);

    @DynamicPropertySource
    static void overrideProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getAmqpPort());
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    /**
     * Provides a mock JwtDecoder so Spring Security does not attempt to connect to Keycloak
     * during integration test context startup.
     */
    @TestConfiguration
    static class SecurityTestConfig {
        @Bean
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
