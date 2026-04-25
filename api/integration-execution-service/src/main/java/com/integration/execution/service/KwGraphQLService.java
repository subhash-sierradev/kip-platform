package com.integration.execution.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.KwGraphqlClient;
import com.integration.execution.config.properties.MonitoringDocumentConfig;
import com.integration.execution.constants.KasewareConstants;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.contract.model.KwLocationDto;
import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.execution.exception.IntegrationApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.integration.execution.constants.KasewareConstants.DYNAMIC_DATA_FIELD;

@Slf4j
@Service
@RequiredArgsConstructor
public class KwGraphQLService {

    private static final Map<Class<?>, String> FIELD_TYPE_NAMES;

    static {
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(String.class, "String");
        map.put(Long.class, "Long");
        map.put(long.class, "Long");
        map.put(Integer.class, "Integer");
        map.put(int.class, "Integer");
        map.put(Double.class, "Double");
        map.put(double.class, "Double");
        map.put(Boolean.class, "Boolean");
        map.put(boolean.class, "Boolean");
        FIELD_TYPE_NAMES = Collections.unmodifiableMap(map);
    }

    // --- Cache region names --------------------------------------------------
    private static final String CACHE_ITEM_SUBTYPES      = "kwItemSubtypesCache";
    private static final String CACHE_FIELD_MAPPING      = "kwFieldMappingCache";
    private static final String CACHE_DYNAMIC_DOC_TYPES  = "kwDynamicDocTypeCache";

    // --- Reusable TypeReference for list-typed attribute fields --------------
    private static final TypeReference<List<Object>> LIST_OF_OBJECTS_REF = new TypeReference<>() { };

    // --- Attribute map key written by this service ---------------------------
    private static final String FORM_FIELD_LABELS_KEY = "formFieldLabels";

    // --- Kaseware document JSON field names ----------------------------------
    private static final String KW_FIELD_DYNAMIC_FORM_DEF_ID    = "dynamicFormDefinitionId";
    private static final String KW_FIELD_DYNAMIC_FORM_DEF_NAME  = "dynamicFormDefinitionName";
    private static final String KW_FIELD_DYNAMIC_FORM_VERSION   = "dynamicFormVersionNumber";
    private static final String KW_FIELD_TENANT_ID              = "tenantId";

    // --- Kaseware form-definition JSON field names ---------------------------
    private static final String KW_FIELD_VERSIONS            = "versions";
    private static final String KW_FIELD_FORM_FIELDS         = "formFields";
    private static final String KW_FIELD_VERSION_STATUS      = "status";
    private static final String KW_FIELD_FORM_VERSION_ACTIVE = "ACTIVE";
    private static final String KW_FIELD_NAME                = "name";
    private static final String KW_FIELD_LABEL               = "label";
    private static final String KW_FIELD_VALUES              = "values";
    private static final String KW_FIELD_OPTION_VALUE        = "value";
    private static final String KW_FIELD_ID                  = "id";
    private static final String KW_FIELD_BASED_ON_DOC_TYPE   = "basedOnDocumentType";
    private static final String KW_FIELD_FORM_NAME           = "formName";

    // --- Kaseware lookup item field names ------------------------------------
    private static final String KW_FIELD_PARENT_CODE       = "parentCode";
    private static final String KW_FIELD_CODE              = "code";
    private static final String KW_FIELD_DISPLAY_VALUE     = "displayValue";
    private static final String KW_LOOKUP_PARENT_DOCUMENT  = "DOCUMENT";

    // --- GraphQL response structural field names -----------------------------
    private static final String GRAPHQL_DATA                 = "data";
    private static final String GRAPHQL_ERRORS               = "errors";
    private static final String GRAPHQL_LOOKUPS              = "lookups";
    private static final String GRAPHQL_ALL_FORM_DEFINITIONS = "allDynamicFormDefinitions";

    private final KwGraphqlClient kwGraphqlClient;
    private final ObjectMapper objectMapper;
    private final MonitoringDocumentConfig monitoringDocumentConfig;

