package com.integration.management.config;

import com.integration.management.entity.UserProfile;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.integration.management.constants.ManagementSecurityConstants.ROLE_TENANT_ADMIN;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_NAME;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_EMAIL;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileSyncFilter extends OncePerRequestFilter {

    private final UserProfileService userProfileService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
                try {
                    processUserSync(jwtAuthToken);
                } catch (Exception e) {
                    log.warn("User sync failed for token, continuing request: {}", e.getMessage());
                }
            }
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private void processUserSync(JwtAuthenticationToken jwtAuthToken) {
        Jwt jwt = jwtAuthToken.getToken();
        String keycloakUserId = jwt.getClaimAsString(X_USER_ID);
        String email = jwt.getClaimAsString(X_USER_EMAIL);
        String displayName = jwt.getClaimAsString(X_USER_NAME);
        String tenantId = getTenant(jwt);
        String tenantName = jwt.getClaimAsString(X_TENANT_NAME);
        validateRequiredClaims(keycloakUserId, email, tenantId);
        boolean isTenantAdmin = jwtAuthToken.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + ROLE_TENANT_ADMIN).equals(a.getAuthority()));

        try {
            UserProfile cached = userProfileService.getUserByKeycloakId(keycloakUserId, tenantId);
            if (!isProfileStale(cached, email, displayName, isTenantAdmin)) {
                return;
            }
            log.debug("User profile stale for user: {} tenant: {}, syncing", keycloakUserId, tenantId);
        } catch (IntegrationNotFoundException e) {
            log.debug("New user detected for keycloakUserId: {} tenant: {}, syncing", keycloakUserId, tenantId);
        }

        UserProfile userProfile = userProfileService.syncUser(
                keycloakUserId, email, displayName, tenantId, tenantName, isTenantAdmin);
        log.debug("User sync completed for user: {} tenant: {}, isTenantAdmin: {}",
                userProfile.getKeycloakUserId(), userProfile.getTenantId(), userProfile.isTenantAdmin());
    }

    private boolean isProfileStale(UserProfile cached, String email, String displayName, boolean isTenantAdmin) {
        return cached.isTenantAdmin() != isTenantAdmin
                || !cached.getEmail().equals(email)
                || !Objects.equals(cached.getDisplayName(), displayName);
    }

    private String getTenant(Jwt jwt) {
        Object claim = jwt.getClaim(X_TENANT_ID);
        if (claim instanceof String tenantId) {
            return tenantId.replaceFirst("^/+", "");
        }
        if (claim instanceof List<?> list && !list.isEmpty()) {
            Object first = list.getFirst();
            if (first instanceof String tenantId) {
                return tenantId.replaceFirst("^/+", "");
            }
        }
        return null;
    }

    private void validateRequiredClaims(String keycloakUserId, String email, String tenantId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            log.error("JWT missing preferred_username claim, cannot sync user");
            throw new IllegalArgumentException("JWT missing preferred_username claim");
        }
        if (email == null || email.isBlank()) {
            log.error("JWT missing email claim, cannot sync user");
        }
        if (tenantId == null || tenantId.isBlank()) {
            log.error("JWT missing tenant_id claim, cannot sync user");
            throw new IllegalArgumentException("JWT missing tenant_id claim");
        }
    }
}
