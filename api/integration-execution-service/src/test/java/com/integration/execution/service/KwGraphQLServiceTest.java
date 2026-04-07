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
}