    @Retryable(retryFor = RuntimeException.class, backoff = @Backoff(delay = 500, multiplier = 2.0))
    @Cacheable(value = CACHE_ITEM_SUBTYPES)
    public List<KwItemSubtypeDto> fetchItemSubtypes() {
        log.info("Fetching item subtypes from KW");

        String gql = """
                query {
                  lookups(
                    lookupType: "OBJECT_SUB_TYPES",
                    languageCode: "en-US"
                  ) {
                    code
                    displayValue
                    parentCode
                  }
                }
                """;

        Map<String, Object> payload = Map.of("query", gql);
        String responseBody = kwGraphqlClient.executeGraphQLQuery(payload);
        List<KwItemSubtypeDto> result = parseLookupResponse(responseBody);
        return result != null ? result : List.of();
    }

    @Cacheable(value = CACHE_FIELD_MAPPING)
    public List<KwDocField> fetchFieldMappingForLocations() {
        Field[] fields = KwLocationDto.class.getDeclaredFields();
        AtomicInteger index = new AtomicInteger(0);
        return Arrays.stream(fields)
                .map(field -> KwDocField.builder()
                        .id(index.getAndIncrement())
                        .fieldName(field.getName())
                        .fieldType(resolveFieldType(field.getType()))
                        .build())
                .toList();
    }

    @Retryable(retryFor = RuntimeException.class, backoff = @Backoff(delay = 500, multiplier = 2.0))
    @Cacheable(value = CACHE_DYNAMIC_DOC_TYPES, key = "'v3|' + #type")
    @SuppressWarnings("unused")
    public List<KwDynamicDocType> fetchDynamicDocumentsTypes(String type, String subType) {
        String query = """
                query DynamicFormDefinitions($versionsToInclude: Int) {
                    allDynamicFormDefinitions(versionsToInclude: $versionsToInclude) {
                        id
                        formName
                        basedOnDocumentType
                    }
                }
                """;
        Map<String, Object> payload = Map.of(
                "query", query,
                "variables", Map.of("versionsToInclude", 1));
        String responseBody = kwGraphqlClient.executeGraphQLQuery(payload);
        List<KwDynamicDocType> result = parseDynamicFormDefinitionsResponse(responseBody, type);
        return result != null ? result : List.of();
    }

