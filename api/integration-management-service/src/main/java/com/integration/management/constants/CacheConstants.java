package com.integration.management.constants;

/**
 * Cache-related constants for Integration Management Service.
 * Includes cache names and cache management identifiers.
 */
public final class CacheConstants {

    // Cache Constants
    public static final String ALL_CACHE = "allCaches";

    // Notification admin caches
    public static final String NOTIFICATION_EVENTS_BY_TENANT_CACHE = "notificationEventsByTenantCache";
    public static final String NOTIFICATION_RULES_BY_TENANT_CACHE = "notificationRulesByTenantCache";
    public static final String NOTIFICATION_TEMPLATES_BY_TENANT_CACHE = "notificationTemplatesByTenantCache";
    public static final String NOTIFICATION_POLICIES_BY_TENANT_CACHE = "notificationPoliciesByTenantCache";

    // Security caches
    public static final String IP_VALIDATION_CACHE = "ipValidationCache";

    private CacheConstants() {
        throw new IllegalStateException("CacheConstants is a utility class and cannot be instantiated");
    }
}
