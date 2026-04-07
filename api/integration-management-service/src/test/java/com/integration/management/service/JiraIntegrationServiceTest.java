package com.integration.management.service;

import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import com.integration.management.ies.client.IesJiraApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIntegrationService")
class JiraIntegrationServiceTest {

    @Mock
    private IesJiraApiClient iesJiraApiClient;

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    @InjectMocks
    private JiraIntegrationService service;

    @Test
    @DisplayName("getProjectsByConnectionId resolves secret and delegates")
    void getProjectsByConnectionId_resolvesSecretAndDelegates() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraProjectResponse> expected = List.of(mock(JiraProjectResponse.class));
        when(iesJiraApiClient.getProjects("secret-1")).thenReturn(expected);

        List<JiraProjectResponse> actual = service.getProjectsByConnectionId(connectionId, "tenant-1");

        assertThat(actual).isSameAs(expected);
        verify(iesJiraApiClient).getProjects("secret-1");
    }

    @Test
    @DisplayName("getProjectUsersByConnectionId resolves secret and delegates")
    void getProjectUsersByConnectionId_resolvesSecretAndDelegates() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraUserResponse> expected = List.of(mock(JiraUserResponse.class));
        when(iesJiraApiClient.getProjectUsers("secret-1", "PRJ")).thenReturn(expected);

        List<JiraUserResponse> actual = service.getProjectUsersByConnectionId(connectionId, "tenant-1", "PRJ");

        assertThat(actual).isSameAs(expected);
        verify(iesJiraApiClient).getProjectUsers("secret-1", "PRJ");
    }

    @Test
    @DisplayName("getProjectIssueTypesByConnectionId resolves secret and delegates")
    void getProjectIssueTypesByConnectionId_resolvesSecretAndDelegates() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraIssueTypeResponse> expected = List.of(mock(JiraIssueTypeResponse.class));
        when(iesJiraApiClient.getProjectIssueTypes("secret-1", "PRJ")).thenReturn(expected);

        List<JiraIssueTypeResponse> actual = service.getProjectIssueTypesByConnectionId(connectionId, "tenant-1",
                "PRJ");

        assertThat(actual).isSameAs(expected);
        verify(iesJiraApiClient).getProjectIssueTypes("secret-1", "PRJ");
    }

    @Test
    @DisplayName("getFieldsByConnectionId returns empty list when client returns null")
    void getFieldsByConnectionId_nullFromClient_returnsEmptyList() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        when(iesJiraApiClient.getFields("secret-1")).thenReturn(null);

        List<JiraFieldResponse> actual = service.getFieldsByConnectionId(connectionId, "tenant-1");

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("getFieldsByConnectionId returns field list when client returns non-null")
    void getFieldsByConnectionId_nonNullFromClient_returnsList() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraFieldResponse> expected = List.of(mock(JiraFieldResponse.class));
        when(iesJiraApiClient.getFields("secret-1")).thenReturn(expected);

        List<JiraFieldResponse> actual = service.getFieldsByConnectionId(connectionId, "tenant-1");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getProjectMetaFieldsByConnectionId returns empty list for null or empty results")
    void getProjectMetaFieldsByConnectionId_nullOrEmpty_returnsEmptyList() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");

        when(iesJiraApiClient.getProjectMetaFields("secret-1", "PRJ", null)).thenReturn(null);
        List<JiraFieldResponse> nullCase = service.getProjectMetaFieldsByConnectionId(connectionId, "tenant-1", "PRJ",
                null);
        assertThat(nullCase).isEmpty();

        when(iesJiraApiClient.getProjectMetaFields("secret-1", "PRJ", "10001")).thenReturn(List.of());
        List<JiraFieldResponse> emptyCase = service.getProjectMetaFieldsByConnectionId(connectionId, "tenant-1", "PRJ",
                "10001");
        assertThat(emptyCase).isEmpty();
    }

    @Test
    @DisplayName("getProjectMetaFieldsByConnectionId returns list when non-empty")
    void getProjectMetaFieldsByConnectionId_nonEmpty_returnsList() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraFieldResponse> expected = List.of(mock(JiraFieldResponse.class));
        when(iesJiraApiClient.getProjectMetaFields("secret-1", "PRJ", "10001")).thenReturn(expected);

        List<JiraFieldResponse> actual = service.getProjectMetaFieldsByConnectionId(connectionId, "tenant-1", "PRJ",
                "10001");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getFieldDetailsByConnectionId delegates and returns response")
    void getFieldDetailsByConnectionId_delegatesAndReturnsResponse() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        JiraFieldDetailResponse expected = mock(JiraFieldDetailResponse.class);
        when(iesJiraApiClient.getFieldDetails("secret-1", "field-1", "PRJ", "10001")).thenReturn(expected);

        JiraFieldDetailResponse actual = service.getFieldDetailsByConnectionId(connectionId, "tenant-1", "field-1",
                "PRJ", "10001");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getTeamsByConnectionId delegates with query and paging")
    void getTeamsByConnectionId_delegatesWithQueryAndPaging() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraTeamResponse> expected = List.of(mock(JiraTeamResponse.class));
        when(iesJiraApiClient.getTeams("secret-1", "kw", 0, 20)).thenReturn(expected);

        List<JiraTeamResponse> actual = service.getTeamsByConnectionId(connectionId, "tenant-1", "kw", 0, 20);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getSprintsByConnectionId delegates with optional filters")
    void getSprintsByConnectionId_delegatesWithOptionalFilters() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<JiraSprintResponse> expected = List.of(mock(JiraSprintResponse.class));
        when(iesJiraApiClient.getSprints("secret-1", 10L, "PRJ", "active", 0, 50)).thenReturn(expected);

        List<JiraSprintResponse> actual = service.getSprintsByConnectionId(connectionId, "tenant-1", 10L, "PRJ",
                "active", 0, 50);

        assertThat(actual).isSameAs(expected);
    }
}
