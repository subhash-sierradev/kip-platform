package com.integration.management.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.management.security.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.info("Processing authentication filter for request: {}", request.getRequestURI());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            log.error("No valid JWT authentication found in security context");
            writeError(response, securityProperties.getError().getMissingAuth(), HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Jwt jwt = jwtAuth.getToken();
        try {
            String tenantId = getTenant(jwt);
            String userId = jwt.getClaimAsString(securityProperties.getJwt().getUserIdClaim());

            if (StringUtils.hasText(tenantId) && StringUtils.hasText(userId)) {
                // Normal path: both tenantId and userId are present
                request.setAttribute(securityProperties.getJwt().getTenantIdClaim(), tenantId);
                request.setAttribute(securityProperties.getJwt().getUserIdClaim(), userId);
                filterChain.doFilter(request, response);
            } else if (isWebhookClient(jwt)) {
                // Webhook client path: allow system access for client-credentials tokens with webhook_client role
                log.info("Allowing webhook client access - using system tenant and user");
                request.setAttribute(securityProperties.getJwt().getTenantIdClaim(),
                        securityProperties.getWebhook().getTenantId());
                request.setAttribute(securityProperties.getJwt().getUserIdClaim(),
                        securityProperties.getWebhook().getUserId());
                filterChain.doFilter(request, response);
            } else {
                // Reject: missing required claims and not a valid webhook client token
                log.error("Missing required claims: tenantId or userId, and token is not a valid webhook client");
                writeError(response, securityProperties.getError().getMissingFields(),
                        HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Exception e) {
            log.error("Error processing JWT token claims", e);
            writeError(response, securityProperties.getError().getInvalidToken(), HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isWebhookClient(Jwt jwt) {
        try {
            Object realmAccessObj = jwt.getClaim("realm_access");
            if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) {
                return false;
            }

            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof List<?> roles)) {
                return false;
            }
            return roles.stream()
                    .anyMatch(role -> securityProperties.getWebhook().getClientRole().equals(role));
        } catch (Exception ex) {
            log.error("Error checking realm access roles", ex);
            return false;
        }
    }

    // TODO : Have to remove this method once all the clients are sending tenant_id as string
    private String getTenant(Jwt jwt) {
        String tenantIdClaim = securityProperties.getJwt().getTenantIdClaim();
        Object rawClaim = jwt.getClaim(tenantIdClaim);
        if (rawClaim instanceof String s && StringUtils.hasText(s)) {
            return s.replaceFirst("^/+", "");
        }
        if (rawClaim instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s && StringUtils.hasText(s)) {
                    return s.replaceFirst("^/+", "");
                }
            }
        }
        return null;
    }

    private void writeError(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = objectMapper.writeValueAsString(Map.of("error", message));
        response.getWriter().write(body);
    }
}
