package com.integration.execution.service.processor;

import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.model.ConfluenceMonitoringReport;
import com.integration.execution.model.ConfluenceMonitoringReport.DynamicField;
import com.integration.execution.model.ConfluenceMonitoringReport.PrioritySummaryEntry;
import com.integration.execution.model.ConfluenceMonitoringReport.ReportEntry;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.integration.execution.model.ConfluenceMonitoringReport.ClientGroup;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Renders Confluence Storage Format XHTML pages from monitoring data.
 *
 * <p>UI label strings (field headings and priority values) are translated dynamically via
 * {@link KwMonitoringDataTranslator#translateLabelMaps} and stored in each
 * {@link ConfluenceMonitoringReport.LanguageSection#uiLabels()} map.  The FreeMarker
 * template consumes that map through {@code uiLabels["key"]} expressions — no
 * hardcoded translated strings exist in the template.</p>
 *
 * <p>Adding a new UI label requires only two steps:
 * <ol>
 *   <li>Add the entry to {@link #ENGLISH_UI_LABELS}.</li>
 *   <li>Reference {@code uiLabels["new_key"]} in the FreeMarker template.</li>
 * </ol>
 * No switch/case translation logic is needed anywhere.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluencePageRenderer {


    /**
     * Canonical English UI labels — the single source of truth for all translatable
     * strings that appear as headings and labels in the rendered report.
     *
     * <p>Two key naming conventions:</p>
     * <ul>
     *   <li>Structural headings use <strong>lowercase-with-underscores</strong>
     *       (e.g. {@code "report_details"}, {@code "priority"}).</li>
     *   <li>Priority-value labels use <strong>UPPERCASE</strong>
     *       (e.g. {@code "HIGH"}, {@code "INFO"}).</li>
     * </ul>
     *
     * <p>Dynamic field label keys discovered at runtime (e.g. {@code "status"},
     * {@code "region"}) are appended to this map inside
     * {@link #buildFullEnglishLabelMap(List)}.  No code changes are required when
     * new dynamic fields are added to the KW data model.</p>
     */
    static final Map<String, String> ENGLISH_UI_LABELS = Map.ofEntries(
            Map.entry("report_details",          "Report Details"),
            Map.entry("priority",                "Priority"),
            Map.entry("case_numbers",            "Case Numbers"),
            Map.entry("date",                    "Date"),
            Map.entry("client",                  "Client"),
            Map.entry("entities",                "Entities"),
            Map.entry("summary",                 "Summary"),
            Map.entry("authors",                 "Authors"),
            Map.entry("created",                 "Created"),
            Map.entry("updated",                 "Updated"),
            Map.entry("client_summary",          "Client Summary"),
            Map.entry("total_reports",           "Total Reports"),
            Map.entry("unknown",                 "Unknown"),
            // Page-level structural headings
            Map.entry("aggregated_daily_report", "Aggregated Daily Report"),
            Map.entry("report_date_label",       "Report Date"),
            Map.entry("generated_label",         "Generated"),
            Map.entry("clients_label",           "Clients"),
            Map.entry("priority_summary",        "Priority Summary"),
            Map.entry("table_of_contents",       "Table of Contents"),
            // Priority value labels — UPPERCASE keys match report.priority?upper_case in FreeMarker
            Map.entry("CRITICAL", "CRITICAL"),
            Map.entry("HIGH",     "HIGH"),
            Map.entry("MEDIUM",   "MEDIUM"),
            Map.entry("LOW",      "LOW"),
            Map.entry("INFO",     "INFO")
    );

    private static final DateTimeFormatter REPORT_DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter REPORT_DATE_LONG_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter GENERATED_AT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm z", Locale.ENGLISH);
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z");
    private static final List<String> PRIORITY_ORDER =
            List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO");

    private final Configuration freemarkerConfig;
    private final KwMonitoringDataTranslator kwMonitoringDataTranslator;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public String buildPageContent(final List<KwMonitoringDocument> data,
                                   final ZoneId targetTimezone) {
        return render(buildModel(data, targetTimezone, Locale.ENGLISH));
    }

    /**
     * Renders a single Confluence page containing one section per language.
     *
     * <p>UI labels and priority values are translated for each target language via
     * {@link KwMonitoringDataTranslator#translateLabelMaps} and stored in
     * {@link ConfluenceMonitoringReport.LanguageSection#uiLabels()}.  The FreeMarker
     * template uses {@code section.uiLabels["key"]} — no hardcoded strings in the
     * template, and adding new label keys requires zero template changes.</p>
     *
     * @param sourceData       original (untranslated) monitoring documents
     * @param translatedByLang map of BCP-47 language code → translated documents
     * @param sourceLanguage   BCP-47 source language code (e.g. {@code "en"})
     * @param targetTimezone   timezone used to format all timestamps
     * @return Confluence Storage Format XHTML for the combined page
     */
    public String buildMultiLanguagePageContent(
            final List<KwMonitoringDocument> sourceData,
            final Map<String, List<KwMonitoringDocument>> translatedByLang,
            final String sourceLanguage,
            final ZoneId targetTimezone) {

        ConfluenceMonitoringReport base = buildModel(sourceData, targetTimezone, Locale.ENGLISH);

        // Full English label map: fixed UI labels + dynamic field labels per document set.
        Map<String, String> englishLabels = buildFullEnglishLabelMap(sourceData);
        List<String> targetLangs = new ArrayList<>(translatedByLang.keySet());

        // Translate all labels for every target language in one batch pass.
        Map<String, Map<String, String>> labelsByLang =
                kwMonitoringDataTranslator.translateLabelMaps(englishLabels, sourceLanguage, targetLangs);

        List<ConfluenceMonitoringReport.LanguageSection> sections = new ArrayList<>();
        sections.add(new ConfluenceMonitoringReport.LanguageSection(
                sourceLanguage, resolveLanguageDisplayName(sourceLanguage),
                base.prioritySummary(), base.clientGroups(), englishLabels));

        for (Map.Entry<String, List<KwMonitoringDocument>> entry : translatedByLang.entrySet()) {
            String lang = entry.getKey();
            Locale langLocale = localeFromLangCode(lang);
            ConfluenceMonitoringReport langBase = buildModel(entry.getValue(), targetTimezone, langLocale);
            // Fall back to English labels gracefully when translation is unavailable.
            Map<String, String> sectionLabels = labelsByLang.getOrDefault(lang, englishLabels);
            sections.add(new ConfluenceMonitoringReport.LanguageSection(
                    lang, resolveLanguageDisplayName(lang),
                    langBase.prioritySummary(), langBase.clientGroups(), sectionLabels));
        }

        ConfluenceMonitoringReport combined = new ConfluenceMonitoringReport(
                base.reportDate(), base.reportDateLong(), base.generatedAt(),
                base.totalReports(), base.clientCount(), base.prioritySummary(),
                base.clientGroups(), sections);

        return render(combined);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String render(final ConfluenceMonitoringReport model) {
        try {
            Template template = freemarkerConfig.getTemplate("monitoring_data_report.ftl");
            StringWriter writer = new StringWriter();
            template.process(Map.of("model", model), writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render monitoring data page template", e);
        }
    }

    private ConfluenceMonitoringReport buildModel(final List<KwMonitoringDocument> data,
                                                   final ZoneId targetTimezone,
                                                   final Locale locale) {
        var now = Instant.now().atZone(targetTimezone);
        String generatedAt  = now.format(GENERATED_AT_FMT);
        String reportDate     = now.format(REPORT_DATE_FMT);
        String reportDateLong = now.format(REPORT_DATE_LONG_FMT);

        Map<String, List<KwMonitoringDocument>> byClient = new TreeMap<>();
        for (KwMonitoringDocument item : data) {
            byClient.computeIfAbsent(extractClientName(item), k -> new ArrayList<>()).add(item);
        }

        List<ClientGroup> clientGroups = new ArrayList<>();
        List<ReportEntry> allReports   = new ArrayList<>();
        for (Map.Entry<String, List<KwMonitoringDocument>> entry : byClient.entrySet()) {
            List<ReportEntry> groupReports = entry.getValue().stream()
                    .map(item -> toReportEntry(item, targetTimezone, locale)).toList();
            allReports.addAll(groupReports);
            clientGroups.add(new ClientGroup(
                    entry.getKey(), groupReports.size(),
                    buildPrioritySummary(groupReports), groupReports));
        }

        List<PrioritySummaryEntry> prioritySummary = buildPrioritySummary(allReports);
        Map<String, String> englishLabels = buildFullEnglishLabelMap(data);

        // Default single section (English) — used by the buildPageContent path.
        List<ConfluenceMonitoringReport.LanguageSection> sections = List.of(
                new ConfluenceMonitoringReport.LanguageSection(
                        "en", resolveLanguageDisplayName("en"),
                        prioritySummary, clientGroups, englishLabels));

        return new ConfluenceMonitoringReport(
                reportDate, reportDateLong, generatedAt,
                data.size(), byClient.size(), prioritySummary, clientGroups, sections);
    }

    // ── Label helpers ────────────────────────────────────────────────────────

    /**
     * Builds the full English label map for a document set.
     *
     * <p>Starts from {@link #ENGLISH_UI_LABELS} then appends any additional
     * {@code dynamicData} keys found in the documents (e.g. {@code "status"},
     * {@code "region"}) so that dynamic field headings are also translatable without
     * any code change.</p>
     */
    private Map<String, String> buildFullEnglishLabelMap(final List<KwMonitoringDocument> docs) {
        Map<String, String> full = new LinkedHashMap<>(ENGLISH_UI_LABELS);
        for (String key : collectDynamicFieldLabelKeys(docs)) {
            // Capitalise first letter for the English display value.
            String display = key.isEmpty() ? key
                    : Character.toUpperCase(key.charAt(0)) + key.substring(1);
            full.putIfAbsent(key, display);
        }
        return full;
    }

    /**
     * Collects lowercase-normalised {@code dynamicData} key names from the given
     * documents, excluding the structural fields {@code "client"} and
     * {@code "priority"} which are never shown as standalone label rows.
     */
    private Set<String> collectDynamicFieldLabelKeys(final List<KwMonitoringDocument> docs) {
        Set<String> keys = new LinkedHashSet<>();
        for (KwMonitoringDocument doc : docs) {
            if (doc.getAttributes() == null) {
                continue;
            }
            Object raw = doc.getAttributes().get("dynamicData");
            if (!(raw instanceof Map<?, ?> dynMap)) {
                continue;
            }
            for (Object k : dynMap.keySet()) {
                if (k instanceof String key) {
                    String lower = key.toLowerCase(Locale.ENGLISH);
                    if (!"client".equals(lower) && !"priority".equals(lower)) {
                        keys.add(lower);
                    }
                }
            }
        }
        return keys;
    }

    // ── Document → model ────────────────────────────────────────────────────

    private String extractClientName(final KwMonitoringDocument item) {
        Map<String, Object> attributes = item.getAttributes() != null ? item.getAttributes() : Map.of();
        Map<?, ?> dynamicData = attributes.get("dynamicData") instanceof Map<?, ?> m ? m : Map.of();
        String client = resolveStringValue(dynamicData.get("Client"));
        return (client != null && !client.isBlank()) ? client : "Unknown";
    }

    private ReportEntry toReportEntry(final KwMonitoringDocument item,
                                      final ZoneId timezone,
                                      final Locale locale) {
        Map<String, Object> attributes = item.getAttributes() != null ? item.getAttributes() : Map.of();
        Map<?, ?> dynamicData = attributes.get("dynamicData") instanceof Map<?, ?> m ? m : Map.of();

        String client   = resolveStringValue(dynamicData.get("Client"));
        String priority = resolveStringValue(dynamicData.get("Priority"));
        if (priority == null || priority.isBlank()) {
            priority = "Info";
        }
        String normalizedPriority = priority.toUpperCase(Locale.ENGLISH);

        return new ReportEntry(
                item.getTitle() != null ? item.getTitle() : "",
                client != null ? client : "Unknown",
                normalizedPriority,
                resolveLevelColour(normalizedPriority),
                resolveLevelBgColour(normalizedPriority),
                resolveLevelBorderColour(normalizedPriority),
                resolveDot(normalizedPriority),
                resolveBodyText(item.getBody(), dynamicData),
                formatTimestamp(item.getCreatedTimestamp(), timezone, locale),
                formatTimestamp(item.getUpdatedTimestamp(), timezone, locale),
                buildDynamicFields(dynamicData),
                extractAuthors(attributes),
                extractSerials(attributes),
                extractTags(attributes)
        );
    }

    /**
     * Returns the best available body/summary text for a report entry.
     *
     * <p>The document {@code body} takes priority.  When it is blank, the method
     * falls back to the {@code dynamicData["Summary"]} field (case-insensitive lookup)
     * so that records that store summary text only in {@code dynamicData} still render
     * correctly in the Summary row, without creating a duplicate dynamic-field row.</p>
     */
    private String resolveBodyText(final String body, final Map<?, ?> dynamicData) {
        if (body != null && !body.isBlank()) {
            return body;
        }
        Object rawSummary = getDynamicValue(dynamicData, "summary");
        String summaryFallback = resolveStringValue(rawSummary);
        return (summaryFallback != null && !summaryFallback.isBlank()) ? summaryFallback : "";
    }

    private List<DynamicField> buildDynamicFields(final Map<?, ?> dynamicData) {
        List<DynamicField> fields = new ArrayList<>();
        for (Map.Entry<?, ?> entry : dynamicData.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            // Exclude structural fields that are rendered in dedicated rows.
            String keyLower = key.toLowerCase(Locale.ENGLISH);
            if ("client".equals(keyLower) || "priority".equals(keyLower)
                    || "summary".equals(keyLower)) {
                continue;
            }
            String value = resolveStringValue(entry.getValue());
            if (value != null) {
                fields.add(new DynamicField(key, value));
            }
        }
        return fields;
    }

    /**
     * Case-insensitive lookup of a dynamicData entry by its lowercase key name.
     * Returns the raw value (may be a String or Map) or {@code null} if not found.
     */
    private Object getDynamicValue(final Map<?, ?> map, final String lowerKey) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() instanceof String k
                    && k.toLowerCase(Locale.ENGLISH).equals(lowerKey)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String resolveStringValue(final Object raw) {
        switch (raw) {
            case null -> {
                return null;
            }
            case String s -> {
                return s;
            }
            case Map<?, ?> map -> {
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

    private List<String> extractAuthors(final Map<String, Object> attributes) {
        Object raw = attributes.get("authors");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(o -> o instanceof Map<?, ?>)
                    .map(o -> (Map<?, ?>) o)
                    .map(m -> {
                        Object v = m.get("displayFullName");
                        return v != null ? String.valueOf(v) : "";
                    })
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    private List<String> extractSerials(final Map<String, Object> attributes) {
        Object raw = attributes.get("serials");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(o -> o instanceof Map<?, ?>)
                    .map(o -> (Map<?, ?>) o)
                    .map(m -> {
                        Object v = m.get("filingNumberDisplay");
                        return v != null ? String.valueOf(v) : "";
                    })
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    private List<String> extractTags(final Map<String, Object> attributes) {
        Object raw = attributes.get("tags");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private String formatTimestamp(final long epochSeconds, final ZoneId timezone, final Locale locale) {
        if (epochSeconds <= 0) {
            return "";
        }
        return Instant.ofEpochSecond(epochSeconds).atZone(timezone)
                .format(TIMESTAMP_FMT.withLocale(locale));
    }

    private List<PrioritySummaryEntry> buildPrioritySummary(final List<ReportEntry> reports) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String level : PRIORITY_ORDER) {
            counts.put(level, 0);
        }
        for (ReportEntry r : reports) {
            counts.merge(r.priority().toUpperCase(Locale.ENGLISH), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new PrioritySummaryEntry(e.getKey(), resolveLevelColour(e.getKey()), e.getValue()))
                .toList();
    }

    // ── Colour / style helpers ───────────────────────────────────────────────

    private String resolveLevelColour(final String level) {
        return switch (level) {
            case "CRITICAL", "HIGH" -> "Red";
            case "MEDIUM" -> "Yellow";
            case "LOW"    -> "Green";
            case "INFO"   -> "Blue";
            default       -> "Grey";
        };
    }

    private String resolveDot(final String level) {
        return switch (level) {
            case "CRITICAL", "HIGH" -> "\uD83D\uDD34";
            case "MEDIUM" -> "\uD83D\uDFE1";
            case "LOW"    -> "\uD83D\uDFE2";
            case "INFO"   -> "\uD83D\uDD35";
            default       -> "⚪";
        };
    }

    private String resolveLevelBgColour(final String level) {
        return switch (level) {
            case "CRITICAL", "HIGH" -> "#FFEBE6";
            case "MEDIUM" -> "#FFF4E5";
            case "LOW"    -> "#E3FCEF";
            case "INFO"   -> "#E6F3FF";
            default       -> "#F4F5F7";
        };
    }

    private String resolveLevelBorderColour(final String level) {
        return switch (level) {
            case "CRITICAL" -> "#BF2600";
            case "HIGH"     -> "#DE350B";
            case "MEDIUM"   -> "#FF8B00";
            case "LOW"      -> "#006644";
            case "INFO"     -> "#0052CC";
            default         -> "#97A0AF";
        };
    }

    private String resolveLanguageDisplayName(final String langCode) {
        return switch (langCode.toLowerCase()) {
            case "en"            -> "📋 Monitoring Reports — English";
            case "ja", "jp"      -> "📋 監視レポート — 日本語";
            case "de"            -> "📋 Überwachungsberichte — Deutsch";
            case "fr"            -> "📋 Rapports de surveillance — Français";
            case "es"            -> "📋 Informes de monitoreo — Español";
            case "ru"            -> "📋 Отчёты мониторинга — Русский";
            case "ar"            -> "📋 تقارير المراقبة — العربية";
            case "zh", "zh-cn"   -> "📋 监控报告 — 中文（简体）";
            case "zh-tw"         -> "📋 監控報告 — 中文（繁體）";
            case "ko"            -> "📋 모니터링 보고서 — 한국어";
            case "hi"            -> "📋 निगरानी रिपोर्ट — हिन्दी";
            default              -> "📋 Monitoring Reports — " + langCode.toUpperCase();
        };
    }

    /**
     * Maps a BCP-47 language code to a {@link Locale} used for formatting timestamps
     * (month names in {@link #TIMESTAMP_FMT}).
     * Falls back to {@link Locale#ENGLISH} for unrecognised codes.
     */
    private Locale localeFromLangCode(final String langCode) {
        return switch (langCode.toLowerCase(Locale.ENGLISH)) {
            case "ja", "jp" -> Locale.JAPANESE;
            case "de"       -> Locale.GERMAN;
            case "fr"       -> Locale.FRENCH;
            case "zh", "zh-cn" -> Locale.SIMPLIFIED_CHINESE;
            case "zh-tw"    -> Locale.TRADITIONAL_CHINESE;
            case "ko"       -> Locale.KOREAN;
            default         -> Locale.forLanguageTag(langCode);
        };
    }
}
