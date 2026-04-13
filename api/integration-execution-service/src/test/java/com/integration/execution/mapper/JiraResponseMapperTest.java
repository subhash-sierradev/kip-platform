package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JiraResponseMapperTest {

    private JiraResponseMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new JiraResponseMapper(objectMapper);
    }

    // -----------------------------------------------------------------------
    // mapProjects
    // -----------------------------------------------------------------------

    @Test
    void mapProjects_validValuesArray_returnsSortedByName() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        values.addObject().put("name", "Zebra Project").put("key", "ZEB");
        values.addObject().put("name", "Alpha Project").put("key", "ALP");

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alpha Project");
        assertThat(result.get(1).getName()).isEqualTo("Zebra Project");
    }

    @Test
    void mapProjects_leadIsObject_extractsLeadDisplayName() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        ObjectNode project = values.addObject();
        project.put("name", "My Project");
        project.putObject("lead").put("displayName", "Jane Doe");

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLead()).isEqualTo("Jane Doe");
    }

    @Test
    void mapProjects_leadIsNotObject_projectReturnedUnchanged() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        ObjectNode project = values.addObject();
        project.put("name", "My Project");
        project.put("lead", "Plain Lead String");

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLead()).isEqualTo("Plain Lead String");
    }

    @Test
    void mapProjects_missingValuesNode_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapProjects_valuesNodeNotArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("values", "not-an-array");

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapProjects_nullNamesInProjects_sortedWithNullsLast() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        values.addObject().put("name", "Beta Project");
        values.addObject().putNull("name");

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Beta Project");
        assertThat(result.get(1).getName()).isNull();
    }

    @Test
    void mapProjects_emptyValuesArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("values");

        List<JiraProjectResponse> result = mapper.mapProjects(root);

        assertThat(result).isEmpty();
    }


    // -----------------------------------------------------------------------
    // mapUsers
    // -----------------------------------------------------------------------

    @Test
    void mapUsers_validArray_returnsSortedByDisplayName() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("displayName", "Zebra User").put("accountId", "z1");
        root.addObject().put("displayName", "Alpha User").put("accountId", "a1");

        List<JiraUserResponse> result = mapper.mapUsers(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDisplayName()).isEqualTo("Alpha User");
        assertThat(result.get(1).getDisplayName()).isEqualTo("Zebra User");
    }

    @Test
    void mapUsers_notArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("key", "value");

        List<JiraUserResponse> result = mapper.mapUsers(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapUsers_emptyArray_returnsEmptyList() {
        ArrayNode root = objectMapper.createArrayNode();

        List<JiraUserResponse> result = mapper.mapUsers(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapUsers_nullDisplayName_sortedWithNullsLast() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("displayName", "Charlie");
        root.addObject().putNull("displayName");

        List<JiraUserResponse> result = mapper.mapUsers(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDisplayName()).isEqualTo("Charlie");
        assertThat(result.get(1).getDisplayName()).isNull();
    }

    // -----------------------------------------------------------------------
    // mapIssueTypes
    // -----------------------------------------------------------------------

    @Test
    void mapIssueTypes_validArray_returnsSortedByName() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("name", "Task").put("id", "10001");
        root.addObject().put("name", "Bug").put("id", "10002");

        List<JiraIssueTypeResponse> result = mapper.mapIssueTypes(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Bug");
        assertThat(result.get(1).getName()).isEqualTo("Task");
    }

    @Test
    void mapIssueTypes_notArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();

        List<JiraIssueTypeResponse> result = mapper.mapIssueTypes(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapIssueTypes_emptyArray_returnsEmptyList() {
        ArrayNode root = objectMapper.createArrayNode();

        List<JiraIssueTypeResponse> result = mapper.mapIssueTypes(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapIssueTypes_nullNames_sortedWithNullsLast() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("name", "Story").put("id", "10003");
        root.addObject().putNull("name");

        List<JiraIssueTypeResponse> result = mapper.mapIssueTypes(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Story");
        assertThat(result.get(1).getName()).isNull();
    }

    // -----------------------------------------------------------------------
    // mapFields
    // -----------------------------------------------------------------------

    @Test
    void mapFields_validArrayWithSchema_setsSchemaAndSchemaDetails() {
        ArrayNode root = objectMapper.createArrayNode();
        ObjectNode field = root.addObject();
        field.put("id", "summary");
        field.put("name", "Summary");
        field.putObject("schema").put("type", "string").put("system", "summary");

        List<JiraFieldResponse> result = mapper.mapFields(root);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("summary");
        assertThat(result.get(0).getSchema()).isEqualTo("string");
        assertThat(result.get(0).getSchemaDetails()).isNotNull();
        assertThat(result.get(0).getSchemaDetails().getType()).isEqualTo("string");
        assertThat(result.get(0).getSchemaDetails().getSystem()).isEqualTo("summary");
    }

    @Test
    void mapFields_validArrayNoSchema_schemaRemainsNull() {
        ArrayNode root = objectMapper.createArrayNode();
        ObjectNode field = root.addObject();
        field.put("id", "customfield_001");
        field.put("name", "Custom Field");

        List<JiraFieldResponse> result = mapper.mapFields(root);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSchema()).isNull();
        assertThat(result.get(0).getSchemaDetails()).isNull();
    }

    @Test
    void mapFields_notArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();

        List<JiraFieldResponse> result = mapper.mapFields(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapFields_emptyArray_returnsEmptyList() {
        ArrayNode root = objectMapper.createArrayNode();

        List<JiraFieldResponse> result = mapper.mapFields(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapFields_multipleFields_allMapped() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("id", "summary").put("name", "Summary");
        root.addObject().put("id", "description").put("name", "Description");

        List<JiraFieldResponse> result = mapper.mapFields(root);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(JiraFieldResponse::getId).containsExactly("summary", "description");
    }

    // -----------------------------------------------------------------------
    // mapCreateMetaFields
    // -----------------------------------------------------------------------

    @Test
    void mapCreateMetaFields_nullRootNode_returnsEmptyList() {
        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(null, "10001");

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_jsonNullNode_returnsEmptyList() {
        JsonNode nullNode = objectMapper.nullNode();

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(nullNode, "10001");

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_missingProjectsNode_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, "10001");

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_emptyProjectsArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("projects");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, "10001");

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_nullIssueTypeId_includesAllIssueTypes() {
        ObjectNode root = buildCreateMetaNode("10001", "summary", "string");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("summary");
    }

    @Test
    void mapCreateMetaFields_blankIssueTypeId_treatedAsNull() {
        ObjectNode root = buildCreateMetaNode("10001", "summary", "string");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, "   ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("summary");
    }

    @Test
    void mapCreateMetaFields_matchingIssueTypeId_returnsOnlyMatchingFields() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");

        ObjectNode matchingType = issueTypes.addObject();
        matchingType.put("id", "10001");
        ObjectNode matchingFields = matchingType.putObject("fields");
        matchingFields.putObject("summary").put("name", "Summary");

        ObjectNode nonMatchingType = issueTypes.addObject();
        nonMatchingType.put("id", "99999");
        ObjectNode nonMatchingFields = nonMatchingType.putObject("fields");
        nonMatchingFields.putObject("description").put("name", "Description");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, "10001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("summary");
    }

    @Test
    void mapCreateMetaFields_nonMatchingIssueTypeId_returnsEmptyList() {
        ObjectNode root = buildCreateMetaNode("10001", "summary", "string");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, "99999");

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_duplicateFieldAcrossIssueTypes_deduplicatedFirstWins() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");

        ObjectNode type1 = issueTypes.addObject();
        type1.put("id", "10001");
        type1.putObject("fields").putObject("summary").put("name", "Summary v1");

        ObjectNode type2 = issueTypes.addObject();
        type2.put("id", "10002");
        type2.putObject("fields").putObject("summary").put("name", "Summary v2");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Summary v1");
    }

    @Test
    void mapCreateMetaFields_fieldWithMissingId_usesMapKey() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        ObjectNode issueType = issueTypes.addObject();
        issueType.put("id", "10001");
        ObjectNode fields = issueType.putObject("fields");
        ObjectNode fieldNode = fields.putObject("customfield_999");
        fieldNode.put("name", "My Custom Field");
        // no "id" property on fieldNode

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("customfield_999");
    }

    @Test
    void mapCreateMetaFields_fieldWithMissingKey_usesMapKey() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        ObjectNode issueType = issueTypes.addObject();
        issueType.put("id", "10001");
        ObjectNode fields = issueType.putObject("fields");
        ObjectNode fieldNode = fields.putObject("customfield_888");
        fieldNode.put("id", "customfield_888");
        fieldNode.put("name", "Another Field");
        // no "key" property on fieldNode

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKey()).isEqualTo("customfield_888");
    }

    @Test
    void mapCreateMetaFields_fieldWithSchemaNode_setsSchemaDetails() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        ObjectNode issueType = issueTypes.addObject();
        issueType.put("id", "10001");
        ObjectNode fields = issueType.putObject("fields");
        ObjectNode fieldNode = fields.putObject("priority");
        fieldNode.put("id", "priority");
        fieldNode.put("name", "Priority");
        fieldNode.putObject("schema").put("type", "priority").put("system", "priority");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSchema()).isEqualTo("priority");
        assertThat(result.get(0).getSchemaDetails()).isNotNull();
        assertThat(result.get(0).getSchemaDetails().getSystem()).isEqualTo("priority");
    }

    @Test
    void mapCreateMetaFields_issueTypeWithNoFieldsNode_skipped() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        // issue type has no "fields" key at all
        issueTypes.addObject().put("id", "10001");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_issueTypeWithEmptyFieldsObject_skipped() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        ObjectNode issueType = issueTypes.addObject();
        issueType.put("id", "10001");
        issueType.putObject("fields"); // empty object

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_projectWithNoIssueTypesNode_skipped() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        projects.addObject(); // project with no "issuetypes" key

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).isEmpty();
    }

    @Test
    void mapCreateMetaFields_projectWithEmptyIssueTypesArray_skipped() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        project.putArray("issuetypes"); // empty array

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, null);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // hasValidData
    // -----------------------------------------------------------------------

    @Test
    void hasValidData_null_returnsFalse() {
        assertThat(mapper.hasValidData(null)).isFalse();
    }

    @Test
    void hasValidData_nullNode_returnsFalse() {
        assertThat(mapper.hasValidData(objectMapper.nullNode())).isFalse();
    }

    @Test
    void hasValidData_totalGreaterThanZero_returnsTrue() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("total", 5);

        assertThat(mapper.hasValidData(root)).isTrue();
    }

    @Test
    void hasValidData_totalEqualsZero_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("total", 0);

        assertThat(mapper.hasValidData(root)).isFalse();
    }

    @Test
    void hasValidData_noTotalNonEmptyValuesArray_returnsTrue() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("values").addObject().put("id", "1");

        assertThat(mapper.hasValidData(root)).isTrue();
    }

    @Test
    void hasValidData_noTotalEmptyValuesArray_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("values");

        assertThat(mapper.hasValidData(root)).isFalse();
    }

    @Test
    void hasValidData_noTotalNoValuesNonEmptyRootArray_returnsTrue() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("id", "1");

        assertThat(mapper.hasValidData(root)).isTrue();
    }

    @Test
    void hasValidData_noTotalNoValuesEmptyRootArray_returnsFalse() {
        ArrayNode root = objectMapper.createArrayNode();

        assertThat(mapper.hasValidData(root)).isFalse();
    }

    @Test
    void hasValidData_noTotalNoValuesObjectNode_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("someKey", "someValue");

        assertThat(mapper.hasValidData(root)).isFalse();
    }


    // -----------------------------------------------------------------------
    // hasValidProjectMetaFieldsData
    // -----------------------------------------------------------------------

    @Test
    void hasValidProjectMetaFieldsData_null_returnsFalse() {
        assertThat(mapper.hasValidProjectMetaFieldsData(null)).isFalse();
        assertThat(mapper.hasValidProjectMetaFieldsData(objectMapper.nullNode())).isFalse();
    }

    @Test
    void hasValidProjectMetaFieldsData_noProjectsNode_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isFalse();
    }

    @Test
    void hasValidProjectMetaFieldsData_emptyProjectsArray_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("projects");

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isFalse();
    }

    @Test
    void hasValidProjectMetaFieldsData_projectWithEmptyIssueTypes_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        project.putArray("issuetypes");

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isFalse();
    }

    @Test
    void hasValidProjectMetaFieldsData_issueTypeWithEmptyFieldsObject_returnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        issueTypes.addObject().putObject("fields"); // empty fields object

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isFalse();
    }

    @Test
    void hasValidProjectMetaFieldsData_validProjectsWithFields_returnsTrue() {
        ObjectNode root = buildCreateMetaNode("10001", "summary", "string");

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isTrue();
    }

    @Test
    void hasValidProjectMetaFieldsData_projectWithNoIssueTypesKey_skippedAndReturnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        projects.addObject(); // no "issuetypes" key at all

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isFalse();
    }

    @Test
    void hasValidProjectMetaFieldsData_issueTypeWithNoFieldsKey_skippedAndReturnsFalse() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        issueTypes.addObject().put("id", "10001"); // no "fields" key

        assertThat(mapper.hasValidProjectMetaFieldsData(root)).isFalse();
    }

    // -----------------------------------------------------------------------
    // mapTeams
    // -----------------------------------------------------------------------

    @Test
    void mapTeams_null_returnsEmptyList() {
        assertThat(mapper.mapTeams(null)).isEmpty();
    }

    @Test
    void mapTeams_nullNode_returnsEmptyList() {
        assertThat(mapper.mapTeams(objectMapper.nullNode())).isEmpty();
    }

    @Test
    void mapTeams_valuesArray_returnsSortedTeams() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        values.addObject().put("name", "Zeta Team").put("id", "z1");
        values.addObject().put("name", "Alpha Team").put("id", "a1");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alpha Team");
        assertThat(result.get(1).getName()).isEqualTo("Zeta Team");
    }

    @Test
    void mapTeams_dataArray_returnsSortedTeams() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode data = root.putArray("data");
        data.addObject().put("name", "Zeta Team").put("id", "z1");
        data.addObject().put("name", "Alpha Team").put("id", "a1");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alpha Team");
    }

    @Test
    void mapTeams_resultsArray_returnsSortedTeams() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode results = root.putArray("results");
        results.addObject().put("name", "Beta Team").put("id", "b1");
        results.addObject().put("name", "Alpha Team").put("id", "a1");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alpha Team");
    }

    @Test
    void mapTeams_noRecognizedKey_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("unknown", "value");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapTeams_emptyValuesArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("values");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapTeams_nullTeamNames_sortedWithNullsLast() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        values.addObject().put("name", "Delta Team");
        values.addObject().putNull("name");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Delta Team");
        assertThat(result.get(1).getName()).isNull();
    }

    @Test
    void mapTeams_valuesKeyPresentButNotArrayValue_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("values", "not-an-array");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // mapSprints
    // -----------------------------------------------------------------------

    @Test
    void mapSprints_null_returnsEmptyList() {
        assertThat(mapper.mapSprints(null)).isEmpty();
    }

    @Test
    void mapSprints_nullNode_returnsEmptyList() {
        assertThat(mapper.mapSprints(objectMapper.nullNode())).isEmpty();
    }

    @Test
    void mapSprints_valuesArray_returnsSortedSprints() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        values.addObject().put("name", "Sprint 3").put("id", 3L);
        values.addObject().put("name", "Sprint 1").put("id", 1L);

        List<JiraSprintResponse> result = mapper.mapSprints(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Sprint 1");
        assertThat(result.get(1).getName()).isEqualTo("Sprint 3");
    }

    @Test
    void mapSprints_rootNodeIsDirectArray_returnsSortedSprints() {
        ArrayNode root = objectMapper.createArrayNode();
        root.addObject().put("name", "Sprint Z").put("id", 9L);
        root.addObject().put("name", "Sprint A").put("id", 1L);

        List<JiraSprintResponse> result = mapper.mapSprints(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Sprint A");
    }

    @Test
    void mapSprints_noValuesAndNotArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("someKey", "someValue");

        List<JiraSprintResponse> result = mapper.mapSprints(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapSprints_emptyValuesArray_returnsEmptyList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("values");

        List<JiraSprintResponse> result = mapper.mapSprints(root);

        assertThat(result).isEmpty();
    }

    @Test
    void mapSprints_nullSprintNames_sortedWithNullsLast() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode values = root.putArray("values");
        values.addObject().put("name", "Active Sprint").put("id", 1L);
        values.addObject().putNull("name");

        List<JiraSprintResponse> result = mapper.mapSprints(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Active Sprint");
        assertThat(result.get(1).getName()).isNull();
    }

    @Test
    void mapSprints_emptyDirectArray_returnsEmptyList() {
        ArrayNode root = objectMapper.createArrayNode();

        List<JiraSprintResponse> result = mapper.mapSprints(root);

        assertThat(result).isEmpty();
    }


    @Test
    void mapCreateMetaFields_multipleProjects_collectsFromAll() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");

        // Project 1
        ObjectNode project1 = projects.addObject();
        ArrayNode issueTypes1 = project1.putArray("issuetypes");
        ObjectNode type1 = issueTypes1.addObject();
        type1.put("id", "10001");
        type1.putObject("fields").putObject("summary").put("name", "Summary");

        // Project 2 - different field
        ObjectNode project2 = projects.addObject();
        ArrayNode issueTypes2 = project2.putArray("issuetypes");
        ObjectNode type2 = issueTypes2.addObject();
        type2.put("id", "10001");
        type2.putObject("fields").putObject("description").put("name", "Description");

        List<JiraFieldResponse> result = mapper.mapCreateMetaFields(root, "10001");

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(JiraFieldResponse::getId))
                .containsExactlyInAnyOrder("summary", "description");
    }

    @Test
    void mapFields_fieldWithNoSchemaNode_setsNullSchema() {
        ArrayNode root = objectMapper.createArrayNode();
        ObjectNode field = root.addObject();
        field.put("id", "f1");
        field.put("name", "Field1");
        // no schema node

        List<JiraFieldResponse> result = mapper.mapFields(root);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSchema()).isNull();
        assertThat(result.get(0).getSchemaDetails()).isNull();
    }

    @Test
    void mapParentIssues_issueWithBlankKey_filtered() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode issues = root.putArray("issues");

        // Issue with blank key - should be filtered out
        ObjectNode issue1 = issues.addObject();
        issue1.put("key", "   ");
        issue1.putObject("fields").put("summary", "Some issue");

        // Issue with valid key
        ObjectNode issue2 = issues.addObject();
        issue2.put("key", "TEST-1");
        issue2.putObject("fields").put("summary", "Valid issue");

        List<?> result = mapper.mapParentIssues(root);

        assertThat(result).hasSize(1);
    }

    @Test
    void mapTeams_valuesHasNonArrayValue_treatedAsNoArray() {
        ObjectNode root = objectMapper.createObjectNode();
        // values key exists but not as array - this exercises the "values but not array" path
        root.put("values", "not-an-array");
        root.put("data", "not-array-either");

        List<JiraTeamResponse> result = mapper.mapTeams(root);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a minimal valid createMeta JSON tree with one project,
     * one issue type, and one field.
     */
    private ObjectNode buildCreateMetaNode(
            final String issueTypeId,
            final String fieldId,
            final String schemaType) {

        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        ObjectNode issueType = issueTypes.addObject();
        issueType.put("id", issueTypeId);
        ObjectNode fields = issueType.putObject("fields");
        ObjectNode field = fields.putObject(fieldId);
        field.put("id", fieldId);
        field.put("name", fieldId);
        if (schemaType != null) {
            field.putObject("schema").put("type", schemaType);
        }
        return root;
    }
}

