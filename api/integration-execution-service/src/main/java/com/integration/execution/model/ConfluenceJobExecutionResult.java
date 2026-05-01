package com.integration.execution.model;

/**
 * Encapsulates the result of a Confluence execution pipeline run.
 */
public record ConfluenceJobExecutionResult(
        int totalRecords,
        String pageUrl,
        String pageId,
        String errorMessage
) {

    /** Factory for a successful single-page publish. */
    public static ConfluenceJobExecutionResult success(
            final int totalRecords,
            final String pageUrl,
            final String pageId) {
        return new ConfluenceJobExecutionResult(totalRecords, pageUrl, pageId, null);
    }

    /** Factory for an execution that produced no data (empty window). */
    public static ConfluenceJobExecutionResult successEmpty() {
        return new ConfluenceJobExecutionResult(0, null, null, null);
    }

    public static ConfluenceJobExecutionResult failed(final String errorMessage) {
        return new ConfluenceJobExecutionResult(0, null, null, errorMessage);
    }
}
