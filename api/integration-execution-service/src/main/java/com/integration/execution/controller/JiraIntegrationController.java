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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Controller for Jira integration operations using secretName (resolved by IMS from connectionId).
 * All endpoints receive a pre-resolved secretName from IMS via Feign.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/jira")
@PreAuthorize("hasRole('feature_jira_webhook')")
@Validated
public class JiraIntegrationController {

    private final JiraIntegrationService jiraIntegrationService;

    @PostMapping("/connections/{secretName}/projects")
    public ResponseEntity<List<JiraProjectResponse>> getProjects(
            @PathVariable @NotBlank String secretName) {
        log.info("Fetching Jira projects for secretName: {}", secretName);
        return ResponseEntity.ok(jiraIntegrationService.getProjectsBySecretName(secretName));
    }

    @PostMapping("/connections/{secretName}/projects/{projectKey}/users")
    public ResponseEntity<List<JiraUserResponse>> getProjectUsers(
            @PathVariable @NotBlank String secretName,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey) {
        log.info("Fetching Jira users for secretName: {}, projectKey: {}", secretName, projectKey);
        return ResponseEntity.ok(jiraIntegrationService.getProjectUsersBySecretName(secretName, projectKey));
    }

    @PostMapping("/connections/{secretName}/projects/{projectKey}/issue-types")
    public ResponseEntity<List<JiraIssueTypeResponse>> getProjectIssueTypes(
            @PathVariable @NotBlank String secretName,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey) {
        log.info("Fetching Jira issue types for secretName: {}, projectKey: {}", secretName, projectKey);
        return ResponseEntity.ok(jiraIntegrationService.getProjectIssueTypesBySecretName(secretName, projectKey));
    }

    @PostMapping("/connections/{secretName}/projects/{projectKey}/parent-issues")
    public ResponseEntity<List<JiraIssueReferenceResponse>> getProjectParentIssues(
            @PathVariable @NotBlank String secretName,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startAt", required = false) Integer startAt,
            @RequestParam(value = "maxResults", required = false) Integer maxResults) {
        log.info("Fetching Jira parent issues for secretName: {}, projectKey: {}", secretName, projectKey);
        return ResponseEntity.ok(jiraIntegrationService.getParentIssuesBySecretName(
                secretName,
                projectKey,
                query,
                startAt,
                maxResults));
    }

    @PostMapping("/connections/{secretName}/fields")
    public ResponseEntity<List<JiraFieldResponse>> getFields(
            @PathVariable @NotBlank String secretName) {
        log.info("Fetching Jira fields for secretName: {}", secretName);
        return ResponseEntity.ok(jiraIntegrationService.getFieldsBySecretName(secretName));
    }

    @PostMapping("/connections/{secretName}/projects/{projectKey}/meta-fields")
    public ResponseEntity<List<JiraFieldResponse>> getProjectMetaFields(
            @PathVariable @NotBlank String secretName,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey,
            @RequestParam(value = "issueTypeId", required = false) String issueTypeId) {

        log.info("Fetching Jira meta-fields for secretName: {}, projectKey: {}, issueTypeId: {}",
            secretName, projectKey, issueTypeId);

        List<JiraFieldResponse> fields = jiraIntegrationService.getProjectMetaFieldsBySecretName(
            secretName, projectKey, issueTypeId);
        return ResponseEntity.ok(fields);
    }

    @PostMapping("/connections/{secretName}/fields/{fieldId}")
    public ResponseEntity<JiraFieldDetailResponse> getFieldDetails(
            @PathVariable @NotBlank String secretName,
            @PathVariable @NotBlank(message = "Field ID must not be blank") String fieldId,
            @RequestParam(value = "projectKey", required = false) String projectKey,
            @RequestParam(value = "issueTypeId", required = false) String issueTypeId) {
        log.info("Fetching Jira field details for secretName: {}, fieldId: {}", secretName, fieldId);
        JiraFieldDetailResponse details = jiraIntegrationService
                .getFieldDetailsBySecretName(secretName, fieldId, projectKey, issueTypeId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/connections/{secretName}/teams")
    public ResponseEntity<List<JiraTeamResponse>> getTeams(
            @PathVariable @NotBlank String secretName,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startAt", required = false) Integer startAt,
            @RequestParam(value = "maxResults", required = false) Integer maxResults) {
        log.info("Fetching Jira teams for secretName: {}", secretName);
        return ResponseEntity.ok(
                jiraIntegrationService.getTeamsBySecretName(secretName, query, startAt, maxResults));
    }

    @PostMapping("/connections/{secretName}/sprints")
    public ResponseEntity<List<JiraSprintResponse>> getSprints(
            @PathVariable @NotBlank String secretName,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "projectKey", required = false) String projectKey,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "startAt", required = false) Integer startAt,
            @RequestParam(value = "maxResults", required = false) Integer maxResults) {
        log.info("Fetching Jira sprints for secretName: {} (boardId={}, projectKey={})",
                secretName, boardId, projectKey);
        return ResponseEntity.ok(jiraIntegrationService.getSprintsBySecretName(
                secretName, boardId, projectKey, state, startAt, maxResults));
    }

}
