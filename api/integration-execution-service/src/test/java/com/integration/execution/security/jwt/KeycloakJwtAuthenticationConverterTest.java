package com.integration.execution.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("KeycloakJwtAuthenticationConverter")
class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void convert_collectsRolesFromAllClaimsAndDeduplicates() {
        Jwt jwt = buildJwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin", "user")),
                "resource_access", Map.of("account", Map.of("roles", List.of("user", "ops"))),
                "roles", List.of("custom", "admin")));

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
        Jwt jwt = buildJwt(Map.of());

        JwtAuthenticationToken authToken = (JwtAuthenticationToken) converter.convert(jwt);
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();

        assertThat(authorities).isEmpty();
    }

    @Nested
    @DisplayName("realm_access branches")
    class RealmAccessBranches {

        @Test
        @DisplayName("realm_access roles not a Collection is skipped")
        void realmAccessRolesNotCollection_skipped() {
            Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", "not-a-list")));
            assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("realm_access is null — no authorities")
        void nullRealmAccess_returnsEmpty() {
            Jwt jwt = buildJwt(Map.of());
            assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("resource_access branches")
    class ResourceAccessBranches {

        @Test
        @DisplayName("resource_access.account not a Map is skipped")
        void resourceAccessAccountNotMap_skipped() {
            Jwt jwt = buildJwt(Map.of("resource_access", Map.of("account", "not-a-map")));
            assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("resource_access.account.roles not a Collection is skipped")
        void resourceAccessRolesNotCollection_skipped() {
            Jwt jwt = buildJwt(Map.of(
                    "resource_access", Map.of("account", Map.of("roles", "not-a-list"))));
            assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("null resource_access — no authorities from that source")
        void nullResourceAccess_returnsEmpty() {
            Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("admin"))));
            assertThat(converter.convert(jwt).getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_admin");
        }
    }

    @Nested
    @DisplayName("custom roles claim branches")
    class CustomRolesClaimBranches {

        @Test
        @DisplayName("custom roles not a Collection is skipped")
        void customRolesNotCollection_skipped() {
            Jwt jwt = buildJwt(Map.of("roles", "not-a-list"));
            assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("custom roles as Collection adds authorities")
        void customRolesAsCollection_addsAuthorities() {
            Jwt jwt = buildJwt(Map.of("roles", List.of("webhook_client")));
            assertThat(converter.convert(jwt).getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_webhook_client");
        }
    }
}
