package com.integration.management.constants;

/**
 * Security-related constants for Integration Management Service.
 * Includes authentication, authorization, and multi-tenant security concerns test -8.
 */
public final class ManagementSecurityConstants {

    // Keycloak token fields
    public static final String X_TENANT_ID = "tenant_id";
    public static final String X_TENANT_NAME = "tenant_name";
    public static final String X_USER_ID = "preferred_username";
    public static final String X_USER_EMAIL = "email";
    public static final String X_USER_NAME = "name";

    // Global tenant identifier
    public static final String GLOBAL = "GLOBAL";

    // System user identifier for automated operations
    public static final String SYSTEM_USER = "system";

    // Security Role Constants
    public static final String ROLE_APP_ADMIN = "app_admin";
    public static final String ROLE_TENANT_ADMIN = "tenant_admin";
    public static final String ROLE_FEATURE_JIRA_WEBHOOK = "feature_jira_webhook";
    public static final String ROLE_FEATURE_ARCGIS_INTEGRATION = "feature_arcgis_integration";
    public static final String ROLE_FEATURE_CONFLUENCE_INTEGRATION = "feature_confluence_integration";

    private ManagementSecurityConstants() {
        throw new IllegalStateException("ManagementSecurityConstants is a utility class and cannot be instantiated");
    }
}
