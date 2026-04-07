package com.integration.execution.constants;

/**
 * Security-related constants for Integration Execution Service.
 * Includes authentication, authorization, and multi-tenant security concerns.
 */
public final class ExecutionSecurityConstants {

    // Keycloak token fields
    public static final String X_TENANT_ID = "tenant_id";
    public static final String X_USER_ID = "preferred_username";

    // Global tenant identifier
    public static final String GLOBAL = "GLOBAL";

    // System user identifier for automated operations
    public static final String SYSTEM_USER = "system";

    // Security Role Constants
    public static final String ROLE_APP_ADMIN = "app_admin";
    public static final String ROLE_TENANT_ADMIN = "tenant_admin";
    public static final String ROLE_FEATURE_JIRA_WEBHOOK = "feature_jira_webhook";
    public static final String ROLE_FEATURE_ARCGIS_INTEGRATION = "feature_arcgis_integration";

    private ExecutionSecurityConstants() {
        throw new IllegalStateException("ExecutionSecurityConstants is a utility class and cannot be instantiated");
    }
}
