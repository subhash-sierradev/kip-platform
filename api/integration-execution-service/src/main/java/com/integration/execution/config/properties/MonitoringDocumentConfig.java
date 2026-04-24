package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.integration.execution.constants.KasewareConstants.HIDDEN_FIELDS_KEY;

/**
 * Configuration for Kaseware monitoring document attribute extraction.
 *
 * <p>Controls which top-level document fields are already captured as typed properties
 * on {@link com.integration.execution.model.KwMonitoringDocument} (stable fields), which
 * dynamic-data keys to skip, and which array fields must be non-empty to be included in
 * the attributes map.
 *
 * <p>Override any of these sets in {@code application.yml} under
 * {@code integration.monitoring.document} to accommodate client-specific schemas without
 * code changes.
 */
@Data
@ConfigurationProperties(prefix = "integration.monitoring.document")
public class MonitoringDocumentConfig {

    /**
     * Top-level document fields already mapped to {@code KwMonitoringDocument} typed
     * properties. These are skipped during generic attribute extraction to avoid duplication.
     */
    private Set<String> stableFields = new LinkedHashSet<>(Set.of(
            "id",
            "title",
            "body",
            "createdTimestamp",
            "updatedTimestamp",
            "dynamicFormDefinitionId",
            "dynamicFormDefinitionName",
            "dynamicFormVersionNumber",
            "tenantId"
    ));

    /**
     * Keys inside the {@code dynamicData} object that are always excluded from the
     * resolved attribute map (e.g. internal Kaseware hidden-fields markers).
     */
    private Set<String> skippedDynamicKeys = new LinkedHashSet<>(Set.of(HIDDEN_FIELDS_KEY));

    /**
     * Array field names for which an empty array is treated as absent and excluded from
     * attributes. Useful for people/case sub-objects that are only meaningful when populated.
     */
    private Set<String> requireNonEmptyArrayFields = new LinkedHashSet<>(Set.of(
            "authors",
            "approvers",
            "serials",
            "caseLabels",
            "relatedEntities"
    ));
}

