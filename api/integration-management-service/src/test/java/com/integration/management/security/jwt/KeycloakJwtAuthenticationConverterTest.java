package com.integration.management.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakJwtAuthenticationConverter")
class KeycloakJwtAuthenticationConverterTest {

    @Test
    @DisplayName("convert should extract roles from realm_access, resource_access, and custom roles")
    void convert_extractsRoles() {
        Jwt jwt = new Jwt(
                "t",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "realm_access", Map.of("roles", List.of("tenant_admin")),
                        "resource_access", Map.of("account", Map.of("roles", List.of("app_admin"))),
                        "roles", List.of("webhook_client")));

        KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();
        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).contains(
                new SimpleGrantedAuthority("ROLE_tenant_admin"),
                new SimpleGrantedAuthority("ROLE_app_admin"),
                new SimpleGrantedAuthority("ROLE_webhook_client"));
    }
}
