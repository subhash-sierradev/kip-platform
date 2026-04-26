package com.integration.execution.config.properties;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.integration.execution.constants.KasewareConstants.HIDDEN_FIELDS_KEY;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_APPROVERS;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_AUTHORS;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_BODY;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_CASE_LABELS;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_CREATED_TIMESTAMP;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_DYNAMIC_FORM_DEFINITION_ID;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_DYNAMIC_FORM_DEFINITION_NAME;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_DYNAMIC_FORM_VERSION_NUMBER;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_ID;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_RELATED_ENTITIES;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_SERIALS;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_TENANT_ID;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_TITLE;
import static com.integration.execution.constants.KasewareConstants.MONITORING_FIELD_UPDATED_TIMESTAMP;

/**
 * Configuration for Kaseware monitoring document attribute extraction.
 *
 * <p>Controls which top-level document fields are already captured as typed properties
 * on {@link com.integration.execution.model.KwMonitoringDocument} (stable fields), which
 * dynamic-data keys to skip, and which array fields must be non-empty to be included in
 * the attributes map.
 */
@Getter
@Component
public class MonitoringDocumentConfig {

    /**
     * Top-level document fields already mapped to {@code KwMonitoringDocument} typed
     * properties. These are skipped during generic attribute extraction to avoid duplication.
     */
    private final Set<String> stableFields = new LinkedHashSet<>(Set.of(
            MONITORING_FIELD_ID,
            MONITORING_FIELD_TITLE,
            MONITORING_FIELD_BODY,
            MONITORING_FIELD_CREATED_TIMESTAMP,
            MONITORING_FIELD_UPDATED_TIMESTAMP,
            MONITORING_FIELD_DYNAMIC_FORM_DEFINITION_ID,
            MONITORING_FIELD_DYNAMIC_FORM_DEFINITION_NAME,
            MONITORING_FIELD_DYNAMIC_FORM_VERSION_NUMBER,
            MONITORING_FIELD_TENANT_ID
    ));

    /**
     * Keys inside the {@code dynamicData} object that are always excluded from the
     * resolved attribute map (e.g. internal Kaseware hidden-fields markers).
     */
    private final Set<String> skippedDynamicKeys = new LinkedHashSet<>(Set.of(HIDDEN_FIELDS_KEY));

    /**
     * Array field names for which an empty array is treated as absent and excluded from
     * attributes. Useful for people/case sub-objects that are only meaningful when populated.
     */
    private final Set<String> requireNonEmptyArrayFields = new LinkedHashSet<>(Set.of(
            MONITORING_FIELD_AUTHORS,
            MONITORING_FIELD_APPROVERS,
            MONITORING_FIELD_SERIALS,
            MONITORING_FIELD_CASE_LABELS,
            MONITORING_FIELD_RELATED_ENTITIES
    ));
}
