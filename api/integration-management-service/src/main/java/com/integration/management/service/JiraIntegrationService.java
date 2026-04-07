package com.integration.management.service;

import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import com.integration.management.ies.client.IesJiraApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraIntegrationService {

    private final IesJiraApiClient iesJiraApiClient;
    private final IntegrationConnectionService integrationConnectionService;

    @Cacheable(value = "jiraProjectsByConnectionCache", key = "#connectionId + ':' + #tenantId")
    public List<JiraProjectResponse> getProjectsByConnectionId(final UUID connectionId, final String tenantId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesJiraApiClient.getProjects(secretName);
    }

    @Cacheable(value = "jiraUsersByConnectionCache", key = "#connectionId + ':' + #tenantId + ':' + #projectKey")
    public List<JiraUserResponse> getProjectUsersByConnectionId(
            final UUID connectionId, final String tenantId, final String projectKey) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesJiraApiClient.getProjectUsers(secretName, projectKey);
    }

    @Cacheable(value = "jiraIssueTypesByConnectionCache", key = "#connectionId + ':' + #tenantId + ':' + #projectKey")
    public List<JiraIssueTypeResponse> getProjectIssueTypesByConnectionId(
            final UUID connectionId, final String tenantId, final String projectKey) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesJiraApiClient.getProjectIssueTypes(secretName, projectKey);
    }

    @Cacheable(
            value = "jiraParentIssuesByConnectionCache",
            key = "#connectionId + ':' + #tenantId + ':' + #projectKey + ':' + (#startAt == null ? 0 : #startAt)"
                    + " + ':' + (#query == null ? '' : #query)"
                    + " + ':' + (#maxResults == null ? 100 : #maxResults)"
    )
    public List<JiraIssueReferenceResponse> getProjectParentIssuesByConnectionId(
            final UUID connectionId,
            final String tenantId,
            final String projectKey,
            final String query,
            final Integer startAt,
            final Integer maxResults) {
        String secretName = integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(),
                tenantId);
        return iesJiraApiClient.getProjectParentIssues(secretName, projectKey, query, startAt, maxResults);
    }

    @Cacheable(value = "jiraFieldsByConnectionCache", key = "#connectionId + ':' + #tenantId")
    public List<JiraFieldResponse> getFieldsByConnectionId(final UUID connectionId, final String tenantId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        List<JiraFieldResponse> fields = iesJiraApiClient.getFields(secretName);
        return (fields == null) ? List.of() : fields;
    }

    @Cacheable(value = "jiraProjectMetaFieldsByConnectionCache",
        key = "#connectionId + ':' + #tenantId + ':' + #projectKey + ':' + (#issueTypeId == null ? '' : #issueTypeId)"
    )
    public List<JiraFieldResponse> getProjectMetaFieldsByConnectionId(
            final UUID connectionId,
            final String tenantId,
            final String projectKey,
            final String issueTypeId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        List<JiraFieldResponse> fields = iesJiraApiClient.getProjectMetaFields(secretName, projectKey, issueTypeId);
        return (fields == null || fields.isEmpty()) ? List.of() : fields;
    }

    @Cacheable(
            value = "jiraFieldDetailsByConnectionCache",
            key = "#connectionId + ':' + #tenantId + ':' + #fieldId + ':' + (#projectKey == null ? '' : #projectKey)"
                    + " + ':' + (#issueTypeId == null ? '' : #issueTypeId)"
    )
    public JiraFieldDetailResponse getFieldDetailsByConnectionId(
            final UUID connectionId,
            final String tenantId,
            final String fieldId,
            final String projectKey,
            final String issueTypeId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesJiraApiClient.getFieldDetails(secretName, fieldId, projectKey, issueTypeId);
    }

    @Cacheable(
            value = "jiraTeamsByConnectionCache",
            key = "T(String).format('%s:%s:%s:%s:%s', #connectionId, #tenantId, "
                    + "(#query ?: ''), (#startAt ?: 0), (#maxResults ?: 20))"
    )
    public List<JiraTeamResponse> getTeamsByConnectionId(
            final UUID connectionId,
            final String tenantId,
            final String query,
            final Integer startAt,
            final Integer maxResults) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesJiraApiClient.getTeams(secretName, query, startAt, maxResults);
    }

    @Cacheable(
            value = "jiraSprintsByConnectionCache",
            key = "T(String).format('%s:%s:%s:%s:%s:%s:%s', #connectionId, #tenantId, "
                    + "(#boardId ?: ''), (#projectKey ?: ''), (#state ?: ''), "
                    + "(#startAt ?: ''), (#maxResults ?: ''))"
    )
    public List<JiraSprintResponse> getSprintsByConnectionId(
            final UUID connectionId,
            final String tenantId,
            final Long boardId,
            final String projectKey,
            final String state,
            final Integer startAt,
            final Integer maxResults) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesJiraApiClient.getSprints(secretName, boardId, projectKey, state, startAt, maxResults);
    }
}
