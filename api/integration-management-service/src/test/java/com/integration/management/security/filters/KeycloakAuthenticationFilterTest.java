package com.integration.management.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.management.security.config.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import jakarta.servlet.FilterChain;

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
                props.getJwt().getTenantIdClaim(), "tenant1",
                props.getJwt().getUserIdClaim(), "user1"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(props.getJwt().getTenantIdClaim())).isEqualTo("tenant1");
        assertThat(request.getAttribute(props.getJwt().getUserIdClaim())).isEqualTo("user1");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("webhook client role should set system tenant/user and continue")
    void webhookClientRole_setsSystemTenantUser() throws Exception {
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
    void missingClaims_noWebhookRole_returns403() throws Exception {
        setAuthContext(Map.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains(props.getError().getMissingFields());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("tenant_id claim as list should be parsed and leading slashes stripped")
    void tenantIdListClaim_isParsed_andStripsLeadingSlash() throws Exception {
        setAuthContext(Map.of(
                props.getJwt().getTenantIdClaim(), List.of("/tenantA", "tenantB"),
                props.getJwt().getUserIdClaim(), "user1"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(props.getJwt().getTenantIdClaim())).isEqualTo("tenantA");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("realm_access malformed values should not be treated as webhook client")
    void malformedRealmAccess_doesNotAllowWebhookClient() throws Exception {
        // Case 1: realm_access is a string, not a map
        setAuthContext(Map.of("realm_access", "not-a-map"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains(props.getError().getMissingFields());
        verify(chain, never()).doFilter(request, response);

        // Case 2: roles is not a list
        setAuthContext(Map.of("realm_access", Map.of("roles", "not-a-list")));
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilterInternal(request2, response2, chain);

        assertThat(response2.getStatus()).isEqualTo(403);
        assertThat(response2.getContentAsString()).contains(props.getError().getMissingFields());
    }
}
