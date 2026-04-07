package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.exception.FieldMappingProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JiraFieldMappingResolverTest {

    private JiraFieldMappingResolver resolver;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resolver = new JiraFieldMappingResolver(objectMapper, new JiraJsonPathResolver());
    }

    @Test
    void processFieldMapping_requiredWithoutTemplateAndDefault_throwsException() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .required(true)
                .build();

        assertThatThrownBy(() -> resolver.processFieldMapping(mapping, objectMapper.createObjectNode(), "wh-1"))
                .isInstanceOf(FieldMappingProcessingException.class)
                .hasMessageContaining("Required field Summary has no value and no default");
    }

    @Test
    void processFieldMapping_emptyTemplate_usesDefaultValue() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .template(" ")
                .defaultValue("Fallback")
                .required(false)
                .build();

        Object result = resolver.processFieldMapping(mapping, objectMapper.createObjectNode(), "wh-2");

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    void processFieldMapping_templateValue_convertsToNumber() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_10010")
                .jiraFieldName("Estimate")
                .dataType(JiraDataType.NUMBER)
                .template("{{issue.estimate}}")
                .required(false)
                .build();

        JsonNode payload = objectMapper.readTree("{\"issue\":{\"estimate\":\"42\"}}");

        Object result = resolver.processFieldMapping(mapping, payload, "wh-3");

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void processAllFieldMappings_projectAndDescription_formatsJiraPayload() throws Exception {
        JiraFieldMappingDto project = JiraFieldMappingDto.builder()
                .jiraFieldId("project")
                .jiraFieldName("Project")
                .dataType(JiraDataType.STRING)
                .template("{{project.key}}")
                .required(true)
                .build();

        JiraFieldMappingDto description = JiraFieldMappingDto.builder()
                .jiraFieldId("description")
                .jiraFieldName("Description")
                .dataType(JiraDataType.STRING)
                .template("{{issue.summary}}")
                .required(true)
                .build();

        JsonNode payload = objectMapper.readTree("{\"project\":{\"key\":\"PRJ\"},\"issue\":{\"summary\":\"test\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(project, description), "wh-4", payload);

        assertThat(result).containsKeys("project", "description");
        @SuppressWarnings("unchecked")
        Map<String, Object> projectMap = (Map<String, Object>) result.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> descriptionMap = (Map<String, Object>) result.get("description");
        assertThat(projectMap.get("key")).isEqualTo("PRJ");
        assertThat(descriptionMap.get("type")).isEqualTo("doc");
    }

    @Test
    void processAllFieldMappings_assigneeInvalidFormat_resultsInNoField() throws Exception {
        JiraFieldMappingDto assignee = JiraFieldMappingDto.builder()
                .jiraFieldId("assignee")
                .jiraFieldName("Assignee")
                .dataType(JiraDataType.USER)
                .template("{{issue.assignee}}")
                .required(false)
                .build();

        JsonNode payload = objectMapper.readTree("{\"issue\":{\"assignee\":\"john.doe\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(assignee), "wh-5", payload);

        assertThat(result).isEmpty();
    }

    @Test
    void processAllFieldMappings_teamAndSprintMetadata_appliesSpecialCustomFormatting() throws Exception {
        JiraFieldMappingDto teamField = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_team")
                .jiraFieldName("Team")
                .dataType(JiraDataType.STRING)
                .template("{{team.id}}")
                .metadata(Map.of("fieldType", "team"))
                .build();

        JiraFieldMappingDto sprintField = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_sprint")
                .jiraFieldName("Sprint")
                .dataType(JiraDataType.ARRAY)
                .template("{{sprint.id}}")
                .metadata(Map.of("fieldType", "gh-sprint"))
                .build();

        JsonNode payload = objectMapper.readTree("{\"team\":{\"id\":\"15\"},\"sprint\":{\"id\":\"123\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(
                List.of(teamField, sprintField),
                "wh-6",
                payload
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> teamMap = (Map<String, Object>) result.get("customfield_team");
        assertThat(teamMap.get("id")).isEqualTo("15");
        assertThat(result.get("customfield_sprint")).isEqualTo(123L);
    }

    @Test
    void extractValueFromJsonPath_objectNodePrefersNameField() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"issue\":{\"priority\":{\"name\":\"High\"}}}");

        String value = resolver.extractValueFromJsonPath("issue.priority", payload);

        assertThat(value).isEqualTo("High");
    }

    @Test
    void convertToDataType_booleanAndArrayConversions_workAsExpected() {
        Object bool = resolver.convertToDataType("yes", JiraDataType.BOOLEAN, "flag", "wh-7");
        Object array = resolver.convertToDataType("a, b, c", JiraDataType.ARRAY, "labels", "wh-7");

        assertThat(bool).isEqualTo(Boolean.TRUE);
        assertThat(array).isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    void processAllFieldMappings_formatsProjectIssueTypeAndPriority() throws Exception {
        JiraFieldMappingDto project = JiraFieldMappingDto.builder()
                .jiraFieldId("project")
                .jiraFieldName("Project")
                .dataType(JiraDataType.STRING)
                .template("{{project.id}}")
                .build();
        JiraFieldMappingDto issueType = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .template("{{issue.type}}")
                .build();
        JiraFieldMappingDto priority = JiraFieldMappingDto.builder()
                .jiraFieldId("priority")
                .jiraFieldName("Priority")
                .dataType(JiraDataType.STRING)
                .template("{{issue.priority}}")
                .build();

        JsonNode payload = objectMapper.readTree("""
                {
                  "project":{"id":"10010"},
                  "issue":{"type":"Task","priority":"1"}
                }
                """);

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(project, issueType, priority), "wh-8", payload);

        assertThat(result.get("project")).isEqualTo(Map.of("id", "10010"));
        assertThat(result.get("issuetype")).isEqualTo(Map.of("name", "Task"));
        assertThat(result.get("priority")).isEqualTo(Map.of("id", "1"));
    }

    @Test
    void processAllFieldMappings_multiUser_convertsOnlyValidUsers() throws Exception {
        JiraFieldMappingDto multiUser = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_multi")
                .jiraFieldName("Approvers")
                .dataType(JiraDataType.MULTIUSER)
                .template("{{issue.approvers}}")
                .build();

        JsonNode payload = objectMapper.readTree(
                "{\"issue\":{\"approvers\":\"accountId:12345678901234567890, invalidUser\"}}"
        );

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(multiUser), "wh-9", payload);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("customfield_multi");
        assertThat(users).hasSize(1);
        assertThat(users.getFirst()).containsEntry("accountId", "accountId:12345678901234567890");
    }

    @Test
    void processFieldMapping_objectType_invalidJsonFallsBackToString() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_obj")
                .jiraFieldName("Object")
                .dataType(JiraDataType.OBJECT)
                .template("{{payload.json}}")
                .build();

        JsonNode payload = objectMapper.readTree("{\"payload\":{\"json\":\"not-json\"}}");

        Object result = resolver.processFieldMapping(mapping, payload, "wh-10");

        assertThat(result).isEqualTo("not-json");
    }

    @Test
    void processAllFieldMappings_sprintFromList_usesFirstItem() throws Exception {
        JiraFieldMappingDto sprintField = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_sprint")
                .jiraFieldName("Sprint")
                .dataType(JiraDataType.STRING)
                .template("{{sprint.id}}")
                .metadata(Map.of("fieldType", "sprint"))
                .build();

        JsonNode payload = objectMapper.readTree("{\"sprint\":{\"id\":\"abc\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(sprintField), "wh-11", payload);

        assertThat(result.get("customfield_sprint")).isEqualTo(Map.of("id", "abc"));
    }

    @Test
    void extractValueFromJsonPath_missingPath_returnsNull() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"issue\":{\"summary\":\"text\"}}");

        String value = resolver.extractValueFromJsonPath("issue.unknown.field", payload);

        assertThat(value).isNull();
    }

    @Test
    void processFieldMapping_templateMissingValue_usesDefault() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .template("{{missing.path}}")
                .defaultValue("Fallback")
                .required(false)
                .build();

        Object result = resolver.processFieldMapping(mapping, objectMapper.createObjectNode(), "wh-12");

        assertThat(result).isEqualTo("Fallback");
    }

    @Test
    void convertToDataType_nullDataType_returnsInputValue() {
        Object result = resolver.convertToDataType("value", null, "Field", "wh-13");

        assertThat(result).isEqualTo("value");
    }

    @Test
    void convertToDataType_booleanUnknownValue_fallsBackToFalse() {
        Object result = resolver.convertToDataType("unknown", JiraDataType.BOOLEAN, "Field", "wh-14");

        assertThat(result).isEqualTo(Boolean.FALSE);
    }

    @Test
    void convertToDataType_arrayWithoutComma_returnsSingleItemList() {
        Object result = resolver.convertToDataType("single", JiraDataType.ARRAY, "Field", "wh-15");

        assertThat(result).isEqualTo(List.of("single"));
    }

    @Test
    void convertToDataType_objectValidJson_returnsParsedObject() {
        Object result = resolver.convertToDataType("{\"k\":\"v\"}", JiraDataType.OBJECT, "Field", "wh-16");

        assertThat(result).isEqualTo(Map.of("k", "v"));
    }

    @Test
    void extractValueFromJsonPath_objectPrefersDisplayNameAndKey() throws Exception {
        JsonNode displayPayload = objectMapper.readTree("{\"obj\":{\"displayName\":\"Display\"}}");
        JsonNode keyPayload = objectMapper.readTree("{\"obj\":{\"key\":\"KEY-1\"}}");

        assertThat(resolver.extractValueFromJsonPath("obj", displayPayload)).isEqualTo("Display");
        assertThat(resolver.extractValueFromJsonPath("obj", keyPayload)).isEqualTo("KEY-1");
    }

    @Test
    void convertToDataType_numberBooleanAndUser_additionalBranches() {
        assertThat(resolver.convertToDataType("12.5", JiraDataType.NUMBER, "n", "wh-17")).isEqualTo(12.5d);
        assertThat(resolver.convertToDataType("abc", JiraDataType.NUMBER, "n", "wh-17")).isEqualTo("abc");
        assertThat(resolver.convertToDataType("0", JiraDataType.BOOLEAN, "b", "wh-17")).isEqualTo(Boolean.FALSE);

        Object validUser = resolver.convertToDataType(
                "12345678901234567890",
                JiraDataType.USER,
                "assignee",
                "wh-17");
        assertThat(validUser).isEqualTo(Map.of("accountId", "12345678901234567890"));
    }

    @Test
    void convertToDataType_multiUser_filtersInvalidAndEmptyItems() {
        Object result = resolver.convertToDataType(
                "accountId:12345678901234567890, invalid-user, , accountId:12345678901234567891",
                JiraDataType.MULTIUSER,
                "approvers",
                "wh-18");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result;
        assertThat(users).hasSize(2);
        assertThat(users.getFirst()).containsEntry("accountId", "accountId:12345678901234567890");
        assertThat(users.get(1)).containsEntry("accountId", "accountId:12345678901234567891");
    }

    @Test
    void processAllFieldMappings_projectIssueTypePriorityEmptyValues_areDropped() throws Exception {
        JiraFieldMappingDto project = JiraFieldMappingDto.builder()
                .jiraFieldId("project")
                .jiraFieldName("Project")
                .dataType(JiraDataType.STRING)
                .template("{{project.key}}")
                .build();
        JiraFieldMappingDto issueType = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .template("{{issue.type}}")
                .build();
        JiraFieldMappingDto priority = JiraFieldMappingDto.builder()
                .jiraFieldId("priority")
                .jiraFieldName("Priority")
                .dataType(JiraDataType.STRING)
                .template("{{issue.priority}}")
                .build();

        JsonNode payload = objectMapper.readTree("""
                {
                  "project":{"key":"   "},
                  "issue":{"type":" ","priority":""}
                }
                """);

        Map<String, Object> result = resolver.processAllFieldMappings(
                List.of(project, issueType, priority),
                "wh-19",
                payload);

        assertThat(result).isEmpty();
    }

    @Test
    void processAllFieldMappings_specialMetadataKeys_schemaTypeAndType_areSupported() throws Exception {
        JiraFieldMappingDto teamBySchemaType = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_team")
                .jiraFieldName("Team")
                .dataType(JiraDataType.STRING)
                .template("{{team.id}}")
                .metadata(Map.of("schemaType", "atlassian-team"))
                .build();

        JiraFieldMappingDto sprintByType = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_sprint")
                .jiraFieldName("Sprint")
                .dataType(JiraDataType.STRING)
                .template("{{sprint.id}}")
                .metadata(Map.of("type", "sprint"))
                .build();

        JsonNode payload = objectMapper.readTree("{\"team\":{\"id\":\"21\"},\"sprint\":{\"id\":\"77\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(
                List.of(teamBySchemaType, sprintByType),
                "wh-20",
                payload);

        assertThat(result.get("customfield_team")).isEqualTo(Map.of("id", "21"));
        assertThat(result.get("customfield_sprint")).isEqualTo(77L);
    }

    @Test
    void extractValueFromJsonPath_numberBooleanAndRawObjectBranches() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "issue": {
                "count": 5,
                "enabled": true,
                "obj": {"x":"y"}
                  }
                }
                """);

        assertThat(resolver.extractValueFromJsonPath("issue.count", payload)).isEqualTo("5");
        assertThat(resolver.extractValueFromJsonPath("issue.enabled", payload)).isEqualTo("true");
        assertThat(resolver.extractValueFromJsonPath("issue.obj", payload)).isEqualTo("{\"x\":\"y\"}");
    }

    @Test
    void processFieldMapping_requiredTemplateResolvesEmpty_throwsException() {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .template("{{missing.path}}")
                .required(true)
                .build();

        assertThatThrownBy(() -> resolver.processFieldMapping(mapping, objectMapper.createObjectNode(), "wh-21"))
                .isInstanceOf(FieldMappingProcessingException.class)
                .hasMessageContaining("template resulted in empty value");
    }

    @Test
    void processAllFieldMappings_teamAndSprintAlreadyMappedValues_preserved() throws Exception {
        JiraFieldMappingDto team = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_team")
                .jiraFieldName("Team")
                .dataType(JiraDataType.OBJECT)
                .template("{{team.obj}}")
                .metadata(Map.of("fieldType", "team"))
                .build();

        JiraFieldMappingDto sprint = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_sprint")
                .jiraFieldName("Sprint")
                .dataType(JiraDataType.OBJECT)
                .template("{{sprint.obj}}")
                .metadata(Map.of("fieldType", "gh-sprint"))
                .build();

        JsonNode payload = objectMapper.readTree("""
                {
                    "team": {"obj":{"id":"T-1"}},
                    "sprint": {"obj":{"id":"10"}}
                }
                """);

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(team, sprint), "wh-22", payload);

        assertThat(result.get("customfield_team")).isEqualTo(Map.of("id", "T-1"));
        assertThat(result.get("customfield_sprint")).isEqualTo(Map.of("id", "10"));
    }

    @Test
    void processAllFieldMappings_sprintTemplateWithCommaList_usesFirstListValue() throws Exception {
        JiraFieldMappingDto sprint = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_sprint")
                .jiraFieldName("Sprint")
                .dataType(JiraDataType.ARRAY)
                .template("{{sprint.ids}}")
                .metadata(Map.of("fieldType", "sprint"))
                .build();

        JsonNode payload = objectMapper.readTree("{\"sprint\":{\"ids\":\"200, 201\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(sprint), "wh-23", payload);

        assertThat(result.get("customfield_sprint")).isEqualTo(200L);
    }

    @Test
    void extractValueFromJsonPath_nullInputs_returnNull() {
        assertThat(resolver.extractValueFromJsonPath(null, objectMapper.createObjectNode())).isNull();
        assertThat(resolver.extractValueFromJsonPath(" ", objectMapper.createObjectNode())).isNull();
        assertThat(resolver.extractValueFromJsonPath("a.b", null)).isNull();
    }

    @Test
    void processFieldMapping_templateEvaluationError_usesDefault() {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .template("{{issue.summary}}")
                .defaultValue("fallback")
                .required(false)
                .build();

        Object result = resolver.processFieldMapping(mapping, null, "wh-24");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void processAllFieldMappings_assigneeWithObjectPayload_preservesAccountIdMap() throws Exception {
        JiraFieldMappingDto assignee = JiraFieldMappingDto.builder()
                .jiraFieldId("assignee")
                .jiraFieldName("Assignee")
                .dataType(JiraDataType.OBJECT)
                .template("{{issue.assignee}}")
                .build();

        JsonNode payload = objectMapper.readTree(
                "{\"issue\":{\"assignee\":{\"accountId\":\"12345678901234567890\"}}}");

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(assignee), "wh-25", payload);

        assertThat(result.get("assignee")).isEqualTo(Map.of("accountId", "12345678901234567890"));
    }

    @Test
    void processAllFieldMappings_unknownSpecialMetadataType_fallsBackToDefaultFieldHandling() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("customfield_unknown")
                .jiraFieldName("Unknown")
                .dataType(JiraDataType.STRING)
                .template("{{issue.value}}")
                .metadata(Map.of("fieldType", "custom-unknown"))
                .build();

        JsonNode payload = objectMapper.readTree("{\"issue\":{\"value\":\"abc\"}}");

        Map<String, Object> result = resolver.processAllFieldMappings(List.of(mapping), "wh-26", payload);

        assertThat(result.get("customfield_unknown")).isEqualTo("abc");
    }

    @Test
    void privateConverters_coverNullAndEmptyBranches() throws Exception {
        Object boolNull = invokePrivate("convertToBoolean", new Class[]{String.class}, new Object[]{null});
        @SuppressWarnings("unchecked")
        List<String> arrayFromNull = (List<String>) invokePrivate(
                "convertToArray",
                new Class[]{String.class},
                new Object[]{null});
        Object userFromEmpty = invokePrivate(
                "convertToJiraUserFormat",
                new Class[]{Object.class, String.class},
                new Object[]{"   ", "wh-27"});
        Object userFromMap = invokePrivate(
                "convertToJiraUserFormat",
                new Class[]{Object.class, String.class},
                new Object[]{Map.of("accountId", "abc1234567890123456789"), "wh-27"});

        assertThat(boolNull).isNull();
        assertThat(arrayFromNull).isEmpty();
        assertThat(userFromEmpty).isNull();
        assertThat(userFromMap).isEqualTo(Map.of("accountId", "abc1234567890123456789"));
    }

    @Test
    void formatSpecialCustomFieldIfAny_nonStringType_returnsNull() throws Exception {
        Object result = invokePrivate(
                "formatSpecialCustomFieldIfAny",
                new Class[]{Object.class, Map.class, String.class},
                new Object[]{"12", Map.of("fieldType", 123), "wh-28"});

        assertThat(result).isNull();
    }

    @Test
    void privateJiraConverters_coverNullEmptyAndFallbackBranches() throws Exception {
        Object sprintFromNull = invokePrivate(
                "convertToJiraSprintFormat",
                new Class[]{Object.class, String.class},
                new Object[]{null, "wh-29"});
        Object sprintFromEmptyList = invokePrivate(
                "convertToJiraSprintFormat",
                new Class[]{Object.class, String.class},
                new Object[]{List.of(), "wh-29"});
        Object sprintFromBlank = invokePrivate(
                "convertToJiraSprintFormat",
                new Class[]{Object.class, String.class},
                new Object[]{"   ", "wh-29"});

        Object teamFromNull = invokePrivate(
                "convertToJiraTeamFormat",
                new Class[]{Object.class, String.class},
                new Object[]{null, "wh-29"});
        Object teamFromBlank = invokePrivate(
                "convertToJiraTeamFormat",
                new Class[]{Object.class, String.class},
                new Object[]{"   ", "wh-29"});

        Object projectFromNull = invokePrivate(
                "convertToJiraProjectFormat",
                new Class[]{Object.class, String.class},
                new Object[]{null, "wh-29"});
        Object issueTypeFromBlank = invokePrivate(
                "convertToJiraIssueTypeFormat",
                new Class[]{Object.class, String.class},
                new Object[]{"", "wh-29"});
        Object priorityFromBlank = invokePrivate(
                "convertToJiraPriorityFormat",
                new Class[]{Object.class, String.class},
                new Object[]{"", "wh-29"});
        Object descriptionFromBlank = invokePrivate(
                "convertToJiraDescriptionFormat",
                new Class[]{Object.class, String.class},
                new Object[]{" ", "wh-29"});

        assertThat(sprintFromNull).isNull();
        assertThat(sprintFromEmptyList).isEqualTo(Map.of("id", "[]"));
        assertThat(sprintFromBlank).isNull();
        assertThat(teamFromNull).isNull();
        assertThat(teamFromBlank).isNull();
        assertThat(projectFromNull).isNull();
        assertThat(issueTypeFromBlank).isNull();
        assertThat(priorityFromBlank).isNull();
        assertThat(descriptionFromBlank).isNull();
    }

    @Test
    void privateBooleanConverter_coversAllTokenVariants() throws Exception {
        Object one = invokePrivate("convertToBoolean", new Class[]{String.class}, new Object[]{"1"});
        Object yes = invokePrivate("convertToBoolean", new Class[]{String.class}, new Object[]{"YES"});
        Object no = invokePrivate("convertToBoolean", new Class[]{String.class}, new Object[]{"no"});
        Object trueToken = invokePrivate("convertToBoolean", new Class[]{String.class}, new Object[]{"true"});
        Object falseToken = invokePrivate("convertToBoolean", new Class[]{String.class}, new Object[]{"false"});

        assertThat(one).isEqualTo(Boolean.TRUE);
        assertThat(yes).isEqualTo(Boolean.TRUE);
        assertThat(no).isEqualTo(Boolean.FALSE);
        assertThat(trueToken).isEqualTo(Boolean.TRUE);
        assertThat(falseToken).isEqualTo(Boolean.FALSE);
    }

    @Test
    void extractValueFromJsonPath_wildcardArrayOfObjects_joinsResolvedValues() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "serials": [
                    {"filingNumberDisplay":"2025-0000430"},
                    {"filingNumberDisplay":"2025-0000431"}
                  ]
                }
                """);

        String value = resolver.extractValueFromJsonPath("serials[].filingNumberDisplay", payload);

        assertThat(value).isEqualTo("2025-0000430, 2025-0000431");
    }

    @Test
    void extractValueFromJsonPath_wildcardArrayOfPrimitives_joinsValues() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "tags": ["alpha", "beta"]
                }
                """);

        String value = resolver.extractValueFromJsonPath("tags[]", payload);

        assertThat(value).isEqualTo("alpha, beta");
    }

    @Test
    void extractValueFromJsonPath_wildcardEmptyArray_returnsEmptyString() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "serials": []
                }
                """);

        String value = resolver.extractValueFromJsonPath("serials[]", payload);

        assertThat(value).isEqualTo("");
    }

    @Test
    void extractValueFromJsonPath_legacyIndexedPath_remainsSupported() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "items": [
                    {"value":"first"}
                  ]
                }
                """);

        String dotIndexed = resolver.extractValueFromJsonPath("items.0.value", payload);
        String bracketIndexed = resolver.extractValueFromJsonPath("items[0].value", payload);

        assertThat(dotIndexed).isEqualTo("first");
        assertThat(bracketIndexed).isEqualTo("first");
    }

    @Test
    void processFieldMapping_wildcardWithNullItems_skipsNulls() throws Exception {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .template("Issue {{serials[].filingNumberDisplay}}")
                .required(false)
                .build();

        JsonNode payload = objectMapper.readTree("""
                {
                  "serials": [
                    {"filingNumberDisplay":"A"},
                    {},
                    {"filingNumberDisplay":"B"}
                  ]
                }
                """);

        Object result = resolver.processFieldMapping(mapping, payload, "wh-wildcard-null");

        assertThat(result).isEqualTo("Issue A, B");
    }

    private Object invokePrivate(final String methodName, final Class<?>[] parameterTypes, final Object[] args)
            throws Exception {
        Method method = JiraFieldMappingResolver.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(resolver, args);
    }
}
