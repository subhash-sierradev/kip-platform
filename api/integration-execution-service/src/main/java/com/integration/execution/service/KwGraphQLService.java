package com.integration.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.KwGraphqlClient;
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

    private final KwGraphqlClient kwGraphqlClient;
    private final ObjectMapper objectMapper;

    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
    @Cacheable(value = "kwItemSubtypesCache")
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

    @Cacheable(value = "kwFieldMappingCache")
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


    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
    @Cacheable(value = "kwDynamicDocTypeCache", key = "'v3|' + #type")
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

    private void validateGraphQLResponse(JsonNode root) {
        JsonNode errors = root.path("errors");
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

        JsonNode lookups = root.path("data").path("lookups");
        if (!lookups.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(lookups.spliterator(), false)
                .filter(this::isDocumentLookup)
                .filter(node -> !node.path("code").asText()
                        .startsWith(KasewareConstants.DOCUMENT_DRAFT_PREFIX))
                .map(node -> new KwItemSubtypeDto(
                        node.path("code").asText(),
                        node.path("displayValue").asText()))
                .sorted(Comparator.comparing(KwItemSubtypeDto::displayValue))
                .toList();
    }

    private boolean isDocumentLookup(JsonNode node) {
        return "DOCUMENT".equals(node.path("parentCode").asText(null))
                && node.has("code")
                && node.has("displayValue");
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

        JsonNode data = root.path("data");
        if (data == null || data.isMissingNode() || data.isNull()) {
            return null;
        }
        return data.path("allDynamicFormDefinitions");
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
        String id = definition.path("id").asText(null);
        if (id == null || id.isBlank()) {
            return null;
        }
        String definitionType = definition.path("basedOnDocumentType").asText(null);
        if (docType != null
                && !docType.isBlank()
                && !docType.equalsIgnoreCase(definitionType)) {
            return null;
        }

        String title = definition.path("formName").asText(null);
        return new KwDynamicDocType(id, title, List.of());
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
            String formDefId = doc.path("dynamicFormDefinitionId").asText(null);
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

    private KwMonitoringDocument toKwMonitoringDocument(
            JsonNode doc,
            Map<String, Map<String, String>> formFieldLabels,
            Map<String, Map<String, Map<String, String>>> formValueLabels) {

        String formDefId = doc.path("dynamicFormDefinitionId").asText(null);
        Map<String, String> fieldLabels = formFieldLabels.getOrDefault(formDefId, Map.of());
        Map<String, Map<String, String>> valueLabelMap = formValueLabels.getOrDefault(formDefId, Map.of());
        Map<String, Object> attributes = buildAttributes(doc, fieldLabels, valueLabelMap);

        return buildKwMonitoringDocument(doc, formDefId, attributes);
    }

    private Map<String, Object> buildAttributes(
            JsonNode doc,
            Map<String, String> fieldLabels,
            Map<String, Map<String, String>> valueLabelMap) {
        Map<String, Object> attributes = new LinkedHashMap<>();

        putDynamicDataAttribute(attributes, doc.path("dynamicData"), fieldLabels, valueLabelMap);

        putArrayAttributeIfPresent(attributes, "attachments", doc.path("attachments"), false);
        putArrayAttributeIfPresent(attributes, "tags", doc.path("tags"), false);
        putArrayAttributeIfPresent(attributes, "classifications", doc.path("classifications"), false);

        putDocumentMetadataAttributes(attributes, doc);
        putPeopleAttributes(attributes, doc);
        putCaseAttributes(attributes, doc);
        putArrayAttributeIfPresent(attributes, "relatedEntities", doc.path("relatedEntities"), true);

        // Form field metadata for consumers
        attributes.put("formFieldLabels", fieldLabels);
        return attributes;
    }

    private KwMonitoringDocument buildKwMonitoringDocument(
            JsonNode doc,
            String formDefId,
            Map<String, Object> attributes) {
        return KwMonitoringDocument.builder()
                .id(doc.path("id").asText(null))
                .title(doc.path("title").asText(null))
                .body(doc.path("body").asText(null))
                .createdTimestamp(doc.path("createdTimestamp").asLong(0L))
                .updatedTimestamp(doc.path("updatedTimestamp").asLong(0L))
                .dynamicFormDefinitionId(formDefId)
                .dynamicFormDefinitionName(doc.path("dynamicFormDefinitionName").asText(null))
                .dynamicFormVersionNumber(doc.path("dynamicFormVersionNumber").asInt(0))
                .tenantId(doc.path("tenantId").asText(null))
                .attributes(attributes)
                .build();
    }

    private void putDynamicDataAttribute(
            Map<String, Object> attributes,
            JsonNode dynamicData,
            Map<String, String> fieldLabels,
            Map<String, Map<String, String>> valueLabelMap) {
        if (!dynamicData.isObject()) {
            return;
        }

        Map<String, Object> resolvedDynamic = new LinkedHashMap<>();
        dynamicData.fields().forEachRemaining(field -> {
            if ("hiddenFields".equals(field.getKey())) {
                return;
            }
            String label = fieldLabels.getOrDefault(field.getKey(), field.getKey());
            Object value = resolveFieldValue(field.getKey(), field.getValue(), fieldLabels, valueLabelMap);
            resolvedDynamic.put(label, value);
        });
        attributes.put("dynamicData", resolvedDynamic);
    }

    private void putDocumentMetadataAttributes(Map<String, Object> attributes, JsonNode doc) {
        putIfPresent(attributes, "documentType", doc.path("documentType"));
        putIfPresent(attributes, "occurrenceDate", doc.path("occurrenceDate"));
        putIfPresent(attributes, "occurrenceTime", doc.path("occurrenceTime"));
        putIfPresent(attributes, "legacyId", doc.path("legacyId"));
    }

    private void putPeopleAttributes(Map<String, Object> attributes, JsonNode doc) {
        putArrayAttributeIfPresent(attributes, "authors", doc.path("authors"), true);
        putArrayAttributeIfPresent(attributes, "approvers", doc.path("approvers"), true);
    }

    private void putCaseAttributes(Map<String, Object> attributes, JsonNode doc) {
        putArrayAttributeIfPresent(attributes, "serials", doc.path("serials"), true);
        putArrayAttributeIfPresent(attributes, "caseLabels", doc.path("caseLabels"), true);
    }

    private void putArrayAttributeIfPresent(
            Map<String, Object> attributes,
            String key,
            JsonNode node,
            boolean requireNonEmpty) {
        if (!node.isArray()) {
            return;
        }
        if (requireNonEmpty && node.isEmpty()) {
            return;
        }
        attributes.put(key, objectMapper.convertValue(node, List.class));
    }

    private Object resolveFieldValue(String fieldName, JsonNode value,
                                     Map<String, String> fieldLabels,
                                     Map<String, Map<String, String>> valueLabelMap) {
        if (value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            // Try to resolve select/radio option label
            Map<String, String> options = valueLabelMap.get(fieldName);
            if (options != null && options.containsKey(value.asText())) {
                return options.get(value.asText());
            }
            return value.asText();
        }
        if (value.isObject()) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            value.fields().forEachRemaining(f -> {
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
            });
            return resolved;
        }
        return objectMapper.convertValue(value, Object.class);
    }

    private void putIfPresent(Map<String, Object> map, String key, JsonNode node) {
        if (node != null && !node.isMissingNode() && !node.isNull() && node.isTextual()) {
            map.put(key, node.asText());
        }
    }

    private void buildFieldLabels(JsonNode formDef,
                                  Map<String, String> fieldLabels,
                                  Map<String, Map<String, String>> valueLabels) {
        if (formDef == null || formDef.isMissingNode()) {
            return;
        }

        JsonNode targetVersion = selectTargetVersion(formDef.path("versions"));
        if (targetVersion == null) {
            return;
        }

        JsonNode formFields = targetVersion.path("formFields");
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
            if ("ACTIVE".equals(version.path("status").asText(null))) {
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
        String name = field.path("name").asText(null);
        String label = field.path("label").asText(null);

        if (name != null && label != null) {
            fieldLabels.put(name, label);
        }

        if (name == null) {
            return;
        }

        Map<String, String> optionMap = buildOptionMap(field.path("values"));
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
            String optionValue = option.path("value").asText(null);
            String optionLabel = option.path("label").asText(null);
            if (optionValue != null && optionLabel != null) {
                optionMap.put(optionValue, optionLabel);
            }
        }
        return optionMap;
    }

}
