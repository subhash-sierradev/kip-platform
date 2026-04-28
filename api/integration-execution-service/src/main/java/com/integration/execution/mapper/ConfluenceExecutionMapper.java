package com.integration.execution.mapper;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.model.ConfluenceJobExecutionResult.PublishedPage;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper for building {@link ConfluenceExecutionResult} from the execution command
 * and the orchestrator result.
 */
@Mapper(componentModel = "spring")
public interface ConfluenceExecutionMapper {

    default ConfluenceExecutionResult toExecutionResult(
            final ConfluenceExecutionCommand command,
            final ConfluenceJobExecutionResult orchResult,
            final Instant startedAt) {
        JobExecutionStatus status = orchResult.errorMessage() == null
                ? JobExecutionStatus.SUCCESS : JobExecutionStatus.FAILED;
        return ConfluenceExecutionResult.builder()
                .jobExecutionId(command.getJobExecutionId())
                .status(status)
                .startedAt(startedAt)
                .completedAt(Instant.now())
                .totalRecords(orchResult.totalRecords())
                .executionMetadata(buildExecutionMetadata(orchResult))
                .errorMessage(orchResult.errorMessage())
                .build();
    }

    default Map<String, String> buildExecutionMetadata(final ConfluenceJobExecutionResult orchResult) {
        List<PublishedPage> pages = orchResult.publishedPages();
        if (pages == null || pages.isEmpty()) {
            return Map.of();
        }

        Map<String, String> metadata = new HashMap<>();

        // Per-language entries: confluencePageUrl_en, confluencePageUrl_ja, etc.
        for (PublishedPage page : pages) {
            String lang = page.languageCode() != null ? page.languageCode() : "unknown";
            if (page.pageUrl() != null) {
                metadata.put("confluencePageUrl_" + lang, page.pageUrl());
            }
            metadata.put("confluencePageId_" + lang, page.pageId() != null ? page.pageId() : "");
        }

        // Backward-compatible keys point to the English (first) page
        PublishedPage first = pages.get(0);
        if (first.pageUrl() != null) {
            metadata.put("confluencePageUrl", first.pageUrl());
        }
        metadata.put("confluencePageId", first.pageId() != null ? first.pageId() : "");

        return Map.copyOf(metadata);
    }
}
