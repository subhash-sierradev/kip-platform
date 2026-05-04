package com.integration.management.security.config;

import com.integration.management.security.filters.KeycloakAuthenticationFilter;
import com.integration.management.security.jwt.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.jspecify.annotations.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String SSE_STREAM_PATH = "/api/management/notifications/stream";

    private final KeycloakAuthenticationFilter keycloakAuthenticationFilter;
    private final SecurityProperties securityProperties;

    @Bean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    /**
     * SSE stream endpoint that requires token via query-string because browsers cannot set
     * Authorization headers on EventSource connections.
     * Restricted to the exact path to prevent other endpoints from accepting query-string tokens.
     */

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return (HttpServletRequest request) -> {
            String token = defaultResolver.resolve(request);
            if (token != null) return token;
            // Allow query-string token ONLY for the SSE stream endpoint (browsers cannot set
            // Authorization headers for EventSource — this is the sole exception)
            if (SSE_STREAM_PATH.equals(request.getRequestURI())) {
                String queryToken = request.getParameter("access_token");
                if (StringUtils.hasText(queryToken)) return queryToken;
            }
            return null;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auths -> auths
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter()))
                )
                .addFilterAfter(keycloakAuthenticationFilter, BearerTokenAuthenticationFilter.class);

        // Configure CSP if enabled
        if (securityProperties.getCsp().isEnabled()) {
            log.info("Configuring security headers including Content Security Policy (CSP)");
            configureCsp(http);
        }

        // Configure frame options using modern Spring Security approach
        http.headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
        );

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                SecurityProperties.Cors corsConfig = securityProperties.getCors();

                log.info("Configuring CORS with allowed origins: {}",
                        String.join(", ", corsConfig.getAllowedOriginPatterns()));

                registry.addMapping(corsConfig.getPathPattern())
                        .allowedOriginPatterns(corsConfig.getAllowedOriginPatterns())
                        .allowedMethods(corsConfig.getAllowedMethods())
                        .allowedHeaders(corsConfig.getAllowedHeaders())
                        .exposedHeaders(corsConfig.getExposedHeaders())
                        .allowCredentials(corsConfig.isAllowCredentials())
                        .maxAge(corsConfig.getMaxAge());
            }
        };
    }

    private void configureCsp(HttpSecurity http) throws Exception {
        SecurityProperties.Csp csp = securityProperties.getCsp();
        StringBuilder cspPolicy = new StringBuilder();

        // Build CSP policy string
        appendCspDirective(cspPolicy, "default-src", csp.getDefaultSrc());
        appendCspDirective(cspPolicy, "script-src", csp.getScriptSrc());
        appendCspDirective(cspPolicy, "style-src", csp.getStyleSrc());
        appendCspDirective(cspPolicy, "img-src", csp.getImgSrc());
        appendCspDirective(cspPolicy, "font-src", csp.getFontSrc());
        appendCspDirective(cspPolicy, "connect-src", csp.getConnectSrc());
        appendCspDirective(cspPolicy, "frame-src", csp.getFrameSrc());
        appendCspDirective(cspPolicy, "object-src", csp.getObjectSrc());
        appendCspDirective(cspPolicy, "media-src", csp.getMediaSrc());
        appendCspDirective(cspPolicy, "manifest-src", csp.getManifestSrc());
        appendCspDirective(cspPolicy, "worker-src", csp.getWorkerSrc());
        appendCspDirective(cspPolicy, "child-src", csp.getChildSrc());
        appendCspDirective(cspPolicy, "form-action", csp.getFormAction());
        appendCspDirective(cspPolicy, "frame-ancestors", csp.getFrameAncestors());
        appendCspDirective(cspPolicy, "base-uri", csp.getBaseUri());

        if (csp.isUpgradeInsecureRequests()) {
            appendCspDirective(cspPolicy, "upgrade-insecure-requests", "");
        }

        String policyString = cspPolicy.toString().trim();
        log.info("CSP Policy configured: {}", policyString);

        if (csp.isReportOnly()) {
            log.info("CSP configured in report-only mode");
            http.headers(headers -> headers
                    .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy-Report-Only",
                            policyString))
            );
        } else {
            http.headers(headers -> headers
                    .addHeaderWriter(new ContentSecurityPolicyHeaderWriter(policyString))
            );
        }
    }

    private void appendCspDirective(StringBuilder policy, String directive, String value) {
        if (StringUtils.hasText(value)) {
            if (!policy.isEmpty()) {
                policy.append("; ");
            }
            policy.append(directive).append(" ").append(value);
        }
    }
}
