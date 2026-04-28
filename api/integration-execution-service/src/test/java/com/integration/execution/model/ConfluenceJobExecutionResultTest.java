package com.integration.execution.model;

import com.integration.execution.model.ConfluenceJobExecutionResult.PublishedPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConfluenceJobExecutionResult} accessor methods,
 * mainly to cover the null/empty publishedPages branches.
 */
class ConfluenceJobExecutionResultTest {

    @Test
    void confluencePageUrl_andPageId_withNullPublishedPages_returnNull() {
        // Direct constructor call with null list — covers the `publishedPages != null` false branch
        // in both confluencePageUrl() and confluencePageId().
        ConfluenceJobExecutionResult result = new ConfluenceJobExecutionResult(0, null, null);

        assertThat(result.confluencePageUrl()).isNull();
        assertThat(result.confluencePageId()).isNull();
    }

    @Test
    void confluencePageId_withEmptyPublishedPages_returnsNull() {
        // successEmpty() result — covers `!publishedPages.isEmpty()` false branch
        // for confluencePageId() (which had no existing test calling it on an empty list).
        ConfluenceJobExecutionResult result = ConfluenceJobExecutionResult.successEmpty();

        assertThat(result.confluencePageId()).isNull();
        assertThat(result.confluencePageUrl()).isNull();
    }

    @Test
    void confluencePageUrl_andPageId_withNonEmptyPages_returnFirstPageValues() {
        PublishedPage page = new PublishedPage("en", "https://example.com/page/1", "page-1");
        ConfluenceJobExecutionResult result = ConfluenceJobExecutionResult.success(5, List.of(page));

        assertThat(result.confluencePageUrl()).isEqualTo("https://example.com/page/1");
        assertThat(result.confluencePageId()).isEqualTo("page-1");
    }
}

