package com.integration.execution.constants;

/**
 * HTTP/REST-related constants for Integration Execution Service.
 * Includes HTTP headers, content types, and authorization schemes test-9.
 */
public final class HttpConstants {

    // Authorization Headers
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String BASIC = "Basic ";

    // Content Types
    public static final String JSON_CONTENT_TYPE = "application/json";

    private HttpConstants() {
        throw new IllegalStateException("HttpConstants is a utility class and cannot be instantiated");
    }
}
