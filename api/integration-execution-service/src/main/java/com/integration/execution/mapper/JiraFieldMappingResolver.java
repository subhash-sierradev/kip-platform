package com.integration.execution.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.exception.FieldMappingProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
// Note: Consider refactoring into smaller classes if it grows too much
public class JiraFieldMappingResolver {
    // Pattern for extracting placeholders from templates like {{field.subfield}}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    // Pattern for validating Jira Cloud accountId format (20-255 alphanumeric
    // chars, optional 'accountId:' prefix)
    private static final Pattern JIRA_ACCOUNT_ID_PATTERN = Pattern.compile("^(accountId:)?[a-zA-Z0-9]{20,255}$");
    private final ObjectMapper objectMapper;
    private final JiraJsonPathResolver jiraJsonPathResolver;

    public Map<String, Object> processAllFieldMappings(
            final List<JiraFieldMappingDto> fieldMappings,
            final String webhookId,
            final JsonNode payloadJson) {
        return fieldMappings.stream()
                .map(mapping -> {
                    try {
                        Object processedValue = processFieldMapping(mapping, payloadJson, webhookId);
                        if (processedValue != null) {
                            // Apply Jira-specific field transformations
                            Object jiraFormattedValue = convertToJiraFormat(processedValue, mapping, webhookId);
                            log.debug("Processed field {}: {} for webhook {}",
                                    mapping.getJiraFieldName(), jiraFormattedValue, webhookId);
                            return Map.entry(mapping.getJiraFieldId(), jiraFormattedValue);
                        }
                    } catch (Exception e) {
                        log.error("Failed to process field mapping {} for webhook {}: {}",
                                mapping.getJiraFieldName(), webhookId, e.getMessage(), e);
                    }
                    return null; // skip this mapping if it fails or has null value
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b,
                        LinkedHashMap::new));
    }

    public Object processFieldMapping(JiraFieldMappingDto mapping, JsonNode payloadJson, String webhookId) {
        String template = mapping.getTemplate();
        String defaultValue = mapping.getDefaultValue();
        boolean isRequired = Boolean.TRUE.equals(mapping.getRequired());
        // If no template provided, try to extract the field directly
        if (template == null || template.trim().isEmpty()) {
            // Use default value if available
            if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                return defaultValue;
            }
            // Handle required fields
            if (isRequired) {
                log.error("Required field {} has no value and no default for webhook {}",
                        mapping.getJiraFieldName(), webhookId);
                // create custom exception for better error handling
                throw new FieldMappingProcessingException("Required field " + mapping.getJiraFieldName()
                        + " has no value and no default");
            }
            return null;
        }
        // Process template with placeholders
        String processedValue = processTemplate(template, payloadJson, webhookId);

