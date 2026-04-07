package com.integration.management.constants;

public final class KwConstants {

    // Kw GraphQL Token Cache
    public static final String KW_GRAPHQL_TOKEN_CACHE_KEY = "kw_graphql_token";

    // Document Subtype Prefixes
    public static final String DOCUMENT_DRAFT_PREFIX = "DOCUMENT_DRAFT_";

    public static final String DOCUMENT_FINAL_DYNAMIC = "DOCUMENT_FINAL_DYNAMIC";

    private static final String ARCGIS_ITEM_TYPE_DOCUMENT = "DOCUMENT";

    private KwConstants() {
        throw new IllegalStateException("KwConstants is a utility class and cannot be instantiated");
    }
}
