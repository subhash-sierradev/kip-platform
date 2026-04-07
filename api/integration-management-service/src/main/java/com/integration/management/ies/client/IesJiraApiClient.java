package com.integration.management.ies.client;

import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "execution-jira-integration")
public interface IesJiraApiClient {

    @PostMapping("/api/integrations/jira/connections/{secretName}/projects")
    List<JiraProjectResponse> getProjects(@PathVariable String secretName);

    @PostMapping("/api/integrations/jira/connections/{secretName}/projects/{projectKey}/users")
    List<JiraUserResponse> getProjectUsers(@PathVariable String secretName, @PathVariable String projectKey);

    @PostMapping("/api/integrations/jira/connections/{secretName}/projects/{projectKey}/issue-types")
    List<JiraIssueTypeResponse> getProjectIssueTypes(@PathVariable String secretName,
            @PathVariable String projectKey);

    @PostMapping("/api/integrations/jira/connections/{secretName}/projects/{projectKey}/parent-issues")
    List<JiraIssueReferenceResponse> getProjectParentIssues(
            @PathVariable String secretName,
            @PathVariable String projectKey,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer startAt,
            @RequestParam(required = false) Integer maxResults);

    @PostMapping("/api/integrations/jira/connections/{secretName}/fields")
    List<JiraFieldResponse> getFields(@PathVariable String secretName);

    @PostMapping("/api/integrations/jira/connections/{secretName}/fields/{fieldId}")
    JiraFieldDetailResponse getFieldDetails(
            @PathVariable String secretName,
            @PathVariable String fieldId,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String issueTypeId);

    @PostMapping("/api/integrations/jira/connections/{secretName}/teams")
    List<JiraTeamResponse> getTeams(
            @PathVariable String secretName,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer startAt,
            @RequestParam(required = false) Integer maxResults);

    @PostMapping("/api/integrations/jira/connections/{secretName}/sprints")
    List<JiraSprintResponse> getSprints(
            @PathVariable String secretName,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Integer startAt,
            @RequestParam(required = false) Integer maxResults);

    @PostMapping("/api/integrations/jira/connections/{secretName}/projects/{projectKey}/meta-fields")
    List<JiraFieldResponse> getProjectMetaFields(
        @PathVariable String secretName,
        @PathVariable String projectKey,
        @RequestParam(required = false) String issueTypeId);
}