        // If template processing resulted in empty value
        if (processedValue == null || processedValue.trim().isEmpty()) {
            if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                log.debug("Template resulted in empty value, using default '{}' for field {} in webhook {}",
                        defaultValue, mapping.getJiraFieldName(), webhookId);
                return defaultValue;
            }
            if (isRequired) {
                log.error("Required field {} resulted in empty value after template processing for webhook {}",
                        mapping.getJiraFieldName(), webhookId);
                throw new FieldMappingProcessingException("Required field " + mapping.getJiraFieldName()
                        + " template resulted in empty value for template: " + template);
            }
            return null;
        }
        // Convert to appropriate data type
        return convertToDataType(processedValue, mapping.getDataType(), mapping.getJiraFieldName(), webhookId);
    }

    private String processTemplate(String template, JsonNode payloadJson, String webhookId) {

        String result = template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(0); // Full placeholder including ${}
            String jsonPath = matcher.group(1); // Path without ${}

            try {
                String value = extractValueFromJsonPath(jsonPath, payloadJson);
                if (value != null) {
                    result = result.replace(placeholder, value);
                    log.debug("Replaced placeholder {} with value '{}' for webhook {}",
                            placeholder, value, webhookId);
                } else {
                    log.error("No value found for placeholder {} in webhook {}", placeholder, webhookId);
                    // Keep placeholder if no value found - it will be handled by default value
                    // logic
                    result = result.replace(placeholder, "");
                }
            } catch (Exception e) {
                log.error("Failed to extract value for placeholder {} in webhook {}: {}",
                        placeholder, webhookId, e.getMessage());
                // Remove failed placeholder
                result = result.replace(placeholder, "");
            }
        }
        return result;
    }

    public String extractValueFromJsonPath(String jsonPath, JsonNode jsonNode) {
        return jiraJsonPathResolver.extractValue(jsonPath, jsonNode);
    }

    public Object convertToDataType(String value,
                                    JiraDataType dataType,
                                    String fieldName,
                                    String webhookId) {
        if (value == null || dataType == null) {
            return value;
        }

        try {
            return switch (dataType) {
                case STRING, EMAIL, URL, DATE -> value;
                case NUMBER -> convertToNumber(value, fieldName, webhookId);
                case BOOLEAN -> convertToBoolean(value);
                case ARRAY -> convertToArray(value);
                case OBJECT -> convertToObject(value, fieldName, webhookId);
                case USER -> convertToJiraUserFormat(value, webhookId);
                case MULTIUSER -> convertToArray(value).stream()
                        .map(item -> convertToJiraUserFormat(item, webhookId))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            };
        } catch (Exception e) {
            log.error("Failed to convert value '{}' to type {} for field {} in webhook {}: {}",
                    value, dataType, fieldName, webhookId, e.getMessage(), e);
            return value;
        }
    }

    private Object convertToNumber(String value, String fieldName, String webhookId) {
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to convert '{}' to number for field {} in webhook {}, using as string",
                    value, fieldName, webhookId);
            return value;
        }
    }

    private Boolean convertToBoolean(String value) {
        if (value == null)
            return null;
        String normalized = value.trim();
        if ("1".equals(normalized) || "yes".equalsIgnoreCase(normalized) || "true".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("0".equals(normalized) || "no".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        // fallback to parseBoolean (defaults to false for unknown values)
        return Boolean.parseBoolean(normalized);
    }

    private List<String> convertToArray(String value) {
        if (value == null)
            return List.of();
        List<String> items = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return items.isEmpty() ? List.of(value) : items;
    }

    private Object convertToObject(String value, String fieldName, String webhookId) {
        if (value == null)
            return null;
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse '{}' as JSON object for field {} in webhook {}, using as string",
                    value, fieldName, webhookId, e);
            return value;
        }
    }

    /**
     * Converts field values to Jira-specific format based on field type and
     * requirements.
     * This method handles special Jira field formatting such as project objects,
     * issue types, etc.
     */
    private Object convertToJiraFormat(Object value, JiraFieldMappingDto mapping, String webhookId) {
        if (value == null) {
            return null;
        }
        final String jiraFieldId = mapping.getJiraFieldId();
        final Map<String, Object> metadata = mapping.getMetadata();

        if (isLegacySprintCustomField(mapping)) {
            return convertToJiraSprintFormat(value, webhookId);
        }

        // Handle special custom field types based on metadata (e.g., Team, Sprint)
        Object special = formatSpecialCustomFieldIfAny(value, metadata, webhookId);
        if (special != null) {
            return special;
        }
        // Handle special Jira fields that require object format
        return switch (jiraFieldId.toLowerCase(Locale.ROOT)) {
            case "project" -> convertToJiraProjectFormat(value, webhookId);
            case "issuetype" -> convertToJiraIssueTypeFormat(value, webhookId);
            case "description" -> convertToJiraDescriptionFormat(value, webhookId);
            case "priority" -> convertToJiraPriorityFormat(value, webhookId);
            case "assignee", "reporter" -> convertToJiraUserFormat(value, webhookId);
            default ->
                // For other fields, apply standard data type conversion
                    value;
        };
    }

    private boolean isLegacySprintCustomField(final JiraFieldMappingDto mapping) {
        String jiraFieldId = mapping.getJiraFieldId();
        String jiraFieldName = mapping.getJiraFieldName();

        if (jiraFieldId == null || jiraFieldName == null) {
            return false;
        }

        return jiraFieldId.toLowerCase(Locale.ROOT).startsWith("customfield_")
                && "sprint".equalsIgnoreCase(jiraFieldName.trim());
    }

    private Object formatSpecialCustomFieldIfAny(Object value, Map<String, Object> metadata, String webhookId) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object t = metadata.get("fieldType");
        if (t == null)
            t = metadata.get("schemaType");
        if (t == null)
            t = metadata.get("type");
        if (!(t instanceof String s)) {
            return null;
        }
        String mdType = s.toLowerCase(Locale.ROOT);
        if (mdType.contains("atlassian-team") || mdType.equals("team")) {
            return convertToJiraTeamFormat(value, webhookId);
        }
        if (mdType.contains("gh-sprint") || mdType.equals("sprint")) {
            return convertToJiraSprintFormat(value, webhookId);
        }
        return null;
    }

    /**
     * Converts Team custom field value to Jira team object format.
     * Jira expects: {@code {"id": "TEAM_ID"}}
     */
    private Object convertToJiraTeamFormat(Object value, String webhookId) {
        if (value == null)
            return null;
        if (value instanceof Map<?, ?> m && m.containsKey("id")) {
            return m; // Already in correct shape
        }
        String id = value.toString().trim();
        if (id.isEmpty())
            return null;
        log.debug("Converting team value '{}' to object format for webhook {}", id, webhookId);
        return Map.of("id", id);
    }

    /**
     * Converts Sprint custom field value to expected Jira format.
     * Commonly Jira accepts the sprint custom field value as a numeric sprint ID.
     * If provided with an object, {@code {"id": ID}} is also safe.
     */
    private Object convertToJiraSprintFormat(Object value, String webhookId) {
        switch (value) {
            case null -> {
                return null;
            }
            case Map<?, ?> m when m.containsKey("id") -> {
                return m; // Already object form
            }
            // If an array/list provided, use the first element (Jira typically expects a
            // single sprint)
            case List<?> list when !list.isEmpty() -> {
                Object first = list.getFirst();
                return convertToJiraSprintFormat(first, webhookId);
            }
            default -> {
            }
        }
        String raw = value.toString().trim();
        if (raw.isEmpty())
            return null;
        try {
            Long sprintId = Long.parseLong(raw);
            log.debug("Converting sprint value '{}' to numeric ID for webhook {}", raw, webhookId);
            return sprintId;
        } catch (NumberFormatException ex) {
            // Fallback to object form with id as string
            log.debug("Sprint value '{}' not numeric, using object form for webhook {}", raw, webhookId);
            return Map.of("id", raw);
        }
    }

    /**
     * Converts project value to Jira project object format.
     * Jira expects: {"key": "PROJECT_KEY"} or {"id": "10001"}
     */
    private Object convertToJiraProjectFormat(Object value, String webhookId) {
        if (value == null) {
            return null;
        }

        String projectValue = value.toString().trim();
        if (projectValue.isEmpty()) {
            return null;
        }

        // Check if the value is numeric (project ID)
        try {
            Long.parseLong(projectValue);
            log.debug("Converting project value '{}' to ID format for webhook {}", projectValue, webhookId);
            return Map.of("id", projectValue);
        } catch (NumberFormatException e) {
            // Not numeric, treat as project key
            log.debug("Converting project value '{}' to key format for webhook {}", projectValue, webhookId);
            return Map.of("key", projectValue);
        }
    }

    /**
     * Converts issue type value to Jira issue type object format.
     * Jira expects: {"id": "10001"} or {"name": "Task"}
     */
    private Object convertToJiraIssueTypeFormat(Object value, String webhookId) {
        if (value == null) {
            return null;
        }

        String issueTypeValue = value.toString().trim();
        if (issueTypeValue.isEmpty()) {
            return null;
        }

        // Check if the value is numeric (issue type ID)
        try {
            Long.parseLong(issueTypeValue);
            log.debug("Converting issue type value '{}' to ID format for webhook {}", issueTypeValue, webhookId);
            return Map.of("id", issueTypeValue);
        } catch (NumberFormatException e) {
            // Not numeric, treat as issue type name
            log.debug("Converting issue type value '{}' to name format for webhook {}", issueTypeValue, webhookId);
            return Map.of("name", issueTypeValue);
        }
    }

    /**
     * Converts description text to Atlassian Document Format (ADF) for Jira API v3.
     * Jira API v3 requires descriptions to be in ADF format instead of plain text.
     */
    private Object convertToJiraDescriptionFormat(Object value, String webhookId) {
        if (value == null) {
            return null;
        }

        String descriptionText = value.toString().trim();
        if (descriptionText.isEmpty()) {
            return null;
        }

        // Convert plain text to Atlassian Document Format (ADF)
        Map<String, Object> adfDocument = Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(
                        Map.of(
                                "type", "paragraph",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", descriptionText)))));

        log.debug("Converting description to ADF format for webhook {}: {} characters",
                webhookId, descriptionText.length());
        return adfDocument;
    }

    /**
     * Converts priority value to Jira priority object format.
     * Jira expects: {"id": "1"} or {"name": "High"}
     */
    private Object convertToJiraPriorityFormat(Object value, String webhookId) {
        if (value == null) {
            return null;
        }

        String priorityValue = value.toString().trim();
        if (priorityValue.isEmpty()) {
            return null;
        }

        // Check if the value is numeric (priority ID)
        try {
            Long.parseLong(priorityValue);
            log.debug("Converting priority value '{}' to ID format for webhook {}", priorityValue, webhookId);
            return Map.of("id", priorityValue);
        } catch (NumberFormatException e) {
            // Not numeric, treat as priority name
            log.debug("Converting priority value '{}' to name format for webhook {}", priorityValue, webhookId);
            return Map.of("name", priorityValue);
        }
    }

    /**
     * Converts user value to Jira user object format.
     * Jira expects: {"accountId": "user123"} or {"name": "username"} or
     * {"emailAddress": "user@example.com"}
     */
    private Object convertToJiraUserFormat(Object value, String webhookId) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> m && m.containsKey("accountId")) {
            return m;
        }

        String userValue = value.toString().trim();
        if (userValue.isEmpty()) {
            return null;
        }

        // Jira Cloud accountId always contains colon OR matches the accountId pattern
        if (userValue.contains(":")
                || JIRA_ACCOUNT_ID_PATTERN.matcher(userValue).matches()) {

            log.debug("Converting user value '{}' to accountId for webhook {}", userValue, webhookId);
            return Map.of("accountId", userValue);
        }

        log.warn("Invalid user format '{}' — Jira Cloud only supports accountId. Returning null.", userValue);
        return null;
    }
}
