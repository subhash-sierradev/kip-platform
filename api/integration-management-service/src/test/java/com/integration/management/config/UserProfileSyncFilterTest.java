package com.integration.management.config;

import com.integration.management.entity.UserProfile;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.service.UserProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import jakarta.servlet.FilterChain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.integration.management.constants.ManagementSecurityConstants.ROLE_TENANT_ADMIN;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_NAME;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_EMAIL;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileSyncFilter")
class UserProfileSyncFilterTest {

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserProfileSyncFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ helpers

    private Jwt buildJwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), claims);
    }

    private Jwt defaultJwt() {
        return buildJwt(Map.of(
                X_TENANT_ID, "tenant1",
                X_USER_ID, "user1",
                X_USER_EMAIL, "u@example.com",
                X_USER_NAME, "User One",
                X_TENANT_NAME, "Tenant One"));
    }

    private UserProfile cachedProfile(String email, String displayName, boolean isTenantAdmin) {
        return UserProfile.builder()
                .keycloakUserId("user1")
                .tenantId("tenant1")
                .email(email)
                .displayName(displayName)
                .isTenantAdmin(isTenantAdmin)
                .build();
    }

    private void setAuth(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void setAuthWithAdminRole(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(
                        new SimpleGrantedAuthority("ROLE_" + ROLE_TENANT_ADMIN))));
    }

    // -------------------------------------------------------- filter chain pass-through

    @Nested
    @DisplayName("filter chain pass-through")
    class FilterChainPassThrough {

        @Test
        @DisplayName("no authentication: chain always continues")
        void noAuthentication_chainContinues() throws Exception {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
            verify(userProfileService, never()).getUserByKeycloakId(any(), any());
            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("non-JWT authentication: chain continues, no sync")
        void nonJwtAuthentication_chainContinues() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    mock(org.springframework.security.core.Authentication.class));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("syncUser throws RuntimeException: chain still continues")
        void syncUser_throwsRuntimeException_chainStillContinues() throws Exception {
            setAuth(defaultJwt());
            when(userProfileService.getUserByKeycloakId(any(), any()))
                    .thenThrow(new IntegrationNotFoundException("not found"));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenThrow(new RuntimeException("db error"));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
        }
    }

    // -------------------------------------------------------- new user (cache miss)

    @Nested
    @DisplayName("new user — cache miss")
    class NewUser {

        @Test
        @DisplayName("getUserByKeycloakId throws IntegrationNotFoundException: syncUser called")
        void cacheMiss_newUser_callsSyncUser() throws Exception {
            setAuth(defaultJwt());
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenThrow(new IntegrationNotFoundException("not found"));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile("u@example.com", "User One", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser("user1", "u@example.com", "User One", "tenant1", "Tenant One", false);
            verify(chain).doFilter(any(), any());
        }
    }

    // -------------------------------------------------------- returning user (cache hit)

    @Nested
    @DisplayName("returning user — cache hit")
    class ReturningUser {

        @Test
        @DisplayName("no field changes: syncUser is never called")
        void cacheHit_noChanges_skipsSyncUser() throws Exception {
            setAuth(defaultJwt());
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", "User One", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("email changed: syncUser is called")
        void cacheHit_emailChanged_callsSyncUser() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, "tenant1", X_USER_ID, "user1",
                    X_USER_EMAIL, "new@example.com", X_USER_NAME, "User One", X_TENANT_NAME, "Tenant One")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("old@example.com", "User One", false));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile("new@example.com", "User One", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser("user1", "new@example.com", "User One", "tenant1", "Tenant One", false);
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("displayName changed: syncUser is called")
        void cacheHit_displayNameChanged_callsSyncUser() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, "tenant1", X_USER_ID, "user1",
                    X_USER_EMAIL, "u@example.com", X_USER_NAME, "New Name", X_TENANT_NAME, "Tenant One")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", "Old Name", false));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile("u@example.com", "New Name", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser("user1", "u@example.com", "New Name", "tenant1", "Tenant One", false);
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("displayName null in cache but present in JWT: syncUser is called")
        void cacheHit_displayNameNullInCache_callsSyncUser() throws Exception {
            setAuth(defaultJwt());
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", null, false));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile("u@example.com", "User One", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser(any(), any(), any(), any(), any(), anyBoolean());
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("promoted to admin: syncUser is called")
        void cacheHit_promotedToAdmin_callsSyncUser() throws Exception {
            setAuthWithAdminRole(defaultJwt());
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", "User One", false));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile("u@example.com", "User One", true));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser("user1", "u@example.com", "User One", "tenant1", "Tenant One", true);
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("demoted from admin: syncUser is called")
        void cacheHit_demotedFromAdmin_callsSyncUser() throws Exception {
            setAuth(defaultJwt()); // no ROLE_TENANT_ADMIN authority
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", "User One", true));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile("u@example.com", "User One", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser("user1", "u@example.com", "User One", "tenant1", "Tenant One", false);
            verify(chain).doFilter(any(), any());
        }
    }

    // -------------------------------------------------------- claim validation

    @Nested
    @DisplayName("claim validation")
    class ClaimValidation {

        @Test
        @DisplayName("missing keycloakUserId: exception swallowed, chain continues")
        void missingKeycloakUserId_chainContinues() throws Exception {
            setAuth(buildJwt(Map.of(X_TENANT_ID, "tenant1", X_USER_EMAIL, "u@example.com")));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("missing tenantId: exception swallowed, chain continues")
        void missingTenantId_chainContinues() throws Exception {
            setAuth(buildJwt(Map.of(X_USER_ID, "user1", X_USER_EMAIL, "u@example.com")));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("missing email: logs warn but sync proceeds (no throw)")
        void missingEmail_syncProceeds() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, "tenant1", X_USER_ID, "user1",
                    X_USER_NAME, "User One", X_TENANT_NAME, "Tenant One")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenThrow(new IntegrationNotFoundException("not found"));
            when(userProfileService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(cachedProfile(null, "User One", false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService).syncUser(any(), any(), any(), any(), any(), anyBoolean());
            verify(chain).doFilter(any(), any());
        }
    }

    // -------------------------------------------------------- tenant claim formats

    @Nested
    @DisplayName("tenant_id claim formats")
    class TenantClaimFormats {

        @Test
        @DisplayName("tenant_id as plain String: used as-is")
        void tenantIdAsString_usedAsIs() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, "tenant1", X_USER_ID, "user1",
                    X_USER_EMAIL, "u@example.com", X_TENANT_NAME, "T")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", null, false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("tenant_id String with leading slash: slash stripped")
        void tenantIdStringWithLeadingSlash_stripped() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, "/tenant1", X_USER_ID, "user1",
                    X_USER_EMAIL, "u@example.com", X_TENANT_NAME, "T")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", null, false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("tenant_id as List<String>: first element used")
        void tenantIdAsList_firstElementUsed() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, List.of("tenant1", "other"), X_USER_ID, "user1",
                    X_USER_EMAIL, "u@example.com", X_TENANT_NAME, "T")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", null, false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("tenant_id as List<String> with leading slash: slash stripped from first element")
        void tenantIdAsListWithLeadingSlash_stripped() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, List.of("/tenant1"), X_USER_ID, "user1",
                    X_USER_EMAIL, "u@example.com", X_TENANT_NAME, "T")));
            when(userProfileService.getUserByKeycloakId("user1", "tenant1"))
                    .thenReturn(cachedProfile("u@example.com", null, false));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("tenant_id as empty List: treated as missing, chain continues")
        void tenantIdAsEmptyList_chainContinues() throws Exception {
            setAuth(buildJwt(Map.of(
                    X_TENANT_ID, List.of(), X_USER_ID, "user1", X_USER_EMAIL, "u@example.com")));

            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            verify(chain).doFilter(any(), any());
            verify(userProfileService, never()).syncUser(any(), any(), any(), any(), any(), anyBoolean());
        }
    }
}
