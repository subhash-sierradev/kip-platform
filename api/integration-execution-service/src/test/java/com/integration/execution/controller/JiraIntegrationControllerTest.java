package com.integration.execution.controller;

import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import com.integration.execution.service.JiraIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraIntegrationControllerTest {

    @Mock
    private JiraIntegrationService jiraIntegrationService;

    private JiraIntegrationController controller;

    @BeforeEach
    void setUp() {
        controller = new JiraIntegrationController(jiraIntegrationService);
    }

    @Test
    void getProjects_validSecretName_returnsProjects() {
        List<JiraProjectResponse> projects = List.of(
            JiraProjectResponse.builder().key("PRJ").name("Project").description("Project").build());
        when(jiraIntegrationService.getProjectsBySecretName("secret")).thenReturn(projects);

        ResponseEntity<List<JiraProjectResponse>> response = controller.getProjects("secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(projects);
        verify(jiraIntegrationService).getProjectsBySecretName("secret");
    }

    @Test
    void getProjectUsers_validInput_returnsUsers() {
        List<JiraUserResponse> users = List.of(
            JiraUserResponse.builder().accountId("id-1").displayName("name").emailAddress("email").active(true)
                .build());
        when(jiraIntegrationService.getProjectUsersBySecretName("secret", "PRJ")).thenReturn(users);

        ResponseEntity<List<JiraUserResponse>> response = controller.getProjectUsers("secret", "PRJ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(users);
        verify(jiraIntegrationService).getProjectUsersBySecretName("secret", "PRJ");
    }

    @Test
    void getProjectIssueTypes_validInput_returnsIssueTypes() {
        List<JiraIssueTypeResponse> issueTypes = List.of(
            JiraIssueTypeResponse.builder().id("1").name("Bug").build());
        when(jiraIntegrationService.getProjectIssueTypesBySecretName("secret", "PRJ")).thenReturn(issueTypes);

        ResponseEntity<List<JiraIssueTypeResponse>> response = controller.getProjectIssueTypes("secret", "PRJ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(issueTypes);
        verify(jiraIntegrationService).getProjectIssueTypesBySecretName("secret", "PRJ");
    }

    @Test
    void getProjectParentIssues_withQuery_returnsParentIssues() {
        List<JiraIssueReferenceResponse> parentIssues = List.of(
            JiraIssueReferenceResponse.builder().key("PRJ-101").summary("Parent issue").build());
        when(jiraIntegrationService.getParentIssuesBySecretName("secret", "PRJ", "PRJ-10", 0, 20))
            .thenReturn(parentIssues);

        ResponseEntity<List<JiraIssueReferenceResponse>> response =
            controller.getProjectParentIssues("secret", "PRJ", "PRJ-10", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(parentIssues);
        verify(jiraIntegrationService).getParentIssuesBySecretName("secret", "PRJ", "PRJ-10", 0, 20);
    }

    @Test
    void getFields_validSecretName_returnsFields() {
        List<JiraFieldResponse> fields = List.of(JiraFieldResponse.builder().id("customfield_1").name("Team").build());
        when(jiraIntegrationService.getFieldsBySecretName("secret")).thenReturn(fields);

        ResponseEntity<List<JiraFieldResponse>> response = controller.getFields("secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(fields);
        verify(jiraIntegrationService).getFieldsBySecretName("secret");
    }

    @Test
    void getFieldDetails_validInput_returnsFieldDetails() {
        JiraFieldDetailResponse details = JiraFieldDetailResponse.builder().id("customfield_1").name("Team").build();
        when(jiraIntegrationService.getFieldDetailsBySecretName("secret", "customfield_1", "PRJ", "1001"))
                .thenReturn(details);

        ResponseEntity<JiraFieldDetailResponse> response =
                controller.getFieldDetails("secret", "customfield_1", "PRJ", "1001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(details);
        verify(jiraIntegrationService).getFieldDetailsBySecretName("secret", "customfield_1", "PRJ", "1001");
    }

    @Test
    void getTeams_withPagingInput_returnsTeams() {
        List<JiraTeamResponse> teams = List.of(new JiraTeamResponse("1", "Team A"));
        when(jiraIntegrationService.getTeamsBySecretName("secret", "qa", 0, 20)).thenReturn(teams);

        ResponseEntity<List<JiraTeamResponse>> response = controller.getTeams("secret", "qa", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(teams);
        verify(jiraIntegrationService).getTeamsBySecretName("secret", "qa", 0, 20);
    }

    @Test
    void getSprints_withBoardAndProjectInput_returnsSprints() {
        List<JiraSprintResponse> sprints = List.of(
            new JiraSprintResponse(10L, "Sprint 1", "active", null, null, null));
        when(jiraIntegrationService.getSprintsBySecretName("secret", 101L, "PRJ", "active", 0, 10))
                .thenReturn(sprints);

        ResponseEntity<List<JiraSprintResponse>> response =
                controller.getSprints("secret", 101L, "PRJ", "active", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sprints);
        verify(jiraIntegrationService).getSprintsBySecretName("secret", 101L, "PRJ", "active", 0, 10);
    }
}
