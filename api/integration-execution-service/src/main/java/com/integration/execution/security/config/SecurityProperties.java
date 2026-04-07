package com.integration.execution.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Security and Authentication settings.
 * Centralizes all security-related configuration in a type-safe manner.
 *
 * <p>
 * Note: CORS and CSP are intentionally excluded from Integration Execution
 * Service.
 * This is a backend-only service that should not be directly accessed by
 * browsers.
 * Only Integration Management Service (IMS) may require CORS/CSP configuration
 * to serve frontend applications.
 */
@Data
@Component
@ConfigurationProperties(prefix = "integration.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Webhook webhook = new Webhook();
    private Error error = new Error();

    @Data
    public static class Jwt {
        private String tenantIdClaim = "tenant_id";
        private String userIdClaim = "preferred_username";
    }

    @Data
    public static class Webhook {
        private String tenantId = "GLOBAL";
        private String userId = "system";
        private String clientRole = "webhook_client";
    }

    @Data
    public static class Error {
        private String missingAuth = "Missing or invalid Authorization header";
        private String missingFields = "Missing tenantId or userId in token";
        private String invalidToken = "Invalid JWT token";
    }
}
