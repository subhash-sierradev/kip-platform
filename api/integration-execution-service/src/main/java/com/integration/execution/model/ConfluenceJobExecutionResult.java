package com.integration.execution.model;

/**
 * Encapsulates the result of a single Confluence page build-and-publish cycle.
 */
public record ConfluenceJobExecutionResult(
        int totalRecords,
        String confluencePageUrl,
        String confluencePageId,
        String errorMessage
) {

    public static ConfluenceJobExecutionResult success(
            final int totalRecords,
            final String confluencePageUrl,
            final String confluencePageId) {
        return new ConfluenceJobExecutionResult(totalRecords,  confluencePageUrl, confluencePageId, null);
    }

    public static ConfluenceJobExecutionResult failed(final String errorMessage) {
        return new ConfluenceJobExecutionResult(0, null, null, errorMessage);
    }
}
