package com.integration.management.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import com.integration.management.service.JiraIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIntegrationController")
class JiraIntegrationControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final UUID CONNECTION_ID = UUID.randomUUID();
    private static final String PROJECT_KEY = "PRJ";
    private static final String BASE_URL = "/api/integrations/jira";

    @Mock
    private JiraIntegrationService jiraIntegrationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JiraIntegrationController(jiraIntegrationService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/teams")
    class GetTeamsByConnectionId {

        @Test
        @DisplayName("should return 200 with list of teams")
        void getTeams_validRequest_returnsOkWithList() throws Exception {
            JiraTeamResponse team = new JiraTeamResponse();

            when(jiraIntegrationService.getTeamsByConnectionId(eq(CONNECTION_ID), eq(TENANT_ID), any(), any(), any()))
                    .thenReturn(List.of(team));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/teams", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("query", "abc")
                    .param("startAt", "0")
                    .param("maxResults", "10")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(jiraIntegrationService).getTeamsByConnectionId(eq(CONNECTION_ID), eq(TENANT_ID), any(), any(),
                    any());
        }

        @Test
        @DisplayName("should return empty list when no teams found")
        void getTeams_noTeams_returnsEmptyList() throws Exception {
            when(jiraIntegrationService.getTeamsByConnectionId(eq(CONNECTION_ID), eq(TENANT_ID), any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/teams", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should handle service exception")
        void getTeams_serviceThrowsException_returns500() throws Exception {
            when(jiraIntegrationService.getTeamsByConnectionId(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("API error"));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/teams", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/sprints")
    class GetSprintsByConnectionId {

        @Test
        @DisplayName("should return 200 with list of sprints")
        void getSprints_validRequest_returnsOkWithList() throws Exception {
            JiraSprintResponse sprint = new JiraSprintResponse();

            when(jiraIntegrationService.getSprintsByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(sprint));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/sprints", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("boardId", "101")
                    .param("projectKey", PROJECT_KEY)
                    .param("state", "active")
                    .param("startAt", "0")
                    .param("maxResults", "50")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(jiraIntegrationService).getSprintsByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should return empty list when no sprints found")
        void getSprints_noSprints_returnsEmptyList() throws Exception {
            when(jiraIntegrationService.getSprintsByConnectionId(
                    any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/sprints", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/projects")
    class GetProjectsByConnectionId {

        @Test
        @DisplayName("should return 200 with list of projects")
        void getProjects_validRequest_returnsOkWithList() throws Exception {
            JiraProjectResponse project = new JiraProjectResponse();

            when(jiraIntegrationService.getProjectsByConnectionId(eq(CONNECTION_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(project));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(jiraIntegrationService).getProjectsByConnectionId(CONNECTION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no projects found")
        void getProjects_noProjects_returnsEmptyList() throws Exception {
            when(jiraIntegrationService.getProjectsByConnectionId(any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should handle service exception")
        void getProjects_serviceThrowsException_returns500() throws Exception {
            when(jiraIntegrationService.getProjectsByConnectionId(any(), any()))
                    .thenThrow(new RuntimeException("API error"));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/projects/{projectKey}/users")
    class GetProjectUsersByConnectionId {

        @Test
        @DisplayName("should return 200 with list of users")
        void getProjectUsers_validRequest_returnsOkWithList() throws Exception {
            JiraUserResponse user = new JiraUserResponse();

            when(jiraIntegrationService.getProjectUsersByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq(PROJECT_KEY)))
                    .thenReturn(List.of(user));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/users",
                    CONNECTION_ID, PROJECT_KEY)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(jiraIntegrationService).getProjectUsersByConnectionId(CONNECTION_ID, TENANT_ID, PROJECT_KEY);
        }

        @ParameterizedTest
        @ValueSource(strings = { "  ", " " })
        @DisplayName("should return 400 for blank project key")
        void getProjectUsers_blankProjectKey_returnsBadRequest(String projectKey) throws Exception {
            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/users",
                    CONNECTION_ID, projectKey)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest());

            verify(jiraIntegrationService, never()).getProjectUsersByConnectionId(any(), any(), any());
        }

        @Test
        @DisplayName("should return empty list when no users found")
        void getProjectUsers_noUsers_returnsEmptyList() throws Exception {
            when(jiraIntegrationService.getProjectUsersByConnectionId(any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/users",
                    CONNECTION_ID, PROJECT_KEY)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/projects/{projectKey}/issue-types")
    class GetProjectIssueTypesByConnectionId {

        @Test
        @DisplayName("should return 200 with list of issue types")
        void getIssueTypes_validRequest_returnsOkWithList() throws Exception {
            JiraIssueTypeResponse issueType = new JiraIssueTypeResponse();

            when(jiraIntegrationService.getProjectIssueTypesByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq(PROJECT_KEY)))
                    .thenReturn(List.of(issueType));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/issue-types",
                    CONNECTION_ID, PROJECT_KEY)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(jiraIntegrationService).getProjectIssueTypesByConnectionId(CONNECTION_ID, TENANT_ID, PROJECT_KEY);
        }

        @Test
        @DisplayName("should return empty list when no issue types found")
        void getIssueTypes_noIssueTypes_returnsEmptyList() throws Exception {
            when(jiraIntegrationService.getProjectIssueTypesByConnectionId(any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/issue-types",
                    CONNECTION_ID, PROJECT_KEY)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @ParameterizedTest
        @ValueSource(strings = { "  ", " " })
        @DisplayName("should return 400 for blank project key")
        void getIssueTypes_blankProjectKey_returnsBadRequest(String projectKey) throws Exception {
            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/issue-types",
                    CONNECTION_ID, projectKey)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest());

            verify(jiraIntegrationService, never()).getProjectIssueTypesByConnectionId(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/projects/{projectKey}/parent-issues")
    class GetProjectParentIssuesByConnectionId {

        @Test
        @DisplayName("should return 200 with list of parent issues")
        void getProjectParentIssues_validRequest_returnsOkWithList() throws Exception {
            JiraIssueReferenceResponse issue = JiraIssueReferenceResponse.builder()
                    .key("PRJ-101")
                    .summary("Parent issue")
                    .issueType("Task")
                    .build();

            when(jiraIntegrationService.getProjectParentIssuesByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq(PROJECT_KEY), any(), any(), any()))
                    .thenReturn(List.of(issue));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/parent-issues",
                    CONNECTION_ID, PROJECT_KEY)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("startAt", "0")
                    .param("maxResults", "20"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].key").value("PRJ-101"));

            verify(jiraIntegrationService).getProjectParentIssuesByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq(PROJECT_KEY), any(), any(), any());
        }

        @Test
        @DisplayName("should pass query param when provided")
        void getProjectParentIssues_withQueryParam_passesQuery() throws Exception {
            when(jiraIntegrationService.getProjectParentIssuesByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq(PROJECT_KEY), eq("SCRUM"), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/parent-issues",
                    CONNECTION_ID, PROJECT_KEY)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("query", "SCRUM")
                    .param("startAt", "0")
                    .param("maxResults", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(jiraIntegrationService).getProjectParentIssuesByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq(PROJECT_KEY), eq("SCRUM"), any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = { "  ", " " })
        @DisplayName("should return 400 for blank project key")
        void getProjectParentIssues_blankProjectKey_returnsBadRequest(String projectKey) throws Exception {
            mockMvc.perform(post(BASE_URL + "/connections/{id}/projects/{projectKey}/parent-issues",
                    CONNECTION_ID, projectKey)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest());

            verify(jiraIntegrationService, never()).getProjectParentIssuesByConnectionId(
                    any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/fields")
    class GetFieldsByConnectionId {

        @Test
        @DisplayName("should return 200 with list of fields")
        void getFields_validRequest_returnsOkWithList() throws Exception {
            JiraFieldResponse field = JiraFieldResponse.builder()
                    .id("summary")
                    .name("Summary")
                    .build();

            when(jiraIntegrationService.getFieldsByConnectionId(eq(CONNECTION_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(field));

            mockMvc.perform(post(BASE_URL + "/connections/{id}/fields", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(jiraIntegrationService).getFieldsByConnectionId(CONNECTION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no fields found")
        void getFields_noFields_returnsEmptyList() throws Exception {
            when(jiraIntegrationService.getFieldsByConnectionId(any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(post(BASE_URL + "/connections/{id}/fields", CONNECTION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/jira/connections/{connectionId}/fields/{fieldId}")
    class GetFieldDetailsByConnectionId {

        @Test
        @DisplayName("should return 200 with field details")
        void getFieldDetails_validRequest_returnsOkWithDetails() throws Exception {
            var fieldDetails = JiraFieldDetailResponse.builder()
                    .id("summary")
                    .name("Summary")
                    .required(true)
                    .build();

            when(jiraIntegrationService.getFieldDetailsByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq("summary"), any(), any()))
                    .thenReturn(fieldDetails);

            mockMvc.perform(post(BASE_URL + "/connections/{id}/fields/{fieldId}",
                    CONNECTION_ID, "summary")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("projectKey", PROJECT_KEY)
                    .param("issueTypeId", "10001"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value("summary"))
                    .andExpect(jsonPath("$.name").value("Summary"));

            verify(jiraIntegrationService).getFieldDetailsByConnectionId(
                    CONNECTION_ID, TENANT_ID, "summary", PROJECT_KEY, "10001");
        }

        @Test
        @DisplayName("should work without optional parameters")
        void getFieldDetails_noOptionalParams_returnsOk() throws Exception {
            var fieldDetails = JiraFieldDetailResponse.builder()
                    .id("summary")
                    .name("Summary")
                    .build();

            when(jiraIntegrationService.getFieldDetailsByConnectionId(
                    eq(CONNECTION_ID), eq(TENANT_ID), eq("summary"), isNull(), isNull()))
                    .thenReturn(fieldDetails);

            mockMvc.perform(post(BASE_URL + "/connections/{id}/fields/{fieldId}",
                    CONNECTION_ID, "summary")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = { "  ", " " })
        @DisplayName("should return 400 for blank field ID")
        void getFieldDetails_blankFieldId_returnsBadRequest(String fieldId) throws Exception {
            mockMvc.perform(post(BASE_URL + "/connections/{id}/fields/{fieldId}",
                    CONNECTION_ID, fieldId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest());

            verify(jiraIntegrationService, never()).getFieldDetailsByConnectionId(
                    any(), any(), any(), any(), any());
        }
    }
}
