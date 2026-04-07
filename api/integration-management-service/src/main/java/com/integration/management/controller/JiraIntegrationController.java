package com.integration.management.controller;

import com.integration.execution.contract.rest.response.jira.JiraFieldDetailResponse;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import com.integration.management.service.JiraIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;

/**
 * Controller for Jira integration operations including fetching Jira resources
 * like projects, users, fields, and issue types using existing connections.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/jira")
@PreAuthorize("hasRole('feature_jira_webhook')")
@Validated
public class JiraIntegrationController {

    private final JiraIntegrationService jiraIntegrationService;

    @PostMapping("/connections/{connectionId}/projects")
    public ResponseEntity<List<JiraProjectResponse>> getProjectsByConnectionId(
            @PathVariable UUID connectionId, @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching projects for connection ID: {}", connectionId);
        List<JiraProjectResponse> projects = jiraIntegrationService.getProjectsByConnectionId(connectionId, tenantId);
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/connections/{connectionId}/projects/{projectKey}/users")
    public ResponseEntity<List<JiraUserResponse>> getProjectUsersByConnectionId(
            @PathVariable UUID connectionId,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey,
            @RequestAttribute(X_TENANT_ID) String tenantId
    ) {
        if (projectKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Fetching users for connection ID: {} and project: {}", connectionId, projectKey);
        List<JiraUserResponse> users = jiraIntegrationService.getProjectUsersByConnectionId(connectionId, tenantId,
                projectKey);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/connections/{connectionId}/projects/{projectKey}/issue-types")
    public ResponseEntity<List<JiraIssueTypeResponse>> getProjectIssueTypesByConnectionId(
            @PathVariable UUID connectionId,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        if (projectKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Fetching issue types for connection ID: {} and project: {}", connectionId, projectKey);
        List<JiraIssueTypeResponse> issueTypes = jiraIntegrationService
                .getProjectIssueTypesByConnectionId(connectionId, tenantId, projectKey);
        return ResponseEntity.ok(issueTypes);
    }

    @PostMapping("/connections/{connectionId}/projects/{projectKey}/parent-issues")
    public ResponseEntity<List<JiraIssueReferenceResponse>> getProjectParentIssuesByConnectionId(
            @PathVariable UUID connectionId,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startAt", required = false) Integer startAt,
            @RequestParam(value = "maxResults", required = false) Integer maxResults) {
        if (projectKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Fetching parent issues for connection ID: {} and project: {}", connectionId, projectKey);
        List<JiraIssueReferenceResponse> parentIssues = jiraIntegrationService
                .getProjectParentIssuesByConnectionId(connectionId, tenantId, projectKey, query, startAt, maxResults);
        return ResponseEntity.ok(parentIssues);
    }

    @PostMapping("/connections/{connectionId}/fields")
    public ResponseEntity<List<JiraFieldResponse>> getFieldsByConnectionId(
            @PathVariable UUID connectionId, @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching fields for connection ID: {}", connectionId);
        List<JiraFieldResponse> fields = jiraIntegrationService.getFieldsByConnectionId(connectionId, tenantId);
        return ResponseEntity.ok(fields);
    }

    @PostMapping("/connections/{connectionId}/fields/{fieldId}")
    public ResponseEntity<JiraFieldDetailResponse> getFieldDetailsByConnectionId(
            @PathVariable("connectionId") UUID connectionId,
            @PathVariable("fieldId") @NotBlank(message = "Field ID must not be blank") String fieldId,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestParam(value = "projectKey", required = false) String projectKey,
            @RequestParam(value = "issueTypeId", required = false) String issueTypeId) {
        if (fieldId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        JiraFieldDetailResponse details = jiraIntegrationService
                .getFieldDetailsByConnectionId(connectionId, tenantId, fieldId, projectKey, issueTypeId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/connections/{connectionId}/teams")
    public ResponseEntity<List<JiraTeamResponse>> getTeamsByConnectionId(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startAt", required = false) Integer startAt,
            @RequestParam(value = "maxResults", required = false) Integer maxResults) {
        log.info("Fetching teams for connection ID: {}", connectionId);
        List<JiraTeamResponse> teams = jiraIntegrationService
                .getTeamsByConnectionId(connectionId, tenantId, query, startAt, maxResults);
        return ResponseEntity.ok(teams);
    }

    @PostMapping("/connections/{connectionId}/sprints")
    public ResponseEntity<List<JiraSprintResponse>> getSprintsByConnectionId(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "projectKey", required = false) String projectKey,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "startAt", required = false) Integer startAt,
            @RequestParam(value = "maxResults", required = false) Integer maxResults) {
        log.info("Fetching sprints for connection ID: {} (boardId={}, projectKey={})",
                connectionId, boardId, projectKey);
        List<JiraSprintResponse> sprints = jiraIntegrationService
                .getSprintsByConnectionId(connectionId, tenantId, boardId, projectKey, state, startAt, maxResults);
        return ResponseEntity.ok(sprints);
    }

    @PostMapping("/connections/{connectionId}/projects/{projectKey}/meta-fields")
    public ResponseEntity<List<JiraFieldResponse>> getProjectMetaFieldsByConnectionId(
            @PathVariable UUID connectionId,
            @PathVariable @NotBlank(message = "Project key must not be blank") String projectKey,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestParam(value = "issueTypeId", required = false) String issueTypeId) {
        log.info("Fetching meta fields for connection ID: {} (projectKey={})", connectionId, projectKey);
        List<JiraFieldResponse> fields = jiraIntegrationService
                .getProjectMetaFieldsByConnectionId(connectionId, tenantId, projectKey, issueTypeId);
        return ResponseEntity.ok(fields);
    }
}
