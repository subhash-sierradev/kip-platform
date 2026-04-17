package com.integration.management.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakJwtAuthenticationConverter")
class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Nested
    @DisplayName("realm_access roles")
    class RealmAccessRoles {

        @Test
        @DisplayName("extracts roles from realm_access.roles")
        void extractsRealmRoles() {
            Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("tenant_admin"))));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_tenant_admin"));
        }

        @Test
        @DisplayName("null realm_access is skipped without error")
        void nullRealmAccess_skipped() {
            Jwt jwt = buildJwt(Map.of());
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("realm_access present but roles not a Collection is skipped")
        void realmAccessRolesNotCollection_skipped() {
            Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", "not-a-list")));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("resource_access roles")
    class ResourceAccessRoles {

        @Test
        @DisplayName("extracts roles from resource_access.account.roles")
        void extractsResourceAccessRoles() {
            Jwt jwt = buildJwt(Map.of(
                    "resource_access", Map.of("account", Map.of("roles", List.of("app_admin")))));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_app_admin"));
        }

        @Test
        @DisplayName("null resource_access is skipped without error")
        void nullResourceAccess_skipped() {
            Jwt jwt = buildJwt(Map.of());
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("resource_access.account not a Map is skipped")
        void resourceAccessAccountNotMap_skipped() {
            Jwt jwt = buildJwt(Map.of(
                    "resource_access", Map.of("account", "not-a-map")));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("resource_access.account.roles not a Collection is skipped")
        void resourceAccessRolesNotCollection_skipped() {
            Jwt jwt = buildJwt(Map.of(
                    "resource_access", Map.of("account", Map.of("roles", "not-a-list"))));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("custom roles claim")
    class CustomRolesClaim {

        @Test
        @DisplayName("extracts roles from custom roles claim")
        void extractsCustomRoles() {
            Jwt jwt = buildJwt(Map.of("roles", List.of("webhook_client")));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_webhook_client"));
        }

        @Test
        @DisplayName("custom roles claim not a Collection is skipped")
        void customRolesNotCollection_skipped() {
            Jwt jwt = buildJwt(Map.of("roles", "not-a-list"));
            var auth = converter.convert(jwt);
            assertThat(auth.getAuthorities()).isEmpty();
        }
    }

    @Test
    @DisplayName("deduplicates roles across all three sources")
    void deduplicatesRolesAcrossSources() {
        Jwt jwt = buildJwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin", "user")),
                "resource_access", Map.of("account", Map.of("roles", List.of("user", "ops"))),
                "roles", List.of("custom", "admin")));
        var auth = converter.convert(jwt);
        assertThat(auth.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user", "ROLE_ops", "ROLE_custom");
    }

    @Test
    @DisplayName("convert returns JwtAuthenticationToken with token value")
    void convert_returnsJwtAuthenticationToken() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("tenant_admin"))));
        var auth = converter.convert(jwt);
        assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(((JwtAuthenticationToken) auth).getToken()).isSameAs(jwt);
    }

    @Test
    @DisplayName("all three sources combined extract correct authorities")
    void convert_extractsRoles() {
        Jwt jwt = buildJwt(Map.of(
                "realm_access", Map.of("roles", List.of("tenant_admin")),
                "resource_access", Map.of("account", Map.of("roles", List.of("app_admin"))),
                "roles", List.of("webhook_client")));
        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).contains(
                new SimpleGrantedAuthority("ROLE_tenant_admin"),
                new SimpleGrantedAuthority("ROLE_app_admin"),
                new SimpleGrantedAuthority("ROLE_webhook_client"));
    }
}
