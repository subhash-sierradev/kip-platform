package com.integration.execution.model;

import java.util.List;
import java.util.Map;

public record ConfluenceMonitoringReport(
        String reportDate,
        String reportDateLong,
        String generatedAt,
        int totalReports,
        int clientCount,
        List<PrioritySummaryEntry> prioritySummary,
        List<ClientGroup> clientGroups,
        List<LanguageSection> languageSections
) {

    public record PrioritySummaryEntry(String level, String colour, int count) {
    }

    public record ClientGroup(
            String clientName,
            int totalReports,
            List<PrioritySummaryEntry> priorityCounts,
            List<ReportEntry> reports
    ) {
    }

    public record ReportEntry(
            String title,
            String client,
            String priority,
            String priorityColour,
            String bgColour,
            String borderColour,
            String dot,
            String body,
            String createdAt,
            String updatedAt,
            List<DynamicField> dynamicFields,
            List<String> authors,
            List<String> serialNumbers,
            List<String> tags
    ) {
    }

    public record DynamicField(String label, String value) {
    }

    /**
     * Represents one language section within a combined multi-language report page.
     *
     * @param languageCode        BCP-47 language code (e.g. {@code "en"}, {@code "ja"})
     * @param languageDisplayName human-readable section heading
     * @param prioritySummary     per-priority counts for this language's records
     * @param clientGroups        client groups containing the translated report entries
     * @param uiLabels            translated UI strings for this section.
     *                            Keys use lowercase-with-underscores for structural headings
     *                            (e.g. {@code "priority"}, {@code "report_details"}) and
     *                            UPPERCASE for priority value labels (e.g. {@code "HIGH"},
     *                            {@code "INFO"}).  The FreeMarker template reads these via
     *                            {@code uiLabels["key"]} — no hardcoded translated strings
     *                            remain in the template.
     */
    public record LanguageSection(
            String languageCode,
            String languageDisplayName,
            List<PrioritySummaryEntry> prioritySummary,
            List<ClientGroup> clientGroups,
            Map<String, String> uiLabels
    ) {
    }
}
