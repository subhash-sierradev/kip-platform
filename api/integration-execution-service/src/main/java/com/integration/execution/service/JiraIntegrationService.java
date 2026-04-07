package com.integration.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraIntegrationService {

    private final JiraApiClient jiraApiClient;
    private final JiraResponseMapper responseMapper;
    private final JiraApiProperties jiraApiProperties;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "jiraProjectsByConnectionCache", key = "#secretName")
    public List<JiraProjectResponse> getProjectsBySecretName(final String secretName) {
        return executeJiraCall("fetching projects", () -> {
            JsonNode node = jiraApiClient.searchProjects(secretName);
            return responseMapper.hasValidData(node)
                    ? responseMapper.mapProjects(node)
                    : List.of();
        });
    }

    @Cacheable(value = "jiraUsersByConnectionCache", key = "#secretName + ':' + #projectKey")
    public List<JiraUserResponse> getProjectUsersBySecretName(
            final String secretName, final String projectKey) {
        return executeJiraCall("fetching project users", () -> {
            JsonNode node = jiraApiClient.getAssignableUsers(secretName, projectKey);
            return responseMapper.hasValidData(node)
                    ? responseMapper.mapUsers(node)
                    : List.of();
        });
    }

    @Cacheable(value = "jiraIssueTypesByConnectionCache", key = "#secretName + ':' + #projectKey")
    public List<JiraIssueTypeResponse> getProjectIssueTypesBySecretName(
            final String secretName, final String projectKey) {
        return executeJiraCall("fetching issue types", () -> {
            JsonNode node = jiraApiClient.getProjectStatuses(projectKey, secretName);
            return responseMapper.hasValidData(node)
                    ? responseMapper.mapIssueTypes(node)
                    : List.of();
        });
    }

    @Cacheable(
            value = "jiraParentIssuesByConnectionCache",
            key = "#secretName + ':' + #projectKey + ':' + (#startAt == null ? 0 : #startAt)"
                + " + ':' + (#query == null ? '' : #query)"
                + " + ':' + (#maxResults == null ? 100 : #maxResults)"
    )
    public List<JiraIssueReferenceResponse> getParentIssuesBySecretName(
            final String secretName,
            final String projectKey,
            final String query,
            final Integer startAt,
            final Integer maxResults) {
        return executeJiraCall("fetching parent issues", () -> {
            JsonNode node = jiraApiClient.searchParentIssues(secretName, projectKey, query, startAt, maxResults);
            List<JiraIssueReferenceResponse> issues = responseMapper.mapParentIssues(node);
            return issues != null ? issues : List.of();
        });
    }

    @Cacheable(value = "jiraFieldsByConnectionCache", key = "#secretName")
    public List<JiraFieldResponse> getFieldsBySecretName(final String secretName) {
        return executeJiraCall("fetching fields", () -> {
            JsonNode node = jiraApiClient.getFields(secretName);
            if (!responseMapper.hasValidData(node)) {
                return List.of();
            }

            return responseMapper.mapFields(node).stream()
                    .filter(f -> Boolean.TRUE.equals(f.getNavigable()))
                    .sorted(
                            Comparator
                                    .comparing((JiraFieldResponse f) -> Boolean.TRUE.equals(f.getCustom()))
                                    .thenComparing(
                                            f -> Optional.ofNullable(f.getName()).orElse(""),
                                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
        });
    }

    @Cacheable(
        value = "jiraProjectMetaFieldsByConnectionCache",
        key = "#secretName + ':' + #projectKey + ':' + (#issueTypeId == null ? '' : #issueTypeId)"
    )
    public List<JiraFieldResponse> getProjectMetaFieldsBySecretName(
            final String secretName,
            final String projectKey,
            final String issueTypeId) {

        return executeJiraCall("fetching project meta fields", () -> {
            JsonNode node = jiraApiClient.getProjectMetaFields(projectKey, secretName);
            if (!responseMapper.hasValidProjectMetaFieldsData(node)) {
                return List.of();
            }

            return responseMapper.mapCreateMetaFields(node, issueTypeId).stream()
                .sorted(
                        Comparator
                                .comparing((JiraFieldResponse f) -> Boolean.TRUE.equals(f.getCustom()))
                                .thenComparing(
                                        f -> Optional.ofNullable(f.getName()).orElse(""),
                                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        });
    }

    @Cacheable(
            value = "jiraFieldDetailsByConnectionCache",
            key = "#secretName + ':' + #fieldId + ':' + (#projectKey == null ? '' : #projectKey)"
                    + " + ':' + (#issueTypeId == null ? '' : #issueTypeId)"
    )
    public JiraFieldDetailResponse getFieldDetailsBySecretName(
            final String secretName,
            final String fieldId,
            final String projectKey,
            final String issueTypeId) {

        return executeJiraCall("fetching field details", () -> {
            JiraFieldResponse baseField = getFieldsBySecretName(secretName)
                    .stream()
                    .filter(f -> fieldId.equals(f.getId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid Jira field id: " + fieldId));

            JiraFieldDetailResponse.JiraFieldDetailResponseBuilder builder =
                    JiraFieldDetailResponse.builder()
                            .id(fieldId)
                            .name(baseField.getName())
                            .key(baseField.getKey())
                            .schema(baseField.getSchema())
                            .custom(baseField.getCustom())
                            .orderable(baseField.getOrderable())
                            .navigable(baseField.getNavigable())
                            .searchable(baseField.getSearchable())
                            .schemaDetails(baseField.getSchemaDetails());

            String dataType = baseField.getSchemaDetails() != null
                    ? baseField.getSchemaDetails().getType()
                    : null;

            if (projectKey != null && !projectKey.isBlank()) {
                populateConstraintsAndAllowedValues(
                        secretName, projectKey, issueTypeId, fieldId, builder);
                populateUsersIfRequired(secretName, projectKey, dataType, builder);
            }

            return builder.dataType(dataType).build();
        });
    }

    @Cacheable(
            value = "jiraTeamsByConnectionCache",
            key = "T(String).format('%s:%s:%s:%s', #secretName, "
                    + "(#query ?: ''), (#startAt ?: 0), (#maxResults ?: 20))"
    )
    public List<JiraTeamResponse> getTeamsBySecretName(
            final String secretName,
            final String query,
            final Integer startAt,
            final Integer maxResults) {
        return executeJiraCall("fetching teams", () -> {
            JsonNode node = jiraApiClient.searchTeams(secretName, query, startAt, maxResults);
            return responseMapper.hasValidData(node)
                    ? responseMapper.mapTeams(node)
                    : List.of();
        });
    }

    @Cacheable(
            value = "jiraSprintsByConnectionCache",
            key = "T(String).format('%s:%s:%s:%s:%s:%s', #secretName, "
                    + "(#boardId ?: ''), (#projectKey ?: ''), (#state ?: ''), "
                    + "(#startAt ?: ''), (#maxResults ?: ''))"
    )
    public List<JiraSprintResponse> getSprintsBySecretName(
            final String secretName,
            final Long boardId,
            final String projectKey,
            final String state,
            final Integer startAt,
            final Integer maxResults) {
        return executeJiraCall("fetching sprints", () -> {
            Long resolvedBoardId = boardId;
            if (resolvedBoardId == null && projectKey != null && !projectKey.isBlank()) {
                JsonNode boardsNode = jiraApiClient.getBoardsByProject(secretName, projectKey);
                JsonNode values = boardsNode.path("values");
                if (values.isArray() && !values.isEmpty()) {
                    resolvedBoardId = values.get(0).path("id").asLong();
                }
            }
            if (resolvedBoardId == null) {
                return List.of();
            }
            JsonNode sprintsNode = jiraApiClient.getSprintsByBoard(
                    secretName, resolvedBoardId, startAt, maxResults, state);
            return responseMapper.hasValidData(sprintsNode)
                    ? responseMapper.mapSprints(sprintsNode)
                    : List.of();
        });
    }

    private void populateConstraintsAndAllowedValues(
            final String secretName,
            final String projectKey,
            final String issueTypeId,
            final String fieldId,
            final JiraFieldDetailResponse.JiraFieldDetailResponseBuilder builder) {

        String endpoint = UriComponentsBuilder
                .fromPath(jiraApiProperties.getPaths().getIssueCreate())
                .queryParam("projectKeys", encode(projectKey))
                .queryParam("expand", "projects.issuetypes.fields")
                .build()
                .toString();

        JsonNode meta = jiraApiClient.get(endpoint, secretName);
        if (meta == null || !meta.has("projects")) return;

        for (JsonNode project : meta.get("projects")) {
            for (JsonNode it : project.path("issuetypes")) {
                if (issueTypeId != null && !issueTypeId.equals(it.path("id").asText())) {
                    continue;
                }
                JsonNode fieldNode = it.path("fields").path(fieldId);
                if (fieldNode.isMissingNode()) continue;

                builder.required(fieldNode.path("required").asBoolean(false));
                if (fieldNode.has("allowedValues")) {
                    List<Map<String, Object>> allowed =
                            objectMapper.convertValue(fieldNode.get("allowedValues"), List.class);
                    builder.allowedValues(allowed);
                }
                return;
            }
        }
    }

    private void populateUsersIfRequired(
            final String secretName,
            final String projectKey,
            final String dataType,
            final JiraFieldDetailResponse.JiraFieldDetailResponseBuilder builder) {

        if (!"user".equalsIgnoreCase(dataType) && !"array".equalsIgnoreCase(dataType)) {
            return;
        }

        List<Map<String, Object>> users = getProjectUsersBySecretName(secretName, projectKey)
                .stream()
                .map(u -> {
                    Map<String, Object> userMap = new HashMap<>();
                    if (u.getAccountId() != null) userMap.put("accountId", u.getAccountId());
                    if (u.getDisplayName() != null) userMap.put("displayName", u.getDisplayName());
                    return userMap;
                })
                .filter(map -> !map.isEmpty())
                .collect(Collectors.toList());
        builder.users(users);
    }

    private String encode(final String value) {
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
    }

    private <T> T executeJiraCall(final String action, final Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            log.error("Jira API failed while {}: {}", action, ex.getMessage(), ex);
            throw new RuntimeException("Jira API failed while " + action, ex);
        }
    }

}
