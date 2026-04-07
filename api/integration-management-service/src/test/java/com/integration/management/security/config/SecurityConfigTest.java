package com.integration.management.security.config;

import com.integration.management.security.filters.KeycloakAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import jakarta.servlet.Filter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

@DisplayName("SecurityConfig")
class SecurityConfigTest {

    private final ApplicationContextRunner cspEnabledRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestCorsBeans.class, TestBeansCspEnabled.class, SecurityConfig.class);
    private final ApplicationContextRunner cspReportOnlyRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestCorsBeans.class, TestBeansCspReportOnly.class, SecurityConfig.class);
    private final ApplicationContextRunner cspDisabledRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestCorsBeans.class, TestBeansCspDisabled.class, SecurityConfig.class);

    @Test
    @DisplayName("corsConfigurer should be constructible and accept a CorsRegistry")
    void corsConfigurer_constructs() {
        SecurityProperties props = new SecurityProperties();
        String[] allowedOriginPatterns = new String[1];
        allowedOriginPatterns[0] = "*";
        props.getCors().setAllowedOriginPatterns(allowedOriginPatterns);

        String[] allowedMethods = new String[1];
        allowedMethods[0] = "GET";
        props.getCors().setAllowedMethods(allowedMethods);

        String[] allowedHeaders = new String[1];
        allowedHeaders[0] = "Authorization";
        props.getCors().setAllowedHeaders(allowedHeaders);

        String[] exposedHeaders = new String[1];
        exposedHeaders[0] = "Authorization";
        props.getCors().setExposedHeaders(exposedHeaders);

        SecurityConfig config = new SecurityConfig(mock(KeycloakAuthenticationFilter.class), props);
        var corsConfigurer = config.corsConfigurer();

        assertThatCode(() -> corsConfigurer.addCorsMappings(new CorsRegistry()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bearerTokenResolver should resolve Authorization header token")
    void bearerTokenResolver_resolvesAuthorizationHeader() {
        SecurityConfig config = new SecurityConfig(mock(KeycloakAuthenticationFilter.class), new SecurityProperties());
        BearerTokenResolver resolver = config.bearerTokenResolver();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        request.addHeader("Authorization", "Bearer abc.def.ghi");

        assertThat(resolver.resolve(request)).isEqualTo("abc.def.ghi");
    }

    @Test
    @DisplayName("bearerTokenResolver should allow query token only for SSE stream path")
    void bearerTokenResolver_allowsQueryTokenOnlyForSsePath() {
        SecurityConfig config = new SecurityConfig(mock(KeycloakAuthenticationFilter.class), new SecurityProperties());
        BearerTokenResolver resolver = config.bearerTokenResolver();

        MockHttpServletRequest sseRequest = new MockHttpServletRequest("GET", SecurityConfig.SSE_STREAM_PATH);
        sseRequest.setParameter("access_token", "query-token");
        assertThat(resolver.resolve(sseRequest)).isEqualTo("query-token");

        MockHttpServletRequest otherPath = new MockHttpServletRequest("GET", "/api/management/other");
        otherPath.setParameter("access_token", "query-token");
        assertThat(resolver.resolve(otherPath)).isNull();

        MockHttpServletRequest sseBlank = new MockHttpServletRequest("GET", SecurityConfig.SSE_STREAM_PATH);
        sseBlank.setParameter("access_token", " ");
        assertThat(resolver.resolve(sseBlank)).isNull();
    }

    @Test
    @DisplayName("securityFilterChain should build and register bearer auth + keycloak filter (CSP enabled)")
    void securityFilterChain_builds_withCspEnabled() {
        cspEnabledRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            SecurityFilterChain chain = context.getBean(SecurityFilterChain.class);
            assertThat(chain).isInstanceOf(DefaultSecurityFilterChain.class);

            List<Filter> filters = ((DefaultSecurityFilterChain) chain).getFilters();
            assertThat(filters)
                    .anyMatch(filter -> filter instanceof BearerTokenAuthenticationFilter)
                    .anyMatch(filter -> filter == context.getBean(KeycloakAuthenticationFilter.class));
        });
    }

    @Test
    @DisplayName("securityFilterChain should build with CSP report-only")
    void securityFilterChain_builds_withCspReportOnly() {
        cspReportOnlyRunner.run(context -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
    }

    @Test
    @DisplayName("securityFilterChain should build when CSP disabled")
    void securityFilterChain_builds_withCspDisabled() {
        cspDisabledRunner.run(context -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestCorsBeans {
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            corsConfiguration.addAllowedOriginPattern("*");
            corsConfiguration.addAllowedHeader("*");
            corsConfiguration.addAllowedMethod("*");
            corsConfiguration.setAllowCredentials(true);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", corsConfiguration);
            return source;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeansCspEnabled {
        @Bean
        KeycloakAuthenticationFilter keycloakAuthenticationFilter() {
            return mock(KeycloakAuthenticationFilter.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties props = new SecurityProperties();
            props.getCsp().setEnabled(true);
            props.getCsp().setReportOnly(false);
            return props;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeansCspReportOnly {
        @Bean
        KeycloakAuthenticationFilter keycloakAuthenticationFilter() {
            return mock(KeycloakAuthenticationFilter.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties props = new SecurityProperties();
            props.getCsp().setEnabled(true);
            props.getCsp().setReportOnly(true);
            return props;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeansCspDisabled {
        @Bean
        KeycloakAuthenticationFilter keycloakAuthenticationFilter() {
            return mock(KeycloakAuthenticationFilter.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties props = new SecurityProperties();
            props.getCsp().setEnabled(false);
            return props;
        }
    }
}
