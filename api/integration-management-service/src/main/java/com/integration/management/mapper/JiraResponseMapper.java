package com.integration.management.mapper;

//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.integration.execution.contract.rest.response.jira.JiraFieldResponse;
//import com.integration.execution.contract.rest.response.jira.JiraIssueTypeResponse;
//import com.integration.execution.contract.rest.response.jira.JiraProjectResponse;
//import com.integration.execution.contract.rest.response.jira.JiraSprintResponse;
//import com.integration.execution.contract.rest.response.jira.JiraTeamResponse;
//import com.integration.execution.contract.rest.response.jira.JiraUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

//import java.util.Comparator;
//import java.util.List;
//import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraResponseMapper {

//    private final ObjectMapper objectMapper;
//
//    private <T> List<T> mapArray(
//            JsonNode node,
//            Class<T> targetClass,
//            Comparator<T> comparator,
//            String context) {
//
//        if (node == null || !node.isArray()) {
//            log.warn("{} response is not an array", context);
//            return List.of();
//        }
//
//        List<T> result = StreamSupport.stream(node.spliterator(), false)
//                .map(n -> objectMapper.convertValue(n, targetClass))
//                .toList();
//
//        if (comparator != null) {
//            result = result.stream().sorted(comparator).toList();
//        }
//
//        log.debug("Mapped {} {}", result.size(), context);
//        return result;
//    }

//    public List<JiraProjectResponse> mapProjects(JsonNode rootNode) {
//
//        JsonNode valuesNode = rootNode.path("values");
//        if (!valuesNode.isArray()) {
//            log.warn("Missing or invalid 'values' node in projects response");
//            return List.of();
//        }
//
//        return StreamSupport.stream(valuesNode.spliterator(), false)
//                .map(node -> {
//                    JiraProjectResponse project =
//                            objectMapper.convertValue(node, JiraProjectResponse.class);
//                    JsonNode leadNode = node.path("lead");
//                    if (leadNode.isObject()) {
//                        project.setLead(leadNode.path("displayName").asText(null));
//                    }
//                    return project;
//                })
//                .sorted(Comparator.comparing(
//                        JiraProjectResponse::getName,
//                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
//                .toList();
//    }

//    public List<JiraUserResponse> mapUsers(JsonNode rootNode) {
//        return mapArray(
//                rootNode,
//                JiraUserResponse.class,
//                Comparator.comparing(
//                        JiraUserResponse::getDisplayName,
//                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
//                "users");
//    }

//    public List<JiraIssueTypeResponse> mapIssueTypes(JsonNode rootNode) {
//        return mapArray(
//                rootNode,
//                JiraIssueTypeResponse.class,
//                Comparator.comparing(
//                        JiraIssueTypeResponse::getName,
//                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
//                "issue types");
//    }

//    public List<JiraFieldResponse> mapFields(JsonNode rootNode) {
//
//        if (!rootNode.isArray()) {
//            log.warn("Fields response is not an array");
//            return List.of();
//        }
//
//        return StreamSupport.stream(rootNode.spliterator(), false)
//                .map(node -> {
//                    JiraFieldResponse field =
//                            objectMapper.convertValue(node, JiraFieldResponse.class);
//
//                    JsonNode schemaNode = node.path("schema");
//                    if (schemaNode.isObject()) {
//                        field.setSchema(schemaNode.path("type").asText(null));
//                        field.setSchemaDetails(
//                                objectMapper.convertValue(
//                                        schemaNode,
//                                        JiraFieldResponse.JiraFieldSchema.class));
//                    }
//                    return field;
//                })
//                .toList();
//    }
//
//    public boolean hasValidData(JsonNode rootNode) {
//
//        JsonNode totalNode = rootNode.path("total");
//        if (totalNode.isNumber()) {
//            return totalNode.asInt() > 0;
//        }
//
//        JsonNode valuesNode = rootNode.path("values");
//        if (valuesNode.isArray()) {
//            return !valuesNode.isEmpty();
//        }
//
//        return rootNode.isArray() && !rootNode.isEmpty();
//    }
//
//    public List<JiraTeamResponse> mapTeams(JsonNode rootNode) {
//
//        if (rootNode == null || rootNode.isNull()) {
//            return List.of();
//        }
//
//        JsonNode arrayNode = null;
//
//        if (rootNode.has("values") && rootNode.get("values").isArray()) {
//            arrayNode = rootNode.get("values");
//        } else if (rootNode.has("data") && rootNode.get("data").isArray()) {
//            arrayNode = rootNode.get("data");
//        } else if (rootNode.has("results") && rootNode.get("results").isArray()) {
//            arrayNode = rootNode.get("results");
//        }
//
//        // ✅ If no valid array found, return empty list safely
//        if (arrayNode == null || !arrayNode.isArray()) {
//            log.warn("No team array found in Jira response: {}", rootNode);
//            return List.of();
//        }
//
//        return mapArray(
//                arrayNode,
//                JiraTeamResponse.class,
//                Comparator.comparing(
//                        JiraTeamResponse::getName,
//                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
//                ),
//                "teams"
//        );
//    }
//
//
//    public List<JiraSprintResponse> mapSprints(JsonNode rootNode) {
//        // Align null-safety with mapTeams
//        if (rootNode == null || rootNode.isNull()) {
//            return List.of();
//        }
//
//        JsonNode valuesNode = rootNode.path("values");
//        JsonNode arrayNode = valuesNode.isArray() ? valuesNode : (rootNode.isArray() ? rootNode : null);
//
//        if (arrayNode == null) {
//            log.warn("No sprint array found in Jira response: {}", rootNode);
//            return List.of();
//        }
//
//        return mapArray(
//                arrayNode,
//                JiraSprintResponse.class,
//                Comparator.comparing(
//                        JiraSprintResponse::getName,
//                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
//                ),
//                "sprints");
//    }
}
