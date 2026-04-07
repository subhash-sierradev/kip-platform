package com.integration.execution.security.config;

import com.integration.execution.security.filters.KeycloakAuthenticationFilter;
import com.integration.execution.security.jwt.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;

import jakarta.servlet.Filter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestBeans.class, SecurityConfig.class);

    @Test
    void keycloakJwtAuthenticationConverter_returnsConverter() {
        SecurityConfig config = new SecurityConfig(mock(KeycloakAuthenticationFilter.class));

        KeycloakJwtAuthenticationConverter converter = config.keycloakJwtAuthenticationConverter();

        assertThat(converter).isNotNull();
    }

    @Test
    void securityFilterChain_registersBearerAuth_andAddsKeycloakAuthenticationFilter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context).hasSingleBean(KeycloakJwtAuthenticationConverter.class);

            SecurityFilterChain chain = context.getBean(SecurityFilterChain.class);
            assertThat(chain).isInstanceOf(DefaultSecurityFilterChain.class);

            List<Filter> filters = ((DefaultSecurityFilterChain) chain).getFilters();
            assertThat(filters)
                    .anyMatch(filter -> filter instanceof BearerTokenAuthenticationFilter)
                    .anyMatch(filter -> filter == context.getBean(KeycloakAuthenticationFilter.class));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        KeycloakAuthenticationFilter keycloakAuthenticationFilter() {
            return mock(KeycloakAuthenticationFilter.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