    public List<KwMonitoringDocument> fetchMonitoringData(String dynamicFormDefinitionId,
                                                          Integer startTimestamp,
                                                          Integer endTimestamp,
                                                          int start,
                                                          int limit) {
        // 1. Fetch raw monitoring documents
        JsonNode documents = kwGraphqlClient.fetchMonitoringDocuments(
                dynamicFormDefinitionId, startTimestamp, endTimestamp, start, limit);

        if (documents == null || !documents.isArray() || documents.isEmpty()) {
            return List.of();
        }

        // 2. Collect unique dynamicFormDefinitionIds and fetch their form definitions
        Map<String, JsonNode> formDefCache = new HashMap<>();
        for (JsonNode doc : documents) {
            String formDefId = doc.path(KW_FIELD_DYNAMIC_FORM_DEF_ID).asText(null);
            if (formDefId != null && !formDefCache.containsKey(formDefId)) {
                try {
                    JsonNode formDef = kwGraphqlClient.fetchFormDefinition(formDefId);
                    formDefCache.put(formDefId, formDef);
                } catch (Exception e) {
                    log.warn("Failed to fetch form definition for {}: {}", formDefId, e.getMessage());
                }
            }
        }

        // 3. Build field-label lookup per form definition: fieldName → label
        Map<String, Map<String, String>> formFieldLabels = new HashMap<>();
        Map<String, Map<String, Map<String, String>>> formValueLabels = new HashMap<>();
        for (var entry : formDefCache.entrySet()) {
            Map<String, String> fieldLabels = new LinkedHashMap<>();
            Map<String, Map<String, String>> valueLabels = new LinkedHashMap<>();
            buildFieldLabels(entry.getValue(), fieldLabels, valueLabels);
            formFieldLabels.put(entry.getKey(), fieldLabels);
            formValueLabels.put(entry.getKey(), valueLabels);
        }

        // 4. Convert each document to KwMonitoringDocument
        List<KwMonitoringDocument> result = new ArrayList<>();
        for (JsonNode doc : documents) {
            result.add(toKwMonitoringDocument(doc, formFieldLabels, formValueLabels));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Document conversion
    // -----------------------------------------------------------------------

    private KwMonitoringDocument toKwMonitoringDocument(
            JsonNode doc,
            Map<String, Map<String, String>> formFieldLabels,
            Map<String, Map<String, Map<String, String>>> formValueLabels) {

        String formDefId = doc.path(KW_FIELD_DYNAMIC_FORM_DEF_ID).asText(null);
        Map<String, String> fieldLabels = formFieldLabels.getOrDefault(formDefId, Map.of());
        Map<String, Map<String, String>> valueLabelMap = formValueLabels.getOrDefault(formDefId, Map.of());
        Map<String, Object> attributes = buildAttributes(doc, fieldLabels, valueLabelMap);

        return buildKwMonitoringDocument(doc, formDefId, attributes);
    }

    /**
     * Builds the attributes map for a document node.
     *
     * <p>Iterates every top-level field of the document JSON node. Fields already captured
     * as typed properties on {@link KwMonitoringDocument} (configured via
     * {@link MonitoringDocumentConfig#getStableFields()}) are skipped. The special
     * {@code dynamicData} object is resolved with form-definition label and value mappings.
     * All remaining fields — including any client-specific custom fields — are converted
     * generically so that no data is silently dropped.
     */
    private Map<String, Object> buildAttributes(
            JsonNode doc,
            Map<String, String> fieldLabels,
            Map<String, Map<String, String>> valueLabelMap) {

        Map<String, Object> attributes = new LinkedHashMap<>();

        // Handle dynamicData first with label + value resolution
        putDynamicDataAttribute(attributes, doc.path(DYNAMIC_DATA_FIELD), fieldLabels, valueLabelMap);

        // Generically capture every remaining top-level field not already on the model
        for (Map.Entry<String, JsonNode> entry : doc.properties()) {
            String key = entry.getKey();
            if (monitoringDocumentConfig.getStableFields().contains(key)
                    || DYNAMIC_DATA_FIELD.equals(key)) {
                continue;
            }
            putDocumentField(attributes, key, entry.getValue());
        }

        // Expose form-field label metadata so consumers can render human-readable keys
        attributes.put(FORM_FIELD_LABELS_KEY, fieldLabels);
        return attributes;
    }

    /**
     * Converts a single document-level JSON node into the attributes map entry.
     *
     * <p>Array nodes are guarded by {@link MonitoringDocumentConfig#getRequireNonEmptyArrayFields()}:
     * if the field name is in that set and the array is empty, the entry is omitted.
     * All other non-null, non-missing nodes are converted to their natural Java type via
     * Jackson, preserving numbers, booleans, nested objects, and arrays as-is.
     */
    private void putDocumentField(Map<String, Object> attributes, String key, JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            boolean requireNonEmpty = monitoringDocumentConfig.getRequireNonEmptyArrayFields().contains(key);
            if (requireNonEmpty && node.isEmpty()) {
                return;
            }
            attributes.put(key, objectMapper.convertValue(node, LIST_OF_OBJECTS_REF));
            return;
        }
        attributes.put(key, objectMapper.convertValue(node, Object.class));
    }

    private KwMonitoringDocument buildKwMonitoringDocument(
            JsonNode doc,
            String formDefId,
            Map<String, Object> attributes) {
        return KwMonitoringDocument.builder()
                .id(doc.path(KW_FIELD_ID).asText(null))
                .title(doc.path("title").asText(null))
                .body(doc.path("body").asText(null))
                .createdTimestamp(doc.path("createdTimestamp").asLong(0L))
                .updatedTimestamp(doc.path("updatedTimestamp").asLong(0L))
                .dynamicFormDefinitionId(formDefId)
                .dynamicFormDefinitionName(doc.path(KW_FIELD_DYNAMIC_FORM_DEF_NAME).asText(null))
                .dynamicFormVersionNumber(doc.path(KW_FIELD_DYNAMIC_FORM_VERSION).asInt(0))
                .tenantId(doc.path(KW_FIELD_TENANT_ID).asText(null))
                .attributes(attributes)
                .build();
    }

    // -----------------------------------------------------------------------
    // Dynamic-data attribute resolution
    // -----------------------------------------------------------------------

    private void putDynamicDataAttribute(
            Map<String, Object> attributes,
            JsonNode dynamicData,
            Map<String, String> fieldLabels,
            Map<String, Map<String, String>> valueLabelMap) {
        if (!dynamicData.isObject()) {
            return;
        }

        Map<String, Object> resolvedDynamic = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> field : dynamicData.properties()) {
            if (monitoringDocumentConfig.getSkippedDynamicKeys().contains(field.getKey())) {
                continue;
            }
            String label = fieldLabels.getOrDefault(field.getKey(), field.getKey());
            Object value = resolveFieldValue(field.getKey(), field.getValue(), fieldLabels, valueLabelMap);
            resolvedDynamic.put(label, value);
        }
        attributes.put(DYNAMIC_DATA_FIELD, resolvedDynamic);
    }

