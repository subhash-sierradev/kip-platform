package com.integration.execution.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.JiraApiClient;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.mapper.JiraFieldMappingResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraWebhookProcessor {

    private static final String ISSUE_TYPE_FIELD_ID = "issuetype";
    private static final String PARENT_FIELD_ID = "parent";
    private static final String SUBTASK_PARENT_REQUIRED_ERROR =
            "Parent field is required for Subtask issue type and was not resolved from mappings";
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("([A-Z][A-Z0-9]*-\\d+)");
    private final JiraFieldMappingResolver jiraFieldMappingResolver;
    private final JiraApiClient jiraApiClient;
    private final ObjectMapper objectMapper;

    public JiraWebhookExecutionResult processWebhookExecution(final JiraWebhookExecutionCommand cmd) {
        log.info("Processing webhook execution for webhook ID: {} (payload length: {})",
                cmd.getWebhookId(),
                cmd.getIncomingPayload() != null ? cmd.getIncomingPayload().length() : 0);

        try {
            // 1. Validate incoming payload
            if (!StringUtils.hasText(cmd.getIncomingPayload())) {
                log.error("Jira webhook payload is null or empty for webhook ID: {}", cmd.getWebhookId());
                return JiraWebhookExecutionResult.failure(cmd.getTriggerEventId(),
                        "Jira webhook payload cannot be null or empty");
            }

            // 2. Parse and extract Jira issue information from payload
            JsonNode payloadJson = parsePayload(cmd.getIncomingPayload(), cmd.getWebhookId());

            // 3. Process field mappings according to configuration
            Map<String, Object> processedFields = jiraFieldMappingResolver.processAllFieldMappings(
                    cmd.getFieldMappings(),
                    cmd.getWebhookId(),
                    payloadJson);

            processedFields = new HashMap<>(processedFields);
            normalizeParentField(processedFields);

            String subtaskValidationError = validateSubtaskParent(processedFields, cmd.getFieldMappings());
            if (subtaskValidationError != null) {
                log.error("Subtask validation failed for webhook ID {}: {}",
                        cmd.getWebhookId(), subtaskValidationError);
                return JiraWebhookExecutionResult.failure(cmd.getTriggerEventId(), subtaskValidationError);
            }

            // 4. Build transformed payload for target system
            String transformedPayload = buildTransformedPayload(processedFields);
            log.debug("Transformed payload for webhook ID {}: {}", cmd.getWebhookId(), transformedPayload);
            log.info("Built transformed payload for webhook ID {} (payload length: {})", cmd.getWebhookId(),
                    transformedPayload != null ? transformedPayload.length() : 0);
            // 5. Send to Jira
            ApiResponse result = jiraApiClient.sendToJira(cmd.getConnectionSecretName(), transformedPayload);

            log.info("Successfully processed webhook event for ID: {} with {} field mappings",
                    cmd.getWebhookId(), processedFields.size());

            return JiraWebhookExecutionResult.builder()
                    .triggerEventId(cmd.getTriggerEventId())
                    .success(result.success())
                    .responseStatusCode(result.statusCode())
                    .responseBody(result.message())
                    .transformedPayload(transformedPayload)
                    .jiraIssueUrl(result.success()
                            ? calculateJiraIssueUrlFromResponse(result.message())
                            : null)
                    .build();

        } catch (Exception e) {
            log.error("Error processing webhook event for ID: {}", cmd.getWebhookId(), e);
            return JiraWebhookExecutionResult.failure(cmd.getTriggerEventId(), e.getMessage());
        }
    }

    private JsonNode parsePayload(final String payload, final String webhookId) {
        try {
            JsonNode payloadJson = objectMapper.readTree(payload);
            log.debug("Successfully parsed webhook payload for webhook ID: {}", webhookId);
            return payloadJson;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON payload for webhook ID: {}", webhookId, e);
            throw new RuntimeException("Invalid JSON payload", e);
        }
    }

    private String buildTransformedPayload(final Map<String, Object> processedFields) {
        try {
            Map<String, Object> jiraPayload = Map.of("fields", processedFields);
            return objectMapper.writeValueAsString(jiraPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transformed payload: {}", e.getMessage());
            throw new RuntimeException("Failed to build transformed payload", e);
        }
    }

    private void normalizeParentField(final Map<String, Object> processedFields) {

        Object parentValue = processedFields.get(PARENT_FIELD_ID);

        if (parentValue == null) {
            processedFields.remove(PARENT_FIELD_ID);
            return;
        }

        // Handle string values
        if (parentValue instanceof String parentString) {

            String extractedKey = extractJiraIssueKey(parentString);
            String normalizedParent = parentString.trim();

            if (StringUtils.hasText(extractedKey)) {
                processedFields.put(PARENT_FIELD_ID, Map.of("key", extractedKey));
            } else if (isValidJiraIssueId(normalizedParent)) {
                processedFields.put(PARENT_FIELD_ID, Map.of("id", normalizedParent));
            } else {
                processedFields.remove(PARENT_FIELD_ID);
            }

            return;
        }

        // Handle object values
        if (parentValue instanceof Map<?, ?> parentMap) {

            Object keyObj = parentMap.get("key");
            Object idObj = parentMap.get("id");

            String key = keyObj instanceof String ? extractJiraIssueKey((String) keyObj) : null;
            String id = idObj instanceof String ? ((String) idObj).trim() : null;

            if (StringUtils.hasText(key)) {
                processedFields.put(PARENT_FIELD_ID, Map.of("key", key));
                return;
            }

            if (StringUtils.hasText(id) && isValidJiraIssueId(id)) {
                processedFields.put(PARENT_FIELD_ID, Map.of("id", id));
                return;
            }

            processedFields.remove(PARENT_FIELD_ID);
            return;
        }

        // Remove invalid types
        processedFields.remove(PARENT_FIELD_ID);
    }

    private String extractJiraIssueKey(String input) {

        if (!StringUtils.hasText(input)) {
            return null;
        }

        String normalized = input.trim().toUpperCase();

        Matcher matcher = ISSUE_KEY_PATTERN.matcher(normalized);

        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isValidJiraIssueId(final String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        // Jira issue ID is typically numeric
        return id.matches("^\\d+$");
    }

    private String calculateJiraIssueUrlFromResponse(final String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);

            String self = json.path("self").asText(null);
            String key = json.path("key").asText(null);

            // We only expose a friendly browse URL when we have a real issue key.
            // If the response doesn't contain a key (or isn't the expected JSON), keep it null.
            if (!StringUtils.hasText(self) || !StringUtils.hasText(key)) {
                return null;
            }

            int idx = self.indexOf("/rest/");
            String baseUrl = (idx > 0) ? self.substring(0, idx) : self;
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

            return baseUrl + "/browse/" + key.trim();
        } catch (Exception e) {
            log.error("Failed to extract Jira issue URL: {}", e.getMessage());
            return null;
        }
    }

    private String validateSubtaskParent(
            final Map<String, Object> processedFields,
            final List<JiraFieldMappingDto> fieldMappings) {
        boolean isSubtask = isSubtaskIssueType(processedFields.get(ISSUE_TYPE_FIELD_ID), fieldMappings);
        if (!isSubtask) {
            return null;
        }

        Object parentValue = processedFields.get(PARENT_FIELD_ID);

        // Only enforce parent requirement if parent mapping exists in configurations
        boolean hasParentMapping = fieldMappings != null && fieldMappings.stream()
                .anyMatch(mapping -> PARENT_FIELD_ID.equalsIgnoreCase(mapping.getJiraFieldId()));
        if (!hasParentMapping) {
            // Parent is not mapped, so it's not required
            return null;
        }

        if (hasValidParent(parentValue)) {
            return null;
        }

        return SUBTASK_PARENT_REQUIRED_ERROR;
    }

    private boolean isSubtaskIssueType(final Object issueTypeValue, final List<JiraFieldMappingDto> fieldMappings) {
        if (hasSubtaskMetadata(fieldMappings)) {
            return true;
        }

        if (issueTypeValue instanceof Map<?, ?> issueTypeMap) {
            Object name = issueTypeMap.get("name");
            if (name instanceof String issueTypeName) {
                return isSubtaskName(issueTypeName);
            }
        }

        if (issueTypeValue instanceof String issueTypeName) {
            return isSubtaskName(issueTypeName);
        }

        return false;
    }

    private boolean hasSubtaskMetadata(final List<JiraFieldMappingDto> fieldMappings) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return false;
        }

        return fieldMappings.stream()
                .filter(mapping -> ISSUE_TYPE_FIELD_ID.equalsIgnoreCase(mapping.getJiraFieldId()))
                .map(JiraFieldMappingDto::getMetadata)
                .filter(metadata -> metadata != null && !metadata.isEmpty())
                .map(metadata -> metadata.get("subtask"))
                .anyMatch(value -> value instanceof Boolean boolValue && boolValue);
    }

    private boolean isSubtaskName(final String issueTypeName) {
        String normalized = issueTypeName.trim().toLowerCase().replace(" ", "");
        return "subtask".equals(normalized) || "sub-task".equals(normalized);
    }

    private boolean hasValidParent(final Object parentValue) {
        if (parentValue instanceof Map<?, ?> parentMap) {
            Object keyObj = parentMap.get("key");
            if (keyObj instanceof String key && StringUtils.hasText(extractJiraIssueKey(key))) {
                return true;
            }

            Object idObj = parentMap.get("id");
            return idObj instanceof String id && isValidJiraIssueId(id.trim());
        }

        if (parentValue instanceof String parentString) {
            String normalizedParent = parentString.trim();
            return StringUtils.hasText(extractJiraIssueKey(normalizedParent))
                    || isValidJiraIssueId(normalizedParent);
        }

        return false;
    }
}
