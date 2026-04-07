package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueReferenceResponse;
import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraResponseMapper {

    private final ObjectMapper objectMapper;

    private <T> List<T> mapArray(
            JsonNode node,
            Class<T> targetClass,
            Comparator<T> comparator,
            String context) {

        if (node == null || !node.isArray()) {
            log.warn("{} response is not an array", context);
            return List.of();
        }

        List<T> result = StreamSupport.stream(node.spliterator(), false)
                .map(n -> objectMapper.convertValue(n, targetClass))
                .toList();

        if (comparator != null) {
            result = result.stream().sorted(comparator).toList();
        }

        log.debug("Mapped {} {}", result.size(), context);
        return result;
    }

    public List<JiraProjectResponse> mapProjects(JsonNode rootNode) {

        JsonNode valuesNode = rootNode.path("values");
        if (!valuesNode.isArray()) {
            log.warn("Missing or invalid 'values' node in projects response");
            return List.of();
        }

        return StreamSupport.stream(valuesNode.spliterator(), false)
                .map(node -> {
                    if (node instanceof ObjectNode objectNode) {
                        ObjectNode sanitized = objectNode.deepCopy();
                        JsonNode leadNode = sanitized.path("lead");
                        if (leadNode.isObject()) {
                            sanitized.put("lead", leadNode.path("displayName").asText(null));
                        }
                        return objectMapper.convertValue(sanitized, JiraProjectResponse.class);
                    }

                    return objectMapper.convertValue(node, JiraProjectResponse.class);
                })
                .sorted(Comparator.comparing(
                        JiraProjectResponse::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<JiraUserResponse> mapUsers(JsonNode rootNode) {
        return mapArray(
                rootNode,
                JiraUserResponse.class,
                Comparator.comparing(
                        JiraUserResponse::getDisplayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
                "users");
    }

    public List<JiraIssueTypeResponse> mapIssueTypes(JsonNode rootNode) {
        return mapArray(
                rootNode,
                JiraIssueTypeResponse.class,
                Comparator.comparing(
                        JiraIssueTypeResponse::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
                "issue types");
    }

    public List<JiraIssueReferenceResponse> mapParentIssues(JsonNode rootNode) {
        JsonNode issuesNode = rootNode.path("issues");
        if (!issuesNode.isArray()) {
            log.warn("Missing or invalid 'issues' node in parent issues response");
            return List.of();
        }

        return StreamSupport.stream(issuesNode.spliterator(), false)
                .map(issueNode -> JiraIssueReferenceResponse.builder()
                        .key(issueNode.path("key").asText(null))
                        .summary(issueNode.path("fields").path("summary").asText(null))
                        .issueType(issueNode.path("fields").path("issuetype").path("name").asText(null))
                        .build())
                .filter(issue -> issue.getKey() != null && !issue.getKey().isBlank())
                .toList();
    }

    public List<JiraFieldResponse> mapFields(JsonNode rootNode) {

        if (!rootNode.isArray()) {
            log.warn("Fields response is not an array");
            return List.of();
        }

        return StreamSupport.stream(rootNode.spliterator(), false)
                .map(node -> {
                    JiraFieldResponse field = objectMapper.convertValue(node, JiraFieldResponse.class);

                    JsonNode schemaNode = node.path("schema");
                    if (schemaNode.isObject()) {
                        field.setSchema(schemaNode.path("type").asText(null));
                        field.setSchemaDetails(
                                objectMapper.convertValue(
                                        schemaNode,
                                        JiraFieldResponse.JiraFieldSchema.class));
                    }
                    return field;
                })
                .toList();
    }


    public List<JiraFieldResponse> mapCreateMetaFields(
            final JsonNode rootNode,
            final String issueTypeId) {

        if (rootNode == null || rootNode.isNull()) {
            return List.of();
        }

        String normalizedIssueTypeId =
                issueTypeId != null && !issueTypeId.isBlank() ? issueTypeId.trim() : null;

        JsonNode projectsNode = rootNode.path("projects");
        if (!projectsNode.isArray() || projectsNode.isEmpty()) {
            log.warn("Missing or invalid 'projects' node in project meta fields response");
            return List.of();
        }

        Map<String, JiraFieldResponse> uniqueFieldsById = new LinkedHashMap<>();
        for (JsonNode projectNode : projectsNode) {
            collectProjectIssueTypeFields(projectNode, normalizedIssueTypeId, uniqueFieldsById);
        }

        return uniqueFieldsById.values().stream().toList();
    }

    private void collectProjectIssueTypeFields(
            final JsonNode projectNode,
            final String issueTypeId,
            final Map<String, JiraFieldResponse> uniqueFieldsById) {

        JsonNode issueTypesNode = projectNode.path("issuetypes");
        if (!issueTypesNode.isArray() || issueTypesNode.isEmpty()) {
            return;
        }

        for (JsonNode issueTypeNode : issueTypesNode) {
            if (!shouldIncludeIssueType(issueTypeNode, issueTypeId)) {
                continue;
            }
            collectIssueTypeFields(issueTypeNode, uniqueFieldsById);
        }
    }

    private boolean shouldIncludeIssueType(final JsonNode issueTypeNode, final String issueTypeId) {
        return issueTypeId == null || issueTypeId.equals(issueTypeNode.path("id").asText());
    }

    private void collectIssueTypeFields(
            final JsonNode issueTypeNode,
            final Map<String, JiraFieldResponse> uniqueFieldsById) {

        JsonNode fieldsNode = issueTypeNode.path("fields");
        if (!fieldsNode.isObject() || fieldsNode.isEmpty()) {
            return;
        }

        fieldsNode.fields().forEachRemaining(entry -> {
            JiraFieldResponse mappedField = mapCreateMetaField(entry.getKey(), entry.getValue());
            uniqueFieldsById.putIfAbsent(mappedField.getId(), mappedField);
        });
    }

    private JiraFieldResponse mapCreateMetaField(final String fieldId, final JsonNode fieldNode) {
        JiraFieldResponse field = objectMapper.convertValue(fieldNode, JiraFieldResponse.class);

        if (field.getId() == null || field.getId().isBlank()) {
            field.setId(fieldId);
        }
        if (field.getKey() == null || field.getKey().isBlank()) {
            field.setKey(fieldId);
        }

        JsonNode schemaNode = fieldNode.path("schema");
        if (schemaNode.isObject()) {
            field.setSchema(schemaNode.path("type").asText(null));
            field.setSchemaDetails(
                    objectMapper.convertValue(
                            schemaNode,
                            JiraFieldResponse.JiraFieldSchema.class));
        }

        return field;
    }

    public boolean hasValidData(JsonNode rootNode) {

        if (rootNode == null || rootNode.isNull()) {
            return false;
        }

        JsonNode totalNode = rootNode.path("total");
        if (totalNode.isNumber()) {
            return totalNode.asInt() > 0;
        }

        JsonNode valuesNode = rootNode.path("values");
        if (valuesNode.isArray()) {
            return !valuesNode.isEmpty();
        }

        return rootNode.isArray() && !rootNode.isEmpty();
    }

    public boolean hasValidProjectMetaFieldsData(JsonNode rootNode) {

        if (rootNode == null || rootNode.isNull()) {
            return false;
        }

        JsonNode projectsNode = rootNode.path("projects");
        if (!projectsNode.isArray() || projectsNode.isEmpty()) {
            return false;
        }

        for (JsonNode projectNode : projectsNode) {
            JsonNode issueTypesNode = projectNode.path("issuetypes");
            if (!issueTypesNode.isArray() || issueTypesNode.isEmpty()) {
                continue;
            }
            for (JsonNode issueTypeNode : issueTypesNode) {
                JsonNode fieldsNode = issueTypeNode.path("fields");
                if (fieldsNode.isObject() && fieldsNode.size() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<JiraTeamResponse> mapTeams(JsonNode rootNode) {

        if (rootNode == null || rootNode.isNull()) {
            return List.of();
        }

        JsonNode arrayNode = null;

        if (rootNode.has("values") && rootNode.get("values").isArray()) {
            arrayNode = rootNode.get("values");
        } else if (rootNode.has("data") && rootNode.get("data").isArray()) {
            arrayNode = rootNode.get("data");
        } else if (rootNode.has("results") && rootNode.get("results").isArray()) {
            arrayNode = rootNode.get("results");
        }

        // ✅ If no valid array found, return empty list safely
        if (arrayNode == null || !arrayNode.isArray()) {
            log.warn("No team array found in Jira response: {}", rootNode);
            return List.of();
        }

        return mapArray(
                arrayNode,
                JiraTeamResponse.class,
                Comparator.comparing(
                        JiraTeamResponse::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
                "teams");
    }

    public List<JiraSprintResponse> mapSprints(JsonNode rootNode) {
        // Align null-safety with mapTeams
        if (rootNode == null || rootNode.isNull()) {
            return List.of();
        }

        JsonNode valuesNode = rootNode.path("values");
        JsonNode arrayNode = valuesNode.isArray() ? valuesNode : (rootNode.isArray() ? rootNode : null);

        if (arrayNode == null) {
            log.warn("No sprint array found in Jira response: {}", rootNode);
            return List.of();
        }

        return mapArray(
                arrayNode,
                JiraSprintResponse.class,
                Comparator.comparing(
                        JiraSprintResponse::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
                "sprints");
    }
}
