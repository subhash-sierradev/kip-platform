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

    private KasewareConstants() {
        throw new IllegalStateException("KasewareConstants is a utility class and cannot be instantiated");
    }
}
