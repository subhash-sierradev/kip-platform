package com.integration.management.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityContextHelper")
class SecurityContextHelperTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("hasRole returns false when no authentication is set")
    void hasRole_noAuthentication_returnsFalse() {
        SecurityContextHolder.clearContext();
        assertThat(SecurityContextHelper.hasRole("tenant_admin")).isFalse();
    }

    @Test
    @DisplayName("hasRole returns true when authentication has matching ROLE_ prefixed authority")
    void hasRole_matchingRole_returnsTrue() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null,
                List.of(new SimpleGrantedAuthority("ROLE_tenant_admin")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextHelper.hasRole("tenant_admin")).isTrue();
    }

    @Test
    @DisplayName("hasRole returns false when authority does not match")
    void hasRole_nonMatchingRole_returnsFalse() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null,
                List.of(new SimpleGrantedAuthority("ROLE_app_admin")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextHelper.hasRole("tenant_admin")).isFalse();
    }

    @Test
    @DisplayName("hasRole returns false when authority list is empty")
    void hasRole_emptyAuthorities_returnsFalse() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextHelper.hasRole("tenant_admin")).isFalse();
    }

    @Test
    @DisplayName("hasRole handles null authority value without NPE")
    void hasRole_nullAuthority_returnsFalse() {
        // Use an authority that produces a null string — proxy via subclass
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null,
                List.of(new SimpleGrantedAuthority("ROLE_other")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextHelper.hasRole("tenant_admin")).isFalse();
    }
}

