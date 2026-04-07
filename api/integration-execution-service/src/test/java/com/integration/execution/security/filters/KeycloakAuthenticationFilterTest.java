package com.integration.execution.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.security.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("KeycloakAuthenticationFilter")
class KeycloakAuthenticationFilterTest {

    private final SecurityProperties props = new SecurityProperties();
    private final KeycloakAuthenticationFilter filter = new KeycloakAuthenticationFilter(props, new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthContext(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    @DisplayName("no JWT in security context should return 401 and not continue")
    void noJwtInSecurityContext_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains(props.getError().getMissingAuth());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("token with tenant and user claims should set request attributes and continue")
    void tenantAndUser_present_setsAttributes_andContinues() throws Exception {
        setAuthContext(Map.of(
                props.getJwt().getTenantIdClaim(), "tenant-a",
                props.getJwt().getUserIdClaim(), "alice"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(props.getJwt().getTenantIdClaim())).isEqualTo("tenant-a");
        assertThat(request.getAttribute(props.getJwt().getUserIdClaim())).isEqualTo("alice");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("tenant_id as list should use first non-blank value and strip leading slash")
    void tenantIdAsArray_usesFirstNonBlankValue() throws Exception {
        setAuthContext(Map.of(
                props.getJwt().getTenantIdClaim(), List.of("", "/tenant-b"),
                props.getJwt().getUserIdClaim(), "bob"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(props.getJwt().getTenantIdClaim())).isEqualTo("tenant-b");
        assertThat(request.getAttribute(props.getJwt().getUserIdClaim())).isEqualTo("bob");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("webhook client role should set system tenant/user and continue")
    void webhookClientRole_usesSystemTenantAndUser() throws Exception {
        setAuthContext(Map.of(
                "realm_access", Map.of("roles", List.of(props.getWebhook().getClientRole()))
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(props.getJwt().getTenantIdClaim())).isEqualTo(props.getWebhook().getTenantId());
        assertThat(request.getAttribute(props.getJwt().getUserIdClaim())).isEqualTo(props.getWebhook().getUserId());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("missing required claims and no webhook role should return 403")
    void missingFieldsAndNoWebhookRole_returns403() throws Exception {
        setAuthContext(Map.of("realm_access", Map.of("roles", List.of("random_role"))));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains(props.getError().getMissingFields());
        verify(chain, never()).doFilter(request, response);
    }
}
