package com.integration.management.constants;

public final class IntegrationManagementConstants {

    // ======================================================
    // General Status Constants
    // ======================================================
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String ROOT_FOLDER_KEY = "ROOT";

    // ======================================================
    // Integration Type Constants
    // ======================================================
    public static final String INTEGRATION_TYPE_ARCGIS = "ARCGIS";
    public static final String INTEGRATION_TYPE_CONFLUENCE = "CONFLUENCE";

    // ===============================================
    // Site Configuration Keys for Initial Sync
    // ===============================================
    public static final String CONFIG_KEY_ARCGIS_INITIAL_SYNC_START = "ARCGIS_INITIAL_SYNC_START_TIMESTAMP";
    public static final String CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START = "CONFLUENCE_INITIAL_SYNC_START_TIMESTAMP";

    // ===============================
    // Security Related Constants
    // ===============================
    public static final class Security {
        // Global tenant identifier
        public static final String GLOBAL = "GLOBAL";

        // System user identifier for automated operations
        public static final String SYSTEM_USER = "system";

        // Security Role Constants
        public static final String ROLE_APP_ADMIN = "app_admin";
        public static final String ROLE_TENANT_ADMIN = "tenant_admin";
        public static final String ROLE_FEATURE_JIRA_WEBHOOK = "feature_jira_webhook";
        public static final String ROLE_FEATURE_ARCGIS_INTEGRATION = "feature_arcgis_integration";

        private Security() {
        }
    }

    // ===============================
    // HTTP Header / Token Fields
    // ===============================
    public static final class Headers {
        public static final String TENANT_ID = "tenant_id";
        public static final String TENANT_NAME = "tenant_name";
        public static final String USER_ID = "preferred_username";
        public static final String USER_EMAIL = "email";
        public static final String USER_NAME = "name";

        private Headers() {
        }
    }

    private IntegrationManagementConstants() {
    }

}