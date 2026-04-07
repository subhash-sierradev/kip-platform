package com.integration.management.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Security and Authentication settings.
 * Centralizes all security-related configuration in a type-safe manner.
 */
@Data
@Component
@ConfigurationProperties(prefix = "integration.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Webhook webhook = new Webhook();
    private Error error = new Error();
    private Cors cors = new Cors();
    private Csp csp = new Csp();

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

    @Data
    public static class Cors {
        private String[] allowedOriginPatterns;
        private String[] allowedMethods;
        private String[] allowedHeaders;
        private String[] exposedHeaders;
        private boolean allowCredentials = true;
        private long maxAge = 3600; // 1 hour
        private String pathPattern = "/**";
    }

    @Data
    public static class Csp {
        private boolean enabled = true;
        private String defaultSrc = "'self'";
        private String scriptSrc = "'self'";
        private String styleSrc = "'self' 'unsafe-inline'";
        private String imgSrc = "'self' data: https:";
        private String fontSrc = "'self' data:";
        private String connectSrc = "'self'";
        private String frameSrc = "'none'";
        private String objectSrc = "'none'";
        private String mediaSrc = "'self'";
        private String manifestSrc = "'self'";
        private String workerSrc = "'self'";
        private String childSrc = "'self'";
        private String formAction = "'self'";
        private String frameAncestors = "'none'";
        private String baseUri = "'self'";
        private boolean upgradeInsecureRequests;
        private boolean reportOnly;
    }
}