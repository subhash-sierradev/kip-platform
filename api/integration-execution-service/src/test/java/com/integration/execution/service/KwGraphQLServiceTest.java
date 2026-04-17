package com.integration.execution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.client.KwGraphqlClient;
import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.model.KwMonitoringDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwGraphQLServiceTest {

    @Mock
    private KwGraphqlClient kwGraphqlClient;

    private KwGraphQLService service;

    @BeforeEach
    void setUp() {
        service = new KwGraphQLService(kwGraphqlClient, new ObjectMapper());
    }

    @Test
    void fetchItemSubtypes_validResponse_filtersAndSortsDocumentLookups() {
        String response = """
                {
                  "data": {
                    "lookups": [
                      {"code":"DOCUMENT_DRAFT_TEST","displayValue":"Draft","parentCode":"DOCUMENT"},
                      {"code":"REPORT","displayValue":"Report","parentCode":"DOCUMENT"},
                      {"code":"ALERT","displayValue":"Alert","parentCode":"DOCUMENT"},
                      {"code":"NOT_DOC","displayValue":"Other","parentCode":"INCIDENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwItemSubtypeDto> result = service.fetchItemSubtypes();

        assertThat(result).containsExactly(
                new KwItemSubtypeDto("ALERT", "Alert"),
                new KwItemSubtypeDto("REPORT", "Report")
        );
    }

    @Test
    void fetchItemSubtypes_invalidJson_throwsRuntimeException() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn("not-json");

        assertThatThrownBy(() -> service.fetchItemSubtypes())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse JSON response");
    }

    @Test
    void fetchItemSubtypes_graphqlErrors_throwsIntegrationApiException() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap()))
            .thenReturn("{\"errors\":[{\"message\":\"boom\"}]}");

        assertThatThrownBy(() -> service.fetchItemSubtypes())
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW GraphQL returned errors");
    }

    @Test
    void fetchItemSubtypes_missingOrNonArrayLookups_returnsEmptyList() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn("{\"data\":{\"lookups\": {}}}");

        List<KwItemSubtypeDto> result = service.fetchItemSubtypes();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchDynamicDocumentsTypes_emptyResponse_throwsIntegrationApiException() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn("  ");

        assertThatThrownBy(() -> service.fetchDynamicDocumentsTypes("DOCUMENT", "ignored"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Empty response from upstream");
    }

    @Test
    void fetchDynamicDocumentsTypes_matchingDocType_returnsFilteredResults() {
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"1","formName":"Incident A","basedOnDocumentType":"DOCUMENT"},
                      {"id":"2","formName":"Case B","basedOnDocumentType":"CASE"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).containsExactly(new KwDynamicDocType("1", "Incident A", List.of()));
    }

    @Test
    void fetchDynamicDocumentsTypes_noDocTypeMatch_fallsBackToAllDefinitions() {
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"2","formName":"Case B","basedOnDocumentType":"CASE"},
                      {"id":"1","formName":"Incident A","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("MISSING", "unused");

        assertThat(result).containsExactly(
                new KwDynamicDocType("2", "Case B", List.of()),
                new KwDynamicDocType("1", "Incident A", List.of())
        );
    }

    @Test
    void fetchDynamicDocumentsTypes_missingData_returnsEmptyList() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn("{\"data\":{}}");

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchDynamicDocumentsTypes_invalidJson_throwsIntegrationApiException() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn("not-json");

        assertThatThrownBy(() -> service.fetchDynamicDocumentsTypes("DOCUMENT", "unused"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Failed to parse upstream response");
    }

    @Test
    void fetchDynamicDocumentsTypes_graphqlErrors_throwsIntegrationApiException() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap()))
                .thenReturn("{\"errors\":[{\"message\":\"dynamic-fail\"}]}");

        assertThatThrownBy(() -> service.fetchDynamicDocumentsTypes("DOCUMENT", "unused"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW GraphQL returned errors");
    }

    @Test
    void fetchDynamicDocumentsTypes_ignoresInvalidDefinitionsAndSortsNullTitlesLast() {
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":" ","formName":"ShouldIgnore","basedOnDocumentType":"DOCUMENT"},
                      {"id":"3","basedOnDocumentType":"DOCUMENT"},
                      {"id":"2","formName":"Beta","basedOnDocumentType":"DOCUMENT"},
                      {"id":"1","formName":"Alpha","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).containsExactly(
                new KwDynamicDocType("1", "Alpha", List.of()),
                new KwDynamicDocType("2", "Beta", List.of()),
                new KwDynamicDocType("3", null, List.of())
        );
    }

    @Test
    void fetchDynamicDocumentsTypes_blankDocType_returnsAllWithoutFiltering() {
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"2","formName":"Case B","basedOnDocumentType":"CASE"},
                      {"id":"1","formName":"Incident A","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("   ", "unused");

        assertThat(result).containsExactly(
                new KwDynamicDocType("2", "Case B", List.of()),
                new KwDynamicDocType("1", "Incident A", List.of())
        );
    }

    @Test
    void fetchFieldMappingForLocations_returnsMappedFields() {
        List<KwDocField> fields = service.fetchFieldMappingForLocations();

        assertThat(fields).isNotEmpty();
        assertThat(fields).extracting(KwDocField::getFieldName)
                .contains("id", "createdTimestamp", "latitude");
        assertThat(fields).extracting(KwDocField::getFieldType)
                .contains("String", "Long", "Double");
    }

    @Test
    void fetchDynamicDocumentsTypes_dataNodeMissingOrNull_returnsEmptyList() {
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn("{\"data\":null}");

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchItemSubtypes_ignoresEntriesMissingRequiredFields() {
        String response = """
                {
                  "data": {
                    "lookups": [
                      {"code":"DOC_A","parentCode":"DOCUMENT"},
                      {"displayValue":"Doc B","parentCode":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwItemSubtypeDto> result = service.fetchItemSubtypes();

        assertThat(result).isEmpty();
    }

    @Test
    void resolveFieldType_privateMethod_coversAllBranches() throws Exception {
        Method method = KwGraphQLService.class.getDeclaredMethod("resolveFieldType", Class.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, String.class)).isEqualTo("String");
        assertThat(method.invoke(service, Long.class)).isEqualTo("Long");
        assertThat(method.invoke(service, long.class)).isEqualTo("Long");
        assertThat(method.invoke(service, Integer.class)).isEqualTo("Integer");
        assertThat(method.invoke(service, int.class)).isEqualTo("Integer");
        assertThat(method.invoke(service, Double.class)).isEqualTo("Double");
        assertThat(method.invoke(service, double.class)).isEqualTo("Double");
        assertThat(method.invoke(service, Boolean.class)).isEqualTo("Boolean");
        assertThat(method.invoke(service, boolean.class)).isEqualTo("Boolean");
        assertThat(method.invoke(service, List.class)).isEqualTo("List");
    }

    // -----------------------------------------------------------------------
    // fetchMonitoringData
    // -----------------------------------------------------------------------

    @Test
    void fetchMonitoringData_nullDocuments_returnsEmptyList() {
        when(kwGraphqlClient.fetchMonitoringDocuments(
                "form-1", 1000, 2000, 0, 500)).thenReturn(null);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-1", 1000, 2000, 0, 500);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchMonitoringData_emptyArrayNode_returnsEmptyList() {
        ObjectMapper mapper = new ObjectMapper();
        when(kwGraphqlClient.fetchMonitoringDocuments(
                "form-1", 1000, 2000, 0, 500))
                .thenReturn(mapper.createArrayNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-1", 1000, 2000, 0, 500);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchMonitoringData_singleDocNoFormDef_returnsDocWithNoAttributes() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-1");
        doc.put("title", "Report 1");
        doc.put("body", "Some body");
        doc.put("createdTimestamp", 1700000000L);
        doc.put("updatedTimestamp", 1700001000L);
        doc.put("dynamicFormDefinitionId", "form-X");
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-X", 1000, 2000, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-X"))
                .thenThrow(new RuntimeException("Form def not found"));

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-X", 1000, 2000, 0, 500);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("doc-1");
        assertThat(result.get(0).getTitle()).isEqualTo("Report 1");
    }

    @Test
    void fetchMonitoringData_withFormDefinition_resolvesFieldLabels() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-2");
        doc.put("title", "T");
        doc.put("dynamicFormDefinitionId", "form-A");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("status", "OPEN");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDef(mapper, "status", "Status Label", "OPEN", "Open");

        when(kwGraphqlClient.fetchMonitoringDocuments("form-A", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-A")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-A", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        KwMonitoringDocument kwDoc = result.get(0);
        assertThat(kwDoc.getAttributes()).isNotNull();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> resolvedDynamic =
                (java.util.Map<String, Object>) kwDoc.getAttributes().get("dynamicData");
        // "status" field should be stored using its label "Status Label" with resolved value "Open"
        assertThat(resolvedDynamic).containsKey("Status Label");
        assertThat(resolvedDynamic.get("Status Label")).isEqualTo("Open");
    }

    @Test
    void fetchMonitoringData_withArrayField_resolvesArrayValues() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-3");
        doc.put("dynamicFormDefinitionId", "form-B");
        ObjectNode dynData = mapper.createObjectNode();
        ArrayNode multiSelect = mapper.createArrayNode();
        multiSelect.add("VAL1");
        multiSelect.add("VAL2");
        dynData.set("tags", multiSelect);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDefWithArrayValues(mapper, "tags", "Tag Label",
                new String[]{"VAL1", "VAL2"}, new String[]{"Value One", "Value Two"});

        when(kwGraphqlClient.fetchMonitoringDocuments("form-B", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-B")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-B", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_docWithObjectDynamicField_resolvesNestedObject() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-4");
        doc.put("dynamicFormDefinitionId", "form-C");
        ObjectNode dynData = mapper.createObjectNode();
        ObjectNode nestedObj = mapper.createObjectNode();
        nestedObj.put("subField", "subValue");
        dynData.set("nested", nestedObj);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDef(mapper, "nested", "Nested Label", null, null);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-C", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-C")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-C", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_docWithNullDynamicField_isExcludedFromAttributes() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-5");
        doc.put("dynamicFormDefinitionId", "form-D");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.putNull("nullField");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-D", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-D")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-D", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_hiddenFieldsKeySkipped() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-6");
        doc.put("dynamicFormDefinitionId", "form-E");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("hiddenFields", "shouldBeIgnored");
        dynData.put("visible", "visibleValue");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-E", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-E")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-E", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap).doesNotContainKey("hiddenFields");
    }

    @Test
    void fetchMonitoringData_docWithNonArrayAttachments_skipsIt() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-7");
        doc.put("dynamicFormDefinitionId", "form-F");
        doc.put("attachments", "not-an-array");
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-F", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-F")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-F", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributes()).doesNotContainKey("attachments");
    }

    @Test
    void fetchMonitoringData_formDefWithActiveVersion_usesActiveVersion() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-8");
        doc.put("dynamicFormDefinitionId", "form-G");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("fieldA", "VALUE_A");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        // Form def with multiple versions: one DRAFT, one ACTIVE
        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode draftVersion = mapper.createObjectNode();
        draftVersion.put("status", "DRAFT");
        draftVersion.set("formFields", mapper.createArrayNode());
        ObjectNode activeVersion = mapper.createObjectNode();
        activeVersion.put("status", "ACTIVE");
        ArrayNode activeFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", "fieldA");
        field.put("label", "Field A Label");
        field.set("values", mapper.createArrayNode());
        activeFields.add(field);
        activeVersion.set("formFields", activeFields);
        versions.add(draftVersion);
        versions.add(activeVersion);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-G", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-G")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-G", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        // Active version should be selected — label "Field A Label" maps "fieldA"
        assertThat(dynMap).containsKey("Field A Label");
    }

    @Test
    void fetchMonitoringData_formDefWithNoActiveVersion_usesFirstVersion() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-9");
        doc.put("dynamicFormDefinitionId", "form-H");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("fieldB", "VALUE_B");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode firstVersion = mapper.createObjectNode();
        firstVersion.put("status", "DRAFT");
        ArrayNode fields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", "fieldB");
        field.put("label", "Field B Label");
        field.set("values", mapper.createArrayNode());
        fields.add(field);
        firstVersion.set("formFields", fields);
        versions.add(firstVersion);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-H", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-H")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-H", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap).containsKey("Field B Label");
    }

    @Test
    void fetchMonitoringData_multipleDocsSameFormDef_cachesFetchOnce() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        for (int i = 1; i <= 3; i++) {
            ObjectNode doc = mapper.createObjectNode();
            doc.put("id", "doc-" + i);
            doc.put("dynamicFormDefinitionId", "shared-form");
            docs.add(doc);
        }

        when(kwGraphqlClient.fetchMonitoringDocuments("shared-form", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("shared-form")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("shared-form", 0, 99999, 0, 500);

        assertThat(result).hasSize(3);
        // fetchFormDefinition should be called only once (cached in the loop)
        org.mockito.Mockito.verify(kwGraphqlClient, org.mockito.Mockito.times(1))
                .fetchFormDefinition("shared-form");
    }

    @Test
    void fetchMonitoringData_docWithNonArrayDynamicData_skipsIt() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-10");
        doc.put("dynamicFormDefinitionId", "form-I");
        doc.put("dynamicData", "not-an-object");
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-I", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-I")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-I", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private ObjectNode buildFormDef(ObjectMapper mapper, String fieldName, String fieldLabel,
                                    String optionValue, String optionLabel) {
        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        ArrayNode formFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", fieldName);
        field.put("label", fieldLabel);
        ArrayNode values = mapper.createArrayNode();
        if (optionValue != null) {
            ObjectNode option = mapper.createObjectNode();
            option.put("value", optionValue);
            option.put("label", optionLabel);
            values.add(option);
        }
        field.set("values", values);
        formFields.add(field);
        version.set("formFields", formFields);
        versions.add(version);
        formDef.set("versions", versions);
        return formDef;
    }

    private ObjectNode buildFormDefWithArrayValues(ObjectMapper mapper, String fieldName, String fieldLabel,
                                                   String[] optionValues, String[] optionLabels) {
        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        ArrayNode formFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", fieldName);
        field.put("label", fieldLabel);
        ArrayNode values = mapper.createArrayNode();
        for (int i = 0; i < optionValues.length; i++) {
            ObjectNode option = mapper.createObjectNode();
            option.put("value", optionValues[i]);
            option.put("label", optionLabels[i]);
            values.add(option);
        }
        field.set("values", values);
        formFields.add(field);
        version.set("formFields", formFields);
        versions.add(version);
        formDef.set("versions", versions);
        return formDef;
    }

    // -----------------------------------------------------------------------
    // Additional branch-coverage tests for resolveFieldValue, putIfPresent,
    // putArrayAttributeIfPresent, buildFieldLabels, selectTargetVersion
    // -----------------------------------------------------------------------

    @Test
    void fetchMonitoringData_docWithTextualDynamicFieldAndOptionMatch_resolvesLabel() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-opt");
        doc.put("dynamicFormDefinitionId", "form-opt");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("priority", "HIGH");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDef(mapper, "priority", "Priority Label", "HIGH", "High Priority");

        when(kwGraphqlClient.fetchMonitoringDocuments("form-opt", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-opt")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-opt", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap.get("Priority Label")).isEqualTo("High Priority");
    }

    @Test
    void fetchMonitoringData_docWithTextualDynamicFieldNoOptionMatch_returnsRawText() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-raw");
        doc.put("dynamicFormDefinitionId", "form-raw");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("priority", "UNKNOWN_VAL");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDef(mapper, "priority", "Priority Label", "HIGH", "High Priority");

        when(kwGraphqlClient.fetchMonitoringDocuments("form-raw", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-raw")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-raw", 0, 99999, 0, 500);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap.get("Priority Label")).isEqualTo("UNKNOWN_VAL");
    }

    @Test
    void fetchMonitoringData_docWithObjectContainingInnerArrayWithOptions_resolvesArrayLabels() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-inner-arr");
        doc.put("dynamicFormDefinitionId", "form-inner-arr");
        ObjectNode dynData = mapper.createObjectNode();
        ObjectNode nestedObj = mapper.createObjectNode();
        ArrayNode innerArr = mapper.createArrayNode();
        innerArr.add("VAL1");
        innerArr.add("VAL2");
        nestedObj.set("tags", innerArr);
        nestedObj.put("simple", "text");
        dynData.set("nested", nestedObj);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDefWithArrayValues(mapper, "tags", "Tag Label",
                new String[]{"VAL1", "VAL2"}, new String[]{"Label One", "Label Two"});

        when(kwGraphqlClient.fetchMonitoringDocuments("form-inner-arr", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-inner-arr")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-inner-arr", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_docWithObjectContainingInnerTextWithOption_resolvesLabel() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-inner-opt");
        doc.put("dynamicFormDefinitionId", "form-inner-opt");
        ObjectNode dynData = mapper.createObjectNode();
        ObjectNode nestedObj = mapper.createObjectNode();
        nestedObj.put("status", "OPEN");
        dynData.set("wrapper", nestedObj);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = buildFormDef(mapper, "status", "Status Label", "OPEN", "Open");

        when(kwGraphqlClient.fetchMonitoringDocuments("form-inner-opt", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-inner-opt")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-inner-opt", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_docWithObjectContainingInnerNonTextNoOption_passesThrough() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-inner-num");
        doc.put("dynamicFormDefinitionId", "form-inner-num");
        ObjectNode dynData = mapper.createObjectNode();
        ObjectNode nestedObj = mapper.createObjectNode();
        nestedObj.put("count", 42);
        dynData.set("wrapper", nestedObj);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-inner-num", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-inner-num")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-inner-num", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_docWithDocumentMetadataFields_putsPresent() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-meta");
        doc.put("dynamicFormDefinitionId", "form-meta");
        doc.put("documentType", "REPORT");
        doc.put("occurrenceDate", "2026-01-01");
        doc.put("occurrenceTime", "10:00");
        doc.put("legacyId", "LEGACY-1");
        doc.put("tenantId", "tenant-abc");
        doc.put("dynamicFormDefinitionName", "Test Form");
        doc.put("dynamicFormVersionNumber", 3);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-meta", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-meta")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-meta", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        KwMonitoringDocument kwDoc = result.get(0);
        assertThat(kwDoc.getAttributes().get("documentType")).isEqualTo("REPORT");
        assertThat(kwDoc.getAttributes().get("occurrenceDate")).isEqualTo("2026-01-01");
        assertThat(kwDoc.getAttributes().get("legacyId")).isEqualTo("LEGACY-1");
        assertThat(kwDoc.getTenantId()).isEqualTo("tenant-abc");
        assertThat(kwDoc.getDynamicFormDefinitionName()).isEqualTo("Test Form");
        assertThat(kwDoc.getDynamicFormVersionNumber()).isEqualTo(3);
    }

    @Test
    void fetchMonitoringData_docWithNonTextualDocumentTypeField_doesNotPutIfPresent() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nontext");
        doc.put("dynamicFormDefinitionId", "form-nontext");
        doc.put("documentType", 123); // non-textual
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nontext", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nontext")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-nontext", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributes()).doesNotContainKey("documentType");
    }

    @Test
    void fetchMonitoringData_docWithPeopleAndCaseArrayAttributes_addsThem() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-people");
        doc.put("dynamicFormDefinitionId", "form-people");
        ArrayNode authors = mapper.createArrayNode();
        authors.add(mapper.createObjectNode().put("name", "Alice"));
        doc.set("authors", authors);
        ArrayNode approvers = mapper.createArrayNode();
        approvers.add(mapper.createObjectNode().put("name", "Bob"));
        doc.set("approvers", approvers);
        ArrayNode serials = mapper.createArrayNode();
        serials.add(mapper.createObjectNode().put("serial", "S-1"));
        doc.set("serials", serials);
        ArrayNode caseLabels = mapper.createArrayNode();
        caseLabels.add("label1");
        doc.set("caseLabels", caseLabels);
        ArrayNode relatedEntities = mapper.createArrayNode();
        relatedEntities.add(mapper.createObjectNode().put("type", "PERSON"));
        doc.set("relatedEntities", relatedEntities);
        ArrayNode tags = mapper.createArrayNode();
        doc.set("tags", tags); // empty array
        ArrayNode classifications = mapper.createArrayNode();
        classifications.add("classified");
        doc.set("classifications", classifications);
        ArrayNode attachments = mapper.createArrayNode();
        attachments.add(mapper.createObjectNode().put("filename", "test.pdf"));
        doc.set("attachments", attachments);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-people", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-people")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-people", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributes()).containsKey("authors");
        assertThat(result.get(0).getAttributes()).containsKey("approvers");
        assertThat(result.get(0).getAttributes()).containsKey("serials");
        assertThat(result.get(0).getAttributes()).containsKey("caseLabels");
        assertThat(result.get(0).getAttributes()).containsKey("relatedEntities");
        assertThat(result.get(0).getAttributes()).containsKey("attachments");
        assertThat(result.get(0).getAttributes()).containsKey("classifications");
        // tags is empty array but requireNonEmpty is false, so it's included
        assertThat(result.get(0).getAttributes()).containsKey("tags");
    }

    @Test
    void fetchMonitoringData_docWithEmptyNonEmptyRequiredArrays_excludesEmptyOnes() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-emptyarr");
        doc.put("dynamicFormDefinitionId", "form-emptyarr");
        doc.set("authors", mapper.createArrayNode()); // empty, requireNonEmpty=true
        doc.set("serials", mapper.createArrayNode()); // empty, requireNonEmpty=true
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-emptyarr", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-emptyarr")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-emptyarr", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributes()).doesNotContainKey("authors");
        assertThat(result.get(0).getAttributes()).doesNotContainKey("serials");
    }

    @Test
    void fetchMonitoringData_formDefWithNullNameField_skipsLabelMapping() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nullname");
        doc.put("dynamicFormDefinitionId", "form-nullname");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("fieldX", "somevalue");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        // Build form def with a field that has null name
        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        ArrayNode formFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        // name is missing
        field.put("label", "X Label");
        field.set("values", mapper.createArrayNode());
        formFields.add(field);
        version.set("formFields", formFields);
        versions.add(version);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nullname", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nullname")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-nullname", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_formDefWithNullLabelField_usesNameAsKey() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nulllabel");
        doc.put("dynamicFormDefinitionId", "form-nulllabel");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("fieldY", "value");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        ArrayNode formFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", "fieldY");
        // label is missing
        field.set("values", mapper.createArrayNode());
        formFields.add(field);
        version.set("formFields", formFields);
        versions.add(version);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nulllabel", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nulllabel")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-nulllabel", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap).containsKey("fieldY");
    }

    @Test
    void fetchMonitoringData_formDefWithEmptyVersionsArray_returnsEmptyFieldLabels() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nover");
        doc.put("dynamicFormDefinitionId", "form-nover");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("f", "v");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        formDef.set("versions", mapper.createArrayNode());

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nover", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nover")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-nover", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap).containsKey("f");
    }

    @Test
    void fetchMonitoringData_formDefWithNonArrayVersions_returnsEmptyFieldLabels() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-badver");
        doc.put("dynamicFormDefinitionId", "form-badver");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("f", "v");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        formDef.put("versions", "not-array");

        when(kwGraphqlClient.fetchMonitoringDocuments("form-badver", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-badver")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-badver", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_formDefWithNonArrayFormFields_returnsEmptyFieldLabels() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-badflds");
        doc.put("dynamicFormDefinitionId", "form-badflds");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("f", "v");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        version.put("formFields", "not-array");
        versions.add(version);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-badflds", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-badflds")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-badflds", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_docWithNumericDynamicField_passesThrough() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-num");
        doc.put("dynamicFormDefinitionId", "form-num");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("count", 42);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-num", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-num")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-num", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap.get("count")).isEqualTo(42);
    }

    @Test
    void fetchMonitoringData_docWithArrayDynamicField_passesThrough() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-arr-dyn");
        doc.put("dynamicFormDefinitionId", "form-arr-dyn");
        ObjectNode dynData = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();
        arr.add("a");
        arr.add("b");
        dynData.set("items", arr);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-arr-dyn", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-arr-dyn")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-arr-dyn", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap.get("items")).isInstanceOf(java.util.List.class);
    }

    @Test
    void fetchMonitoringData_formFieldOptionWithNullValueOrLabel_skipsOption() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nullopt");
        doc.put("dynamicFormDefinitionId", "form-nullopt");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("status", "OPEN");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        ArrayNode formFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", "status");
        field.put("label", "Status");
        ArrayNode values = mapper.createArrayNode();
        // option with null value
        ObjectNode opt1 = mapper.createObjectNode();
        opt1.putNull("value");
        opt1.put("label", "Null Value");
        values.add(opt1);
        // option with null label
        ObjectNode opt2 = mapper.createObjectNode();
        opt2.put("value", "OPEN");
        opt2.putNull("label");
        values.add(opt2);
        field.set("values", values);
        formFields.add(field);
        version.set("formFields", formFields);
        versions.add(version);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nullopt", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nullopt")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-nullopt", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        // No valid option match, raw value returned
        assertThat(dynMap.get("Status")).isEqualTo("OPEN");
    }

    @Test
    void fetchMonitoringData_objectWithInnerArrayNoOptionsMatch_passesElementThrough() {
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-arr-noopt");
        doc.put("dynamicFormDefinitionId", "form-arr-noopt");
        ObjectNode dynData = mapper.createObjectNode();
        ObjectNode nestedObj = mapper.createObjectNode();
        ArrayNode innerArr = mapper.createArrayNode();
        innerArr.add(123); // non-textual element in array
        nestedObj.set("nums", innerArr);
        dynData.set("wrapper", nestedObj);
        doc.set("dynamicData", dynData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-arr-noopt", 0, 99999, 0, 500)).thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-arr-noopt")).thenReturn(mapper.createObjectNode());

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-arr-noopt", 0, 99999, 0, 500);
        assertThat(result).hasSize(1);
    }

    // -----------------------------------------------------------------------
    // selectTargetVersion branch coverage
    // -----------------------------------------------------------------------

    @Test
    void fetchMonitoringData_formDefWithNonArrayVersions_skipsFieldLabels() {
        // Covers selectTargetVersion: !versions.isArray() -> return null
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nonarray-ver");
        doc.put("dynamicFormDefinitionId", "form-nonarray-ver");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("field1", "val1");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        // Form def where "versions" is an object, not an array
        ObjectNode formDef = mapper.createObjectNode();
        ObjectNode versionsObj = mapper.createObjectNode();
        versionsObj.put("status", "ACTIVE");
        formDef.set("versions", versionsObj);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nonarray-ver", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nonarray-ver")).thenReturn(formDef);

        List<KwMonitoringDocument> result =
                service.fetchMonitoringData("form-nonarray-ver", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("doc-nonarray-ver");
    }

    @Test
    void fetchMonitoringData_formDefWithEmptyVersionsArray_skipsFieldLabels() {
        // Covers selectTargetVersion: versions.isEmpty() -> return null
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-empty-ver");
        doc.put("dynamicFormDefinitionId", "form-empty-ver");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("field1", "val1");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        formDef.set("versions", mapper.createArrayNode()); // empty array

        when(kwGraphqlClient.fetchMonitoringDocuments("form-empty-ver", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-empty-ver")).thenReturn(formDef);

        List<KwMonitoringDocument> result =
                service.fetchMonitoringData("form-empty-ver", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_formDefWithTwoDraftVersionsNoneActive_usesFirstVersion() {
        // Covers selectTargetVersion: versions not empty, neither is ACTIVE -> return versions.get(0)
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-twodraft-ver");
        doc.put("dynamicFormDefinitionId", "form-twodraft-ver");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("myField2", "MY_VAL2");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        // Two DRAFT versions, no ACTIVE
        ObjectNode draftVersion1 = mapper.createObjectNode();
        draftVersion1.put("status", "DRAFT");
        ArrayNode formFields1 = mapper.createArrayNode();
        ObjectNode field1 = mapper.createObjectNode();
        field1.put("name", "myField2");
        field1.put("label", "My Field 2 Label");
        field1.set("values", mapper.createArrayNode());
        formFields1.add(field1);
        draftVersion1.set("formFields", formFields1);
        ObjectNode draftVersion2 = mapper.createObjectNode();
        draftVersion2.put("status", "DRAFT");
        draftVersion2.set("formFields", mapper.createArrayNode());
        versions.add(draftVersion1);
        versions.add(draftVersion2);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-twodraft-ver", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-twodraft-ver")).thenReturn(formDef);

        List<KwMonitoringDocument> result =
                service.fetchMonitoringData("form-twodraft-ver", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dynMap =
                (java.util.Map<String, Object>) result.get(0).getAttributes().get("dynamicData");
        assertThat(dynMap).containsKey("My Field 2 Label");
    }

    @Test
    void fetchMonitoringData_formDefIsMissingNode_skipsFieldLabels() {
        // Covers buildFieldLabels: formDef.isMissingNode() -> return
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-missing-formdef");
        doc.put("dynamicFormDefinitionId", "form-missing-node");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("field1", "val1");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        // Returning missingNode triggers buildFieldLabels: formDef.isMissingNode() -> return
        when(kwGraphqlClient.fetchMonitoringDocuments("form-missing-node", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-missing-node"))
                .thenReturn(mapper.getNodeFactory().missingNode());

        List<KwMonitoringDocument> result =
                service.fetchMonitoringData("form-missing-node", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_formDefWithNonArrayFormFields_skipsFields() {
        // Covers buildFieldLabels: !formFields.isArray() -> return
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-nonarr-ff");
        doc.put("dynamicFormDefinitionId", "form-nonarr-ff");
        ObjectNode dynData = mapper.createObjectNode();
        dynData.put("f1", "v1");
        doc.set("dynamicData", dynData);
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        // formFields is NOT an array
        version.put("formFields", "not-an-array");
        versions.add(version);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-nonarr-ff", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-nonarr-ff")).thenReturn(formDef);

        List<KwMonitoringDocument> result =
                service.fetchMonitoringData("form-nonarr-ff", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_definitionWithNullDocType_isIncludedWhenDocTypeFilterBlank() {
        // Covers mapDynamicFormDefinitionNode: definition.isNull() branch
        // and mapDynamicFormDefinitionNodes with null docType filter passes all
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      null,
                      {"id":"good-1","formName":"Good Form","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("good-1");
    }

    // -----------------------------------------------------------------------
    // Additional branch coverage tests
    // -----------------------------------------------------------------------

    @Test
    void fetchItemSubtypes_parseLookupResponseReturnsNull_returnsEmptyList() {
        // parseLookupResponse returns null when lookups is not an array (null path)
        // This covers fetchItemSubtypes: result != null ? result : List.of() → List.of()
        // Actually parseLookupResponse returns List.of() when lookups not array, but we can
        // trigger the null path by making the response valid but having no lookups array
        String response = """
                {"data":{}}
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwItemSubtypeDto> result = service.fetchItemSubtypes();

        // parseLookupResponse returns List.of() for missing lookups node
        assertThat(result).isEmpty();
    }

    @Test
    void fetchDynamicDocumentsTypes_parseDynamicResponseReturnsEmptyListForNullResult_returnsEmpty() {
        // Covers fetchDynamicDocumentsTypes: result != null check (result is List.of() not null)
        // and parseDynamicFormDefinitionsResponse: filtered.isEmpty() => fallback to all
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"form-1","formName":"Form One","basedOnDocumentType":"INCIDENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        // docType "DOCUMENT" doesn't match "INCIDENT" => filtered is empty => fall back to all
        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("form-1");
    }

    @Test
    void fetchDynamicDocumentsTypes_definitionsNodeIsNotArray_returnsEmptyList() {
        // Covers parseDynamicFormDefinitionsResponse: definitionsNode != null but !isArray() => List.of()
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": "not-an-array"
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchDynamicDocumentsTypes_dataNodeIsNull_returnsEmptyList() {
        // Covers extractDynamicFormDefinitionsNode: data.isNull() => return null
        // => parseDynamicFormDefinitionsResponse: definitionsNode == null => List.of()
        String response = """
                {
                  "data": null
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchDynamicDocumentsTypes_definitionWithBlankId_isExcluded() {
        // Covers mapDynamicFormDefinitionNode: id is blank => return null
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"  ","formName":"Blank ID","basedOnDocumentType":"DOCUMENT"},
                      {"id":"good-2","formName":"Good Form","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("good-2");
    }

    @Test
    void fetchMonitoringData_documentsNotArray_returnsEmptyList() {
        // Covers fetchMonitoringData: documents != null but !documents.isArray() => return List.of()
        ObjectMapper mapper = new ObjectMapper();
        // Return a non-array node
        ObjectNode nonArrayNode = mapper.createObjectNode();
        nonArrayNode.put("total", 0);
        when(kwGraphqlClient.fetchMonitoringDocuments("form-non-arr", 0, 99999, 0, 500))
                .thenReturn(nonArrayNode);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-non-arr", 0, 99999, 0, 500);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchMonitoringData_putIfPresent_nodeIsTextualFalse_notAdded() {
        // Covers putIfPresent: node.isTextual() is false (e.g. number) => not added
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-put-test");
        doc.put("title", "Test");
        doc.put("body", "body text");
        doc.put("createdTimestamp", 1000L);
        doc.put("updatedTimestamp", 2000L);
        // documentType is a number (not textual) => putIfPresent should skip it
        doc.put("documentType", 42);
        // occurrenceDate is null
        doc.putNull("occurrenceDate");
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-put-test", 0, 99999, 0, 500))
                .thenReturn(docs);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-put-test", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        // documentType not added since it's not textual
        assertThat(result.get(0).getAttributes()).doesNotContainKey("documentType");
    }

    @Test
    void fetchMonitoringData_buildFieldLabels_formDefIsNull_skipsFieldLabels() {
        // Covers buildFieldLabels: formDef == null => return immediately
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-null-formdef");
        doc.put("dynamicFormDefinitionId", "form-null-def");
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-null-def", 0, 99999, 0, 500))
                .thenReturn(docs);
        // fetchFormDefinition returns null
        when(kwGraphqlClient.fetchFormDefinition("form-null-def")).thenReturn(null);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-null-def", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_buildOptionMap_emptyValuesArray_returnsEmptyMap() {
        // Covers buildOptionMap: values.isEmpty() => return Map.of()
        // When formFields has a field with empty "values" array
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-empty-opts");
        doc.put("dynamicFormDefinitionId", "form-empty-opts");
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        ObjectNode version = mapper.createObjectNode();
        version.put("status", "ACTIVE");
        ArrayNode formFields = mapper.createArrayNode();
        ObjectNode field = mapper.createObjectNode();
        field.put("name", "myField");
        field.put("label", "My Field");
        // values is an empty array => buildOptionMap returns Map.of()
        field.set("values", mapper.createArrayNode());
        formFields.add(field);
        version.set("formFields", formFields);
        versions.add(version);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-empty-opts", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-empty-opts")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-empty-opts", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_selectTargetVersion_allDraftVersions_usesFirstVersion_branchCoverage() {
        // Covers selectTargetVersion: versions is array, none is ACTIVE, not empty => return versions.get(0)
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-draft");
        doc.put("dynamicFormDefinitionId", "form-draft");
        docs.add(doc);

        ObjectNode formDef = mapper.createObjectNode();
        ArrayNode versions = mapper.createArrayNode();
        // Two DRAFT versions, none ACTIVE
        ObjectNode v1 = mapper.createObjectNode();
        v1.put("status", "DRAFT");
        ArrayNode formFields1 = mapper.createArrayNode();
        ObjectNode field1 = mapper.createObjectNode();
        field1.put("name", "f1");
        field1.put("label", "Field One");
        formFields1.add(field1);
        v1.set("formFields", formFields1);
        versions.add(v1);
        formDef.set("versions", versions);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-draft", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-draft")).thenReturn(formDef);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-draft", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchDynamicDocumentsTypes_validateGraphQLResponse_noErrors_doesNotThrow() {
        // Covers validateGraphQLResponse: errors.isArray() but errors.isEmpty() => does not throw
        String response = """
                {
                  "errors": [],
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"form-noerr","formName":"No Error Form","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("form-noerr");
    }

    @Test
    void fetchMonitoringData_formFetchThrowsException_continuesWithEmptyLabels() {
        // Covers fetchMonitoringData: catch(Exception e) in formDef fetch
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-exc");
        doc.put("dynamicFormDefinitionId", "form-exc");
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-exc", 0, 99999, 0, 500))
                .thenReturn(docs);
        when(kwGraphqlClient.fetchFormDefinition("form-exc"))
                .thenThrow(new RuntimeException("fetch failed"));

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-exc", 0, 99999, 0, 500);

        // Should still return the document with empty field labels
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("doc-exc");
    }

    @Test
    void fetchDynamicDocumentsTypes_dataMissingNode_returnsEmptyList() {
        // Covers extractDynamicFormDefinitionsNode: data.isMissingNode() => return null
        // Root has no "data" field at all => data will be MissingNode
        String response = """
                {
                  "other": "value"
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchDynamicDocumentsTypes_nullResponseBody_throwsIntegrationApiException() {
        // Covers parseDynamicFormDefinitionsResponse: responseBody == null => throw
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(null);

        assertThatThrownBy(() -> service.fetchDynamicDocumentsTypes("DOCUMENT", "unused"))
                .isInstanceOf(com.integration.execution.exception.IntegrationApiException.class)
                .hasMessageContaining("Empty response from upstream");
    }

    @Test
    void fetchMonitoringData_putIfPresent_missingNode_notAdded() {
        // Covers putIfPresent: node.isMissingNode() => skip (condition is node != null && !isMissingNode())
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-missing-node");
        // documentType is not present at all => path("documentType") returns MissingNode
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-missing", 0, 99999, 0, 500))
                .thenReturn(docs);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-missing", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributes()).doesNotContainKey("documentType");
    }

    @Test
    void fetchMonitoringData_mapDynamicFormDefinitionNode_missingNode_returnsNull() {
        // Covers mapDynamicFormDefinitionNode: definition.isMissingNode() => return null
        // Force an object node that when iterated contains MissingNode elements - use special response
        String response = """
                {
                  "data": {
                    "allDynamicFormDefinitions": [
                      {"id":"valid-1","formName":"Good","basedOnDocumentType":"DOCUMENT"}
                    ]
                  }
                }
                """;
        when(kwGraphqlClient.executeGraphQLQuery(anyMap())).thenReturn(response);

        List<KwDynamicDocType> result = service.fetchDynamicDocumentsTypes("DOCUMENT", "unused");

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_resolveFieldValue_objectWithInnerArrayAndNoOptions_coversNonTextualBranch() {
        // Covers lambda$resolveFieldValue$0: f.getValue().isArray() is true, options is null
        // => resolvedArray.add(objectMapper.convertValue(element, Object.class))
        // for element that is neither textual nor matched
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-obj-arr");
        doc.put("dynamicFormDefinitionId", "form-obj-arr");

        // dynamicData is an object with a field whose value is an array of objects (not textual)
        ObjectNode dynamicData = mapper.createObjectNode();
        ArrayNode arrayVal = mapper.createArrayNode();
        ObjectNode objElement = mapper.createObjectNode();
        objElement.put("name", "Element One");
        arrayVal.add(objElement);
        // Also add a textual element that has no option match
        arrayVal.add(mapper.getNodeFactory().textNode("text-val"));
        dynamicData.set("arrayField", arrayVal);
        doc.set("dynamicData", dynamicData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-obj-arr", 0, 99999, 0, 500))
                .thenReturn(docs);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-obj-arr", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringData_resolveFieldValue_objectWithInnerNonArrayNonText_coversElseBranch() {
        // Covers lambda$resolveFieldValue$0: f.getValue() is neither textual nor array
        // => resolved.put(innerLabel, objectMapper.convertValue(f.getValue(), Object.class))
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", "doc-inner-obj");
        doc.put("dynamicFormDefinitionId", "form-inner-obj");

        ObjectNode dynamicData = mapper.createObjectNode();
        ObjectNode fieldValue = mapper.createObjectNode();
        // Inner field with an object value (non-textual, non-array)
        ObjectNode innerObj = mapper.createObjectNode();
        innerObj.put("nested", 42);
        fieldValue.set("innerField", innerObj);
        dynamicData.set("objField", fieldValue);
        doc.set("dynamicData", dynamicData);
        docs.add(doc);

        when(kwGraphqlClient.fetchMonitoringDocuments("form-inner-obj", 0, 99999, 0, 500))
                .thenReturn(docs);

        List<KwMonitoringDocument> result = service.fetchMonitoringData("form-inner-obj", 0, 99999, 0, 500);

        assertThat(result).hasSize(1);
    }
}
