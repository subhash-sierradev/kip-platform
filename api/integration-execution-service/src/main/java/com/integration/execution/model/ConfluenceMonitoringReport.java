package com.integration.execution.model;

import java.util.List;

public record ConfluenceMonitoringReport(
        String reportDate,
        String reportDateLong,
        String generatedAt,
        int totalReports,
        int clientCount,
        List<PrioritySummaryEntry> prioritySummary,
        List<ClientGroup> clientGroups,
        int unassignedCount,
        int unrecognizedPriorityCount
) {

    public record PrioritySummaryEntry(String level, String colour, int count) {
    }

    public record ClientGroup(
            String clientName,
            int totalReports,
            List<PrioritySummaryEntry> priorityCounts,
            List<ReportEntry> reports,
            boolean unassigned
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
            List<String> tags,
            boolean unrecognizedPriority
    ) {
    }

    public record DynamicField(String label, String value) {
    }
}
