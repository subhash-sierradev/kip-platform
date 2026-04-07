package com.integration.execution.contract.rest.response;

/**
 * Generic API response for connection test operations.
 */
public record ApiResponse(
        int statusCode,
        boolean success,
        String message
) {
}
