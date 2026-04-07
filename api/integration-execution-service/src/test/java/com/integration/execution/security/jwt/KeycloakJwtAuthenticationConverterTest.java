package com.integration.execution.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void convert_collectsRolesFromAllClaimsAndDeduplicates() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("realm_access", Map.of("roles", List.of("admin", "user")))
                .claim("resource_access", Map.of("account", Map.of("roles", List.of("user", "ops"))))
                .claim("roles", List.of("custom", "admin"))
                .build();

        JwtAuthenticationToken authToken = (JwtAuthenticationToken) converter.convert(jwt);

        Set<String> authorities = authToken.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user", "ROLE_ops", "ROLE_custom");
    }

    @Test
    void convert_withMissingClaims_returnsNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken authToken = (JwtAuthenticationToken) converter.convert(jwt);
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();

        assertThat(authorities).isEmpty();
    }
}
