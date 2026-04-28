package com.integration.execution.model;

import java.util.List;

/**
 * Encapsulates the result of a Confluence execution pipeline run.
 *
 * <p>A single execution may publish multiple Confluence pages — one per language
 * configured on the integration (English source page + one page per target language).
 * {@link #publishedPages()} holds the full list; {@link #confluencePageUrl()} and
 * {@link #confluencePageId()} return the first page's values for backward compatibility.</p>
 */
public record ConfluenceJobExecutionResult(
        int totalRecords,
        List<PublishedPage> publishedPages,
        String errorMessage
) {

    /**
     * A single Confluence page published during the execution.
     *
     * @param languageCode BCP-47 language code of the page (e.g. {@code "en"}, {@code "ja"})
     * @param pageUrl      full Confluence URL to the page
     * @param pageId       Confluence page ID
     */
    public record PublishedPage(String languageCode, String pageUrl, String pageId) {
    }

    /** Factory for a successful multi-page publish. */
    public static ConfluenceJobExecutionResult success(
            final int totalRecords,
            final List<PublishedPage> publishedPages) {
        return new ConfluenceJobExecutionResult(totalRecords, publishedPages, null);
    }

    /** Factory for an execution that produced no data (empty window). */
    public static ConfluenceJobExecutionResult successEmpty() {
        return new ConfluenceJobExecutionResult(0, List.of(), null);
    }

    public static ConfluenceJobExecutionResult failed(final String errorMessage) {
        return new ConfluenceJobExecutionResult(0, List.of(), errorMessage);
    }

    // -----------------------------------------------------------------------
    // Backward-compatible accessors (first page URL / ID)
    // -----------------------------------------------------------------------

    /** URL of the first published page, or {@code null} if none. */
    public String confluencePageUrl() {
        return publishedPages != null && !publishedPages.isEmpty()
                ? publishedPages.get(0).pageUrl() : null;
    }

    /** ID of the first published page, or {@code null} if none. */
    public String confluencePageId() {
        return publishedPages != null && !publishedPages.isEmpty()
                ? publishedPages.get(0).pageId() : null;
    }
}
