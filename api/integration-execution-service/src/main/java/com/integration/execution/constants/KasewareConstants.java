package com.integration.execution.constants;

/**
 * Kaseware-specific constants for Integration Execution Service.
 * Includes cache keys, document types, and Kaseware integration identifiers.
 */
public final class KasewareConstants {

    // Kaseware GraphQL Token Cache
    public static final String KW_GRAPHQL_TOKEN_CACHE_KEY = "kw_graphql_token";

    // Document Subtype Prefixes
    public static final String DOCUMENT_DRAFT_PREFIX = "DOCUMENT_DRAFT_";

    public static final String DOCUMENT_FINAL_DYNAMIC = "DOCUMENT_FINAL_DYNAMIC";

    // Monitoring Document Field Keys
    /** JSON field name for the dynamic-data object in a Kaseware monitoring document. */
    public static final String DYNAMIC_DATA_FIELD = "dynamicData";

    /**
     * Key inside the {@code dynamicData} object used by Kaseware to store internal
     * hidden-field markers. This key is always excluded from resolved attribute maps.
     */
    public static final String HIDDEN_FIELDS_KEY = "hiddenFields";

    private KasewareConstants() {
        throw new IllegalStateException("KasewareConstants is a utility class and cannot be instantiated");
    }
}
