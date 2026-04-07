package com.integration.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.client.JiraApiClient;
import com.integration.execution.config.properties.JiraApiProperties;
import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import com.integration.execution.mapper.JiraResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIntegrationService")
class JiraIntegrationServiceTest {

    private static final String SECRET = "vault-secret-1";
    private static final String PROJECT = "TESTPROJ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private JiraApiClient jiraApiClient;

    @Mock
    private JiraResponseMapper responseMapper;

    private JiraIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new JiraIntegrationService(
                jiraApiClient, responseMapper, new JiraApiProperties(), OBJECT_MAPPER);
    }

    // -----------------------------------------------------------------------
    // getProjectsBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getProjectsBySecretName_hasValidData_returnsMappedProjects() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        List<JiraProjectResponse> expected =
                List.of(JiraProjectResponse.builder().name("P1").build());
        when(jiraApiClient.searchProjects(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapProjects(node)).thenReturn(expected);

        assertThat(service.getProjectsBySecretName(SECRET)).isEqualTo(expected);
    }

    @Test
    void getProjectsBySecretName_noValidData_returnsEmptyList() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.searchProjects(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(false);

        assertThat(service.getProjectsBySecretName(SECRET)).isEmpty();
    }

    @Test
    void getProjectsBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.searchProjects(SECRET)).thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() -> service.getProjectsBySecretName(SECRET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching projects");
    }

    // -----------------------------------------------------------------------
    // getProjectUsersBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getProjectUsersBySecretName_hasValidData_returnsMappedUsers() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        List<JiraUserResponse> expected =
                List.of(JiraUserResponse.builder().displayName("Alice").build());
        when(jiraApiClient.getAssignableUsers(SECRET, PROJECT)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapUsers(node)).thenReturn(expected);

        assertThat(service.getProjectUsersBySecretName(SECRET, PROJECT)).isEqualTo(expected);
    }

    @Test
    void getProjectUsersBySecretName_noValidData_returnsEmptyList() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getAssignableUsers(SECRET, PROJECT)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(false);

        assertThat(service.getProjectUsersBySecretName(SECRET, PROJECT)).isEmpty();
    }

    @Test
    void getProjectUsersBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.getAssignableUsers(SECRET, PROJECT))
                .thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> service.getProjectUsersBySecretName(SECRET, PROJECT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching project users");
    }

    // -----------------------------------------------------------------------
    // getProjectIssueTypesBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getProjectIssueTypesBySecretName_hasValidData_returnsMappedIssueTypes() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        List<JiraIssueTypeResponse> expected =
                List.of(JiraIssueTypeResponse.builder().name("Bug").build());
        when(jiraApiClient.getProjectStatuses(PROJECT, SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapIssueTypes(node)).thenReturn(expected);

        assertThat(service.getProjectIssueTypesBySecretName(SECRET, PROJECT)).isEqualTo(expected);
    }

    @Test
    void getProjectIssueTypesBySecretName_noValidData_returnsEmptyList() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getProjectStatuses(PROJECT, SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(false);

        assertThat(service.getProjectIssueTypesBySecretName(SECRET, PROJECT)).isEmpty();
    }

    @Test
    void getProjectIssueTypesBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.getProjectStatuses(PROJECT, SECRET))
                .thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> service.getProjectIssueTypesBySecretName(SECRET, PROJECT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching issue types");
    }

    @Test
    void getParentIssuesBySecretName_withQuery_passesQueryToClient() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        List<JiraIssueReferenceResponse> expected =
                List.of(JiraIssueReferenceResponse.builder().key("PRJ-101").build());
        when(jiraApiClient.searchParentIssues(SECRET, PROJECT, "PRJ-10", 0, 20)).thenReturn(node);
        when(responseMapper.mapParentIssues(node)).thenReturn(expected);

        List<JiraIssueReferenceResponse> actual =
                service.getParentIssuesBySecretName(SECRET, PROJECT, "PRJ-10", 0, 20);

        assertThat(actual).isEqualTo(expected);
    }

    // -----------------------------------------------------------------------
    // getFieldsBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getFieldsBySecretName_noValidData_returnsEmptyList() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getFields(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(false);

        assertThat(service.getFieldsBySecretName(SECRET)).isEmpty();
    }

    @Test
    void getFieldsBySecretName_filtersOutNonNavigableFields() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        JiraFieldResponse navigable = JiraFieldResponse.builder()
                .id("f1").name("Summary").navigable(true).custom(false).build();
        JiraFieldResponse hidden = JiraFieldResponse.builder()
                .id("f2").name("Hidden").navigable(false).custom(false).build();
        when(jiraApiClient.getFields(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapFields(node)).thenReturn(List.of(navigable, hidden));

        List<JiraFieldResponse> result = service.getFieldsBySecretName(SECRET);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("f1");
    }

    @Test
    void getFieldsBySecretName_systemFieldsSortedBeforeCustomFields() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        JiraFieldResponse custom = JiraFieldResponse.builder()
                .id("c1").name("Alpha Custom").navigable(true).custom(true).build();
        JiraFieldResponse system = JiraFieldResponse.builder()
                .id("s1").name("Zebra System").navigable(true).custom(false).build();
        when(jiraApiClient.getFields(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapFields(node)).thenReturn(List.of(custom, system));

        List<JiraFieldResponse> result = service.getFieldsBySecretName(SECRET);

        assertThat(result.get(0).getId()).isEqualTo("s1");
        assertThat(result.get(1).getId()).isEqualTo("c1");
    }

    @Test
    void getFieldsBySecretName_nullNameTreatedAsEmptyForSorting() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        JiraFieldResponse nullName = JiraFieldResponse.builder()
                .id("n1").name(null).navigable(true).custom(false).build();
        JiraFieldResponse named = JiraFieldResponse.builder()
                .id("n2").name("Beta").navigable(true).custom(false).build();
        when(jiraApiClient.getFields(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapFields(node)).thenReturn(List.of(named, nullName));

        List<JiraFieldResponse> result = service.getFieldsBySecretName(SECRET);

        assertThat(result).hasSize(2);
        // null name treated as "" → sorts before "Beta"
        assertThat(result.get(0).getId()).isEqualTo("n1");
    }

    @Test
    void getFieldsBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.getFields(SECRET)).thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() -> service.getFieldsBySecretName(SECRET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching fields");
    }

    // -----------------------------------------------------------------------
    // getProjectMetaFieldsBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getProjectMetaFieldsBySecretName_noValidData_returnsEmptyList() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getProjectMetaFields(PROJECT, SECRET)).thenReturn(node);
        when(responseMapper.hasValidProjectMetaFieldsData(node)).thenReturn(false);

        assertThat(service.getProjectMetaFieldsBySecretName(SECRET, PROJECT, null)).isEmpty();
    }

    @Test
    void getProjectMetaFieldsBySecretName_hasValidData_systemFieldSortedFirst() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        JiraFieldResponse custom = JiraFieldResponse.builder()
                .id("c1").name("Alpha").custom(true).build();
        JiraFieldResponse system = JiraFieldResponse.builder()
                .id("s1").name("Bravo").custom(false).build();
        when(jiraApiClient.getProjectMetaFields(PROJECT, SECRET)).thenReturn(node);
        when(responseMapper.hasValidProjectMetaFieldsData(node)).thenReturn(true);
        when(responseMapper.mapCreateMetaFields(node, "10001"))
                .thenReturn(List.of(custom, system));

        List<JiraFieldResponse> result =
                service.getProjectMetaFieldsBySecretName(SECRET, PROJECT, "10001");

        assertThat(result.get(0).getId()).isEqualTo("s1");
        assertThat(result.get(1).getId()).isEqualTo("c1");
    }

    @Test
    void getProjectMetaFieldsBySecretName_nullIssueTypeId_passesNullToMapper() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getProjectMetaFields(PROJECT, SECRET)).thenReturn(node);
        when(responseMapper.hasValidProjectMetaFieldsData(node)).thenReturn(true);
        when(responseMapper.mapCreateMetaFields(node, null)).thenReturn(List.of());

        service.getProjectMetaFieldsBySecretName(SECRET, PROJECT, null);

        verify(responseMapper).mapCreateMetaFields(node, null);
    }

    @Test
    void getProjectMetaFieldsBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.getProjectMetaFields(PROJECT, SECRET))
                .thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() ->
                service.getProjectMetaFieldsBySecretName(SECRET, PROJECT, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching project meta fields");
    }

    // -----------------------------------------------------------------------
    // getFieldDetailsBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getFieldDetailsBySecretName_fieldFound_noProjectKey_returnsBasicDetail() {
        stubFieldsChain(List.of(navigableField("summary", "Summary", false)));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "summary", null, null);

        assertThat(result.getId()).isEqualTo("summary");
        assertThat(result.getName()).isEqualTo("Summary");
    }

    @Test
    void getFieldDetailsBySecretName_fieldNotFound_throwsRuntimeException() {
        stubFieldsChain(List.of());

        assertThatThrownBy(() ->
                service.getFieldDetailsBySecretName(SECRET, "unknown", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching field details");
    }

    @Test
    void getFieldDetailsBySecretName_withProjectKey_populatesRequiredConstraint() {
        stubFieldsChain(List.of(navigableField("priority", "Priority", false)));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(buildConstraintMeta("priority", true, null, null));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "priority", PROJECT, null);

        assertThat(result.getRequired()).isTrue();
    }

    @Test
    void getFieldDetailsBySecretName_constraintMetaHasAllowedValues_populatesAllowedValues() {
        stubFieldsChain(List.of(navigableField("priority", "Priority", false)));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(buildConstraintMeta("priority", false, "High", "Low"));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "priority", PROJECT, null);

        assertThat(result.getAllowedValues()).hasSize(2);
    }

    @Test
    void getFieldDetailsBySecretName_withMatchingIssueTypeId_populatesConstraint() {
        stubFieldsChain(List.of(navigableField("priority", "Priority", false)));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(buildConstraintMetaWithIssueType(
                        "priority", "10001", true, null, null));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "priority", PROJECT, "10001");

        assertThat(result.getRequired()).isTrue();
    }

    @Test
    void getFieldDetailsBySecretName_nonMatchingIssueTypeId_requiredRemainsNull() {
        stubFieldsChain(List.of(navigableField("priority", "Priority", false)));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(buildConstraintMetaWithIssueType(
                        "priority", "10001", true, null, null));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "priority", PROJECT, "99999");

        assertThat(result.getRequired()).isNull();
    }

    @Test
    void getFieldDetailsBySecretName_userDataType_populatesUsers() {
        stubFieldsChain(List.of(
                navigableFieldWithSchema("reporter", "Reporter", false, "user")));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(OBJECT_MAPPER.createObjectNode());
        JsonNode usersNode = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getAssignableUsers(SECRET, PROJECT)).thenReturn(usersNode);
        when(responseMapper.hasValidData(usersNode)).thenReturn(true);
        when(responseMapper.mapUsers(usersNode)).thenReturn(List.of(
                JiraUserResponse.builder()
                        .accountId("u1").displayName("Alice").build()));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "reporter", PROJECT, null);

        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getUsers().get(0).get("accountId")).isEqualTo("u1");
    }

    @Test
    void getFieldDetailsBySecretName_arrayDataType_populatesUsers() {
        stubFieldsChain(List.of(
                navigableFieldWithSchema("components", "Components", false, "array")));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(OBJECT_MAPPER.createObjectNode());
        JsonNode usersNode = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getAssignableUsers(SECRET, PROJECT)).thenReturn(usersNode);
        when(responseMapper.hasValidData(usersNode)).thenReturn(true);
        when(responseMapper.mapUsers(usersNode)).thenReturn(List.of());

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "components", PROJECT, null);

        verify(jiraApiClient).getAssignableUsers(SECRET, PROJECT);
        assertThat(result.getUsers()).isEmpty();
    }

    @Test
    void getFieldDetailsBySecretName_stringDataType_doesNotPopulateUsers() {
        stubFieldsChain(List.of(
                navigableFieldWithSchema("summary", "Summary", false, "string")));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(OBJECT_MAPPER.createObjectNode());

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "summary", PROJECT, null);

        assertThat(result.getUsers()).isNull();
        verify(jiraApiClient, never()).getAssignableUsers(any(), any());
    }

    @Test
    void getFieldDetailsBySecretName_constraintMetaNullProjects_buildsDetailWithoutConstraints() {
        stubFieldsChain(List.of(navigableField("priority", "Priority", false)));
        when(jiraApiClient.get(any(String.class), eq(SECRET)))
                .thenReturn(OBJECT_MAPPER.createObjectNode());

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "priority", PROJECT, null);

        assertThat(result.getId()).isEqualTo("priority");
        assertThat(result.getRequired()).isNull();
        assertThat(result.getAllowedValues()).isNull();
    }

    @Test
    void getFieldDetailsBySecretName_blankProjectKey_skipsConstraintPopulation() {
        stubFieldsChain(List.of(navigableField("summary", "Summary", false)));

        JiraFieldDetailResponse result =
                service.getFieldDetailsBySecretName(SECRET, "summary", "  ", null);

        // blank projectKey → no jiraApiClient.get() call for constraints
        verify(jiraApiClient, never()).get(any(String.class), eq(SECRET));
        assertThat(result.getId()).isEqualTo("summary");
    }

    @Test
    void getFieldDetailsBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.getFields(SECRET)).thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() ->
                service.getFieldDetailsBySecretName(SECRET, "summary", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching field details");
    }

    // -----------------------------------------------------------------------
    // getTeamsBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getTeamsBySecretName_hasValidData_returnsMappedTeams() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        List<JiraTeamResponse> expected =
                List.of(JiraTeamResponse.builder().name("Team A").build());
        when(jiraApiClient.searchTeams(SECRET, null, null, null)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapTeams(node)).thenReturn(expected);

        assertThat(service.getTeamsBySecretName(SECRET, null, null, null)).isEqualTo(expected);
    }

    @Test
    void getTeamsBySecretName_noValidData_returnsEmptyList() {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.searchTeams(SECRET, "query", 0, 10)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(false);

        assertThat(service.getTeamsBySecretName(SECRET, "query", 0, 10)).isEmpty();
    }

    @Test
    void getTeamsBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.searchTeams(SECRET, null, null, null))
                .thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> service.getTeamsBySecretName(SECRET, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching teams");
    }

    // -----------------------------------------------------------------------
    // getSprintsBySecretName
    // -----------------------------------------------------------------------

    @Test
    void getSprintsBySecretName_boardIdProvided_skipsBoardLookup() {
        JsonNode sprintsNode = OBJECT_MAPPER.createObjectNode();
        List<JiraSprintResponse> expected =
                List.of(JiraSprintResponse.builder().name("Sprint 1").build());
        when(jiraApiClient.getSprintsByBoard(SECRET, 42L, null, null, null))
                .thenReturn(sprintsNode);
        when(responseMapper.hasValidData(sprintsNode)).thenReturn(true);
        when(responseMapper.mapSprints(sprintsNode)).thenReturn(expected);

        assertThat(service.getSprintsBySecretName(SECRET, 42L, null, null, null, null))
                .isEqualTo(expected);
        verify(jiraApiClient, never()).getBoardsByProject(any(), any());
    }

    @Test
    void getSprintsBySecretName_noBoardIdWithProjectKey_resolvesFirstBoard() {
        ObjectNode boardsNode = OBJECT_MAPPER.createObjectNode();
        boardsNode.putArray("values").addObject().put("id", 7);
        JsonNode sprintsNode = OBJECT_MAPPER.createObjectNode();
        List<JiraSprintResponse> expected =
                List.of(JiraSprintResponse.builder().name("Sprint A").build());
        when(jiraApiClient.getBoardsByProject(SECRET, PROJECT)).thenReturn(boardsNode);
        when(jiraApiClient.getSprintsByBoard(SECRET, 7L, null, null, null))
                .thenReturn(sprintsNode);
        when(responseMapper.hasValidData(sprintsNode)).thenReturn(true);
        when(responseMapper.mapSprints(sprintsNode)).thenReturn(expected);

        assertThat(service.getSprintsBySecretName(SECRET, null, PROJECT, null, null, null))
                .isEqualTo(expected);
    }

    @Test
    void getSprintsBySecretName_noBoardIdNullProjectKey_returnsEmptyList() {
        assertThat(service.getSprintsBySecretName(SECRET, null, null, null, null, null))
                .isEmpty();
    }

    @Test
    void getSprintsBySecretName_noBoardIdBlankProjectKey_returnsEmptyList() {
        assertThat(service.getSprintsBySecretName(SECRET, null, "  ", null, null, null))
                .isEmpty();
    }

    @Test
    void getSprintsBySecretName_noBoardIdEmptyBoardsArray_returnsEmptyList() {
        ObjectNode emptyBoards = OBJECT_MAPPER.createObjectNode();
        emptyBoards.putArray("values");
        when(jiraApiClient.getBoardsByProject(SECRET, PROJECT)).thenReturn(emptyBoards);

        assertThat(service.getSprintsBySecretName(SECRET, null, PROJECT, null, null, null))
                .isEmpty();
    }

    @Test
    void getSprintsBySecretName_sprintsNoValidData_returnsEmptyList() {
        JsonNode sprintsNode = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getSprintsByBoard(SECRET, 5L, null, null, null))
                .thenReturn(sprintsNode);
        when(responseMapper.hasValidData(sprintsNode)).thenReturn(false);

        assertThat(service.getSprintsBySecretName(SECRET, 5L, null, null, null, null)).isEmpty();
    }

    @Test
    void getSprintsBySecretName_withAllParams_passesThemThrough() {
        JsonNode sprintsNode = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getSprintsByBoard(SECRET, 3L, 10, 50, "active"))
                .thenReturn(sprintsNode);
        when(responseMapper.hasValidData(sprintsNode)).thenReturn(false);

        service.getSprintsBySecretName(SECRET, 3L, null, "active", 10, 50);

        verify(jiraApiClient).getSprintsByBoard(SECRET, 3L, 10, 50, "active");
    }

    @Test
    void getSprintsBySecretName_clientThrows_throwsRuntimeException() {
        when(jiraApiClient.getSprintsByBoard(SECRET, 1L, null, null, null))
                .thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() ->
                service.getSprintsBySecretName(SECRET, 1L, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fetching sprints");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void stubFieldsChain(final List<JiraFieldResponse> fields) {
        JsonNode node = OBJECT_MAPPER.createObjectNode();
        when(jiraApiClient.getFields(SECRET)).thenReturn(node);
        when(responseMapper.hasValidData(node)).thenReturn(true);
        when(responseMapper.mapFields(node)).thenReturn(fields);
    }

    private JiraFieldResponse navigableField(
            final String id, final String name, final boolean custom) {
        return JiraFieldResponse.builder()
                .id(id).name(name).navigable(true).custom(custom).build();
    }

    private JiraFieldResponse navigableFieldWithSchema(
            final String id,
            final String name,
            final boolean custom,
            final String schemaType) {
        JiraFieldResponse.JiraFieldSchema schema =
                JiraFieldResponse.JiraFieldSchema.builder().type(schemaType).build();
        return JiraFieldResponse.builder()
                .id(id).name(name).navigable(true).custom(custom)
                .schemaDetails(schema).build();
    }

    private JsonNode buildConstraintMeta(
            final String fieldId,
            final boolean required,
            final String allowedValue1,
            final String allowedValue2) {
        return buildConstraintMetaWithIssueType(
                fieldId, "10001", required, allowedValue1, allowedValue2);
    }

    private JsonNode buildConstraintMetaWithIssueType(
            final String fieldId,
            final String issueTypeId,
            final boolean required,
            final String allowedValue1,
            final String allowedValue2) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode projects = root.putArray("projects");
        ObjectNode project = projects.addObject();
        ArrayNode issueTypes = project.putArray("issuetypes");
        ObjectNode issueType = issueTypes.addObject();
        issueType.put("id", issueTypeId);
        ObjectNode fields = issueType.putObject("fields");
        ObjectNode field = fields.putObject(fieldId);
        field.put("required", required);
        if (allowedValue1 != null) {
            ArrayNode allowed = field.putArray("allowedValues");
            allowed.addObject().put("name", allowedValue1);
            if (allowedValue2 != null) {
                allowed.addObject().put("name", allowedValue2);
            }
        }
        return root;
    }
}