    private Object resolveFieldValue(String fieldName, JsonNode value,
                                     Map<String, String> fieldLabels,
                                     Map<String, Map<String, String>> valueLabelMap) {
        if (value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            Map<String, String> options = valueLabelMap.get(fieldName);
            if (options != null && options.containsKey(value.asText())) {
                return options.get(value.asText());
            }
            return value.asText();
        }
        if (value.isObject()) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> f : value.properties()) {
                String innerLabel = fieldLabels.getOrDefault(f.getKey(), f.getKey());
                Map<String, String> options = valueLabelMap.get(f.getKey());
                if (f.getValue().isTextual()) {
                    if (options != null && options.containsKey(f.getValue().asText())) {
                        resolved.put(innerLabel, options.get(f.getValue().asText()));
                    } else {
                        resolved.put(innerLabel, f.getValue().asText());
                    }
                } else if (f.getValue().isArray()) {
                    List<Object> resolvedArray = new ArrayList<>();
                    for (JsonNode element : f.getValue()) {
                        if (element.isTextual() && options != null
                                && options.containsKey(element.asText())) {
                            resolvedArray.add(options.get(element.asText()));
                        } else {
                            resolvedArray.add(objectMapper.convertValue(element, Object.class));
                        }
                    }
                    resolved.put(innerLabel, resolvedArray);
                } else {
                    resolved.put(innerLabel, objectMapper.convertValue(f.getValue(), Object.class));
                }
            }
            return resolved;
        }
        return objectMapper.convertValue(value, Object.class);
    }

    // -----------------------------------------------------------------------
    // Form-definition label building
    // -----------------------------------------------------------------------

    private void buildFieldLabels(JsonNode formDef,
                                  Map<String, String> fieldLabels,
                                  Map<String, Map<String, String>> valueLabels) {
        if (formDef == null || formDef.isMissingNode()) {
            return;
        }

        JsonNode targetVersion = selectTargetVersion(formDef.path(KW_FIELD_VERSIONS));
        if (targetVersion == null) {
            return;
        }

        JsonNode formFields = targetVersion.path(KW_FIELD_FORM_FIELDS);
        if (!formFields.isArray()) {
            return;
        }

        for (JsonNode field : formFields) {
            mapFieldLabelAndOptions(field, fieldLabels, valueLabels);
        }
    }

    private JsonNode selectTargetVersion(JsonNode versions) {
        if (!versions.isArray()) {
            return null;
        }
        for (JsonNode version : versions) {
            if (KW_FIELD_FORM_VERSION_ACTIVE.equals(version.path(KW_FIELD_VERSION_STATUS).asText(null))) {
                return version;
            }
        }
        if (versions.isEmpty()) {
            return null;
        }
        return versions.get(0);
    }

    private void mapFieldLabelAndOptions(
            JsonNode field,
            Map<String, String> fieldLabels,
            Map<String, Map<String, String>> valueLabels) {
        String name = field.path(KW_FIELD_NAME).asText(null);
        String label = field.path(KW_FIELD_LABEL).asText(null);

        if (name != null && label != null) {
            fieldLabels.put(name, label);
        }

        if (name == null) {
            return;
        }

        Map<String, String> optionMap = buildOptionMap(field.path(KW_FIELD_VALUES));
        if (!optionMap.isEmpty()) {
            valueLabels.put(name, optionMap);
        }
    }

    private Map<String, String> buildOptionMap(JsonNode values) {
        if (!values.isArray() || values.isEmpty()) {
            return Map.of();
        }

        Map<String, String> optionMap = new LinkedHashMap<>();
        for (JsonNode option : values) {
            String optionValue = option.path(KW_FIELD_OPTION_VALUE).asText(null);
            String optionLabel = option.path(KW_FIELD_LABEL).asText(null);
            if (optionValue != null && optionLabel != null) {
                optionMap.put(optionValue, optionLabel);
            }
        }
        return optionMap;
    }

    // -----------------------------------------------------------------------
    // GraphQL response parsers
    // -----------------------------------------------------------------------

    private void validateGraphQLResponse(JsonNode root) {
        JsonNode errors = root.path(GRAPHQL_ERRORS);
        if (errors.isArray() && !errors.isEmpty()) {
            throw new IntegrationApiException("KW GraphQL returned errors: " + errors, 502);
        }
    }

    private List<KwItemSubtypeDto> parseLookupResponse(String responseBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }

        validateGraphQLResponse(root);

        JsonNode lookups = root.path(GRAPHQL_DATA).path(GRAPHQL_LOOKUPS);
        if (!lookups.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(lookups.spliterator(), false)
                .filter(this::isDocumentLookup)
                .filter(node -> !node.path(KW_FIELD_CODE).asText()
                        .startsWith(KasewareConstants.DOCUMENT_DRAFT_PREFIX))
                .map(node -> new KwItemSubtypeDto(
                        node.path(KW_FIELD_CODE).asText(),
                        node.path(KW_FIELD_DISPLAY_VALUE).asText()))
                .sorted(Comparator.comparing(KwItemSubtypeDto::displayValue))
                .toList();
    }

    private boolean isDocumentLookup(JsonNode node) {
        return KW_LOOKUP_PARENT_DOCUMENT.equals(node.path(KW_FIELD_PARENT_CODE).asText(null))
                && node.has(KW_FIELD_CODE)
                && node.has(KW_FIELD_DISPLAY_VALUE);
    }

    private String resolveFieldType(final Class<?> type) {
        return FIELD_TYPE_NAMES.getOrDefault(type, type.getSimpleName());
    }

    private List<KwDynamicDocType> parseDynamicFormDefinitionsResponse(
            final String responseBody,
            final String docType) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IntegrationApiException("Empty response from upstream", 502);
        }
        JsonNode definitionsNode = extractDynamicFormDefinitionsNode(responseBody);
        if (definitionsNode == null || !definitionsNode.isArray()) {
            return List.of();
        }
        List<KwDynamicDocType> filtered = mapDynamicFormDefinitionNodes(definitionsNode, docType);
        if (!filtered.isEmpty()) {
            return filtered;
        }
        return mapDynamicFormDefinitionNodes(definitionsNode, null);
    }

    private JsonNode extractDynamicFormDefinitionsNode(final String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IntegrationApiException("Failed to parse upstream response: " + e.getMessage(), 502, e);
        }

        validateGraphQLResponse(root);

        JsonNode data = root.path(GRAPHQL_DATA);
        if (data == null || data.isMissingNode() || data.isNull()) {
            return null;
        }
        return data.path(GRAPHQL_ALL_FORM_DEFINITIONS);
    }

    private List<KwDynamicDocType> mapDynamicFormDefinitionNodes(
            final JsonNode definitionsNode,
            final String docType) {
        return StreamSupport.stream(definitionsNode.spliterator(), false)
                .map(definitionNode -> mapDynamicFormDefinitionNode(definitionNode, docType))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        KwDynamicDocType::title,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    private KwDynamicDocType mapDynamicFormDefinitionNode(
            final JsonNode definition,
            final String docType) {
        if (definition == null || definition.isNull() || definition.isMissingNode()) {
            return null;
        }
        String id = definition.path(KW_FIELD_ID).asText(null);
        if (id == null || id.isBlank()) {
            return null;
        }
        String definitionType = definition.path(KW_FIELD_BASED_ON_DOC_TYPE).asText(null);
        if (docType != null
                && !docType.isBlank()
                && !docType.equalsIgnoreCase(definitionType)) {
            return null;
        }

        String title = definition.path(KW_FIELD_FORM_NAME).asText(null);
        return new KwDynamicDocType(id, title, List.of());
    }
}
