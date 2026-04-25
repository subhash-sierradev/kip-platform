package com.integration.execution.constants;

/**
 * Kaseware-specific constants for Integration Execution Service.
 * Includes cache keys, document types, and Kaseware integration identifiers.
 */
public final class KasewareConstants {

    // Kaseware GraphQL Token Cache
    public static final String KW_GRAPHQL_TOKEN_CACHE_KEY = "kw_graphql_token";

    // Document Subtype Prefixes
    public static final String DOCUMENT_DRAFT_PREFIX = "DOCUMENT_DRAFT_";

    public static final String DOCUMENT_FINAL_DYNAMIC = "DOCUMENT_FINAL_DYNAMIC";

    // Monitoring Document Field Keys
    /** JSON field name for the dynamic-data object in a Kaseware monitoring document. */
    public static final String DYNAMIC_DATA_FIELD = "dynamicData";

    /**
     * Key inside the {@code dynamicData} object used by Kaseware to store internal
     * hidden-field markers. This key is always excluded from resolved attribute maps.
     */
    public static final String HIDDEN_FIELDS_KEY = "hiddenFields";

    // Monitoring Document — Stable (typed) top-level field names
    // These are already mapped to KwMonitoringDocument typed properties and are
    // skipped during generic attribute extraction to avoid duplication.
    public static final String MONITORING_FIELD_ID = "id";
    public static final String MONITORING_FIELD_TITLE = "title";
    public static final String MONITORING_FIELD_BODY = "body";
    public static final String MONITORING_FIELD_CREATED_TIMESTAMP = "createdTimestamp";
    public static final String MONITORING_FIELD_UPDATED_TIMESTAMP = "updatedTimestamp";
    public static final String MONITORING_FIELD_DYNAMIC_FORM_DEFINITION_ID = "dynamicFormDefinitionId";
    public static final String MONITORING_FIELD_DYNAMIC_FORM_DEFINITION_NAME = "dynamicFormDefinitionName";
    public static final String MONITORING_FIELD_DYNAMIC_FORM_VERSION_NUMBER = "dynamicFormVersionNumber";
    public static final String MONITORING_FIELD_TENANT_ID = "tenantId";

    // Monitoring Document — Array fields that are excluded when the array is empty
    public static final String MONITORING_FIELD_AUTHORS = "authors";
    public static final String MONITORING_FIELD_APPROVERS = "approvers";
    public static final String MONITORING_FIELD_SERIALS = "serials";
    public static final String MONITORING_FIELD_CASE_LABELS = "caseLabels";
    public static final String MONITORING_FIELD_RELATED_ENTITIES = "relatedEntities";

    private KasewareConstants() {
        throw new IllegalStateException("KasewareConstants is a utility class and cannot be instantiated");
    }
}
