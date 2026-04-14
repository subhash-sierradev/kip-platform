package com.integration.execution.service.processor;

import com.integration.execution.model.ConfluenceMonitoringReport;
import com.integration.execution.model.ConfluenceMonitoringReport.ClientGroup;
import com.integration.execution.model.ConfluenceMonitoringReport.DynamicField;
import com.integration.execution.model.ConfluenceMonitoringReport.PrioritySummaryEntry;
import com.integration.execution.model.ConfluenceMonitoringReport.ReportEntry;
import com.integration.execution.model.KwMonitoringDocument;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.integration.execution.constants.KasewareConstants.DYNAMIC_DATA_FIELD;

/**
 * Renders Confluence Storage Format XHTML pages from monitoring data.
 * Transforms {@link KwMonitoringDocument} into presentation models and applies FreeMarker templates
 * to generate Confluence-compatible page content.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluencePageRenderer {

    private static final DateTimeFormatter GENERATED_AT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm 'UTC'", Locale.ENGLISH);
    private static final DateTimeFormatter REPORT_DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter REPORT_DATE_LONG_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm 'UTC'", Locale.ENGLISH);
    private static final List<String> PRIORITY_ORDER = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO");
    private static final String UNASSIGNED_CLIENT = "Unassigned";
    private static final Set<String> VALID_PRIORITIES = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO");

    private final Configuration freemarkerConfig;
    private ZoneId timezone = ZoneOffset.UTC;


    public String buildPageContent(List<KwMonitoringDocument> data, ZoneId targetTimezone) {
        ConfluenceMonitoringReport model = buildModel(data, targetTimezone);
        try {
            Template template = freemarkerConfig.getTemplate("monitoring_data_report.ftl");
            StringWriter writer = new StringWriter();
            template.process(Map.of("model", model), writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render monitoring data page template", e);
        }
    }

    private ConfluenceMonitoringReport buildModel(List<KwMonitoringDocument> data, ZoneId targetTimezone) {
        this.timezone = targetTimezone;
        var now = Instant.now().atZone(targetTimezone);
        DateTimeFormatter generatedAtFmt = DateTimeFormatter.ofPattern(
                "dd MMM yyyy 'at' HH:mm z", Locale.ENGLISH);
        String generatedAt = now.format(generatedAtFmt);
        String reportDate = now.format(REPORT_DATE_FMT);
        String reportDateLong = now.format(REPORT_DATE_LONG_FMT);

        // Separate named clients (alphabetical) from unassigned
        Map<String, List<KwMonitoringDocument>> namedClients = new TreeMap<>();
        List<KwMonitoringDocument> unassignedDocs = new ArrayList<>();

        for (KwMonitoringDocument item : data) {
            String clientName = extractClientName(item);
            if (UNASSIGNED_CLIENT.equals(clientName)) {
                unassignedDocs.add(item);
            } else {
                namedClients.computeIfAbsent(clientName, k -> new ArrayList<>()).add(item);
            }
        }

        // Build groups: named clients first (alphabetical), unassigned appended last
        List<ClientGroup> clientGroups = new ArrayList<>();
        List<ReportEntry> allReports = new ArrayList<>();
        int unrecognizedPriorityCount = 0;

        for (Map.Entry<String, List<KwMonitoringDocument>> entry : namedClients.entrySet()) {
            List<ReportEntry> groupReports = entry.getValue().stream()
                    .map(this::toReportEntry).toList();
            allReports.addAll(groupReports);
            unrecognizedPriorityCount += countUnrecognizedPriorities(groupReports);
            List<PrioritySummaryEntry> clientPriority = buildPrioritySummary(groupReports);
            clientGroups.add(new ClientGroup(
                    entry.getKey(), groupReports.size(), clientPriority, groupReports, false));
        }

        int unassignedCount = unassignedDocs.size();
        if (!unassignedDocs.isEmpty()) {
            List<ReportEntry> unassignedReports = unassignedDocs.stream()
                    .map(this::toReportEntry).toList();
            allReports.addAll(unassignedReports);
            unrecognizedPriorityCount += countUnrecognizedPriorities(unassignedReports);
            List<PrioritySummaryEntry> unassignedPriority = buildPrioritySummary(unassignedReports);
            clientGroups.add(new ClientGroup(
                    UNASSIGNED_CLIENT, unassignedReports.size(), unassignedPriority, unassignedReports, true));
        }

        if (unassignedCount > 0) {
            log.warn("Confluence report: {} document(s) have no resolved Client field — grouped as Unassigned",
                    unassignedCount);
        }
        if (unrecognizedPriorityCount > 0) {
            log.warn("Confluence report: {} document(s) have unrecognized Priority values",
                    unrecognizedPriorityCount);
        }

        List<PrioritySummaryEntry> prioritySummary = buildPrioritySummary(allReports);
        return new ConfluenceMonitoringReport(
                reportDate, reportDateLong, generatedAt,
                data.size(), namedClients.size(), prioritySummary, clientGroups,
                unassignedCount, unrecognizedPriorityCount);
    }

    @SuppressWarnings("unchecked")
    private String extractClientName(KwMonitoringDocument item) {
        Map<String, Object> attributes = item.getAttributes() != null ? item.getAttributes() : Map.of();
        Map<String, Object> dynamicData = attributes.containsKey(DYNAMIC_DATA_FIELD)
                ? (Map<String, Object>) attributes.get(DYNAMIC_DATA_FIELD) : Map.of();
        String client = resolveStringValue(dynamicData.get("Client"));
        return (client != null && !client.isBlank()) ? client : UNASSIGNED_CLIENT;
    }

    @SuppressWarnings("unchecked")
    private ReportEntry toReportEntry(KwMonitoringDocument item) {
        Map<String, Object> attributes = item.getAttributes() != null ? item.getAttributes() : Map.of();
        Map<String, Object> dynamicData = attributes.containsKey(DYNAMIC_DATA_FIELD)
                ? (Map<String, Object>) attributes.get(DYNAMIC_DATA_FIELD) : Map.of();

        String client = resolveStringValue(dynamicData.get("Client"));
        String priority = resolveStringValue(dynamicData.get("Priority"));
        if (priority == null || priority.isBlank()) {
            priority = "Info";
        }
        String normalizedPriority = priority.toUpperCase(Locale.ENGLISH);

        boolean unrecognizedPriority = !VALID_PRIORITIES.contains(normalizedPriority);
        if (unrecognizedPriority) {
            log.debug("Unrecognized priority '{}' in document id={}", priority, item.getId());
        }

        String title = item.getTitle();
        if (title == null || title.isBlank()) {
            title = "[Untitled – " + item.getId() + "]";
        }

        List<DynamicField> dynamicFields = buildDynamicFields(dynamicData);
        List<String> authors = extractAuthors(attributes);
        List<String> serialNumbers = extractSerials(attributes);
        List<String> tags = extractTags(attributes);

        String createdAt = formatTimestamp(item.getCreatedTimestamp());
        String updatedAt = formatTimestamp(item.getUpdatedTimestamp());

        return new ReportEntry(
                title,
                client != null ? client : UNASSIGNED_CLIENT,
                priority,
                resolveLevelColour(normalizedPriority),
                resolveLevelBgColour(normalizedPriority),
                resolveLevelBorderColour(normalizedPriority),
                resolveDot(normalizedPriority),
                item.getBody() != null ? item.getBody() : "",
                createdAt,
                updatedAt,
                dynamicFields,
                authors,
                serialNumbers,
                tags,
                unrecognizedPriority
        );
    }

    private List<DynamicField> buildDynamicFields(Map<String, Object> dynamicData) {
        List<DynamicField> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dynamicData.entrySet()) {
            String key = entry.getKey();
            if ("Client".equals(key) || "Priority".equals(key)) {
                continue;
            }
            String value = resolveStringValue(entry.getValue());
            if (value != null) {
                fields.add(new DynamicField(key, value));
            }
        }
        return fields;
    }

    private String resolveStringValue(Object raw) {
        switch (raw) {
            case null -> {
                return null;
            }
            case String s -> {
                return s;
            }
            case Map<?, ?> map -> {
                // Nested checkbox-group values like {"Region/Country": "United States"}
                for (Object v : map.values()) {
                    if (v instanceof String s) {
                        return s;
                    }
                }
            }
            default -> {
            }
        }
        return raw.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAuthors(Map<String, Object> attributes) {
        Object raw = attributes.get("authors");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<String, Object>) o)
                    .map(m -> String.valueOf(m.getOrDefault("displayFullName", "")))
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractSerials(Map<String, Object> attributes) {
        Object raw = attributes.get("serials");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<String, Object>) o)
                    .map(m -> String.valueOf(m.getOrDefault("filingNumberDisplay", "")))
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    private List<String> extractTags(Map<String, Object> attributes) {
        Object raw = attributes.get("tags");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private String formatTimestamp(long epochSeconds) {
        if (epochSeconds <= 0) {
            return "";
        }
        DateTimeFormatter timestampFmt = DateTimeFormatter.ofPattern(
                "dd MMM yyyy HH:mm z", Locale.ENGLISH);
        return Instant.ofEpochSecond(epochSeconds).atZone(timezone).format(timestampFmt);
    }

    private List<PrioritySummaryEntry> buildPrioritySummary(List<ReportEntry> reports) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String level : PRIORITY_ORDER) {
            counts.put(level, 0);
        }
        for (ReportEntry r : reports) {
            String key = r.priority().toUpperCase(Locale.ENGLISH);
            counts.merge(key, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new PrioritySummaryEntry(e.getKey(), resolveLevelColour(e.getKey()), e.getValue()))
                .toList();
    }

    private int countUnrecognizedPriorities(List<ReportEntry> reports) {
        return (int) reports.stream().filter(ReportEntry::unrecognizedPriority).count();
    }

    private String resolveLevelColour(String level) {
        return switch (level) {
            case "CRITICAL", "HIGH" -> "Red";
            case "MEDIUM" -> "Yellow";
            case "LOW" -> "Green";
            case "INFO" -> "Blue";
            default -> "Grey";
        };
    }

    private String resolveDot(String level) {
        return switch (level) {
            case "CRITICAL", "HIGH" -> "\uD83D\uDD34";
            case "MEDIUM" -> "\uD83D\uDFE1";
            case "LOW" -> "\uD83D\uDFE2";
            case "INFO" -> "\uD83D\uDD35";
            default -> "⚪";
        };
    }

    private String resolveLevelBgColour(String level) {
        return switch (level) {
            case "CRITICAL", "HIGH" -> "#FFEBE6";
            case "MEDIUM" -> "#FFF4E5";
            case "LOW" -> "#E3FCEF";
            case "INFO" -> "#E6F3FF";
            default -> "#F4F5F7";
        };
    }

    private String resolveLevelBorderColour(String level) {
        return switch (level) {
            case "CRITICAL" -> "#BF2600";
            case "HIGH" -> "#DE350B";
            case "MEDIUM" -> "#FF8B00";
            case "LOW" -> "#006644";
            case "INFO" -> "#0052CC";
            default -> "#97A0AF";
        };
    }
}
