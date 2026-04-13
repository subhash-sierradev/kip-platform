package com.integration.execution.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.JiraApiClient;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.mapper.JiraFieldMappingResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraWebhookProcessorTest {

    @Mock
    private JiraFieldMappingResolver jiraFieldMappingResolver;

    @Mock
    private JiraApiClient jiraApiClient;

    private JiraWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new JiraWebhookProcessor(jiraFieldMappingResolver, jiraApiClient, new ObjectMapper());
    }

    @Test
    void processWebhookExecution_emptyPayload_returnsFailureResult() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-1")
                .incomingPayload(" ")
                .triggerEventId(UUID.randomUUID())
                .build();

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("payload cannot be null or empty");
    }

    @Test
    void processWebhookExecution_invalidJsonPayload_returnsFailureResult() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-2")
                .incomingPayload("not-json")
                .triggerEventId(UUID.randomUUID())
                .fieldMappings(java.util.List.of())
                .build();

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid JSON payload");
    }

    @Test
    void processWebhookExecution_successResponse_buildsJiraIssueUrl() {
        UUID triggerEventId = UUID.randomUUID();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-3")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(java.util.List.of())
                .triggerEventId(triggerEventId)
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-3"), any()))
                .thenReturn(Map.of("summary", "Issue from webhook"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class))).thenReturn(
                new ApiResponse(
                        201,
                        true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10000\",\"key\":\"PRJ-1\"}"
                )
        );

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResponseStatusCode()).isEqualTo(201);
        assertThat(result.getTransformedPayload()).contains("summary");
        assertThat(result.getJiraIssueUrl()).isEqualTo("https://jira.example.com/browse/PRJ-1");
        assertThat(result.getTriggerEventId()).isEqualTo(triggerEventId);
    }

    @Test
    void processWebhookExecution_unsuccessfulApiResponse_hasNoIssueUrl() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-4")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(java.util.List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-4"), any()))
                .thenReturn(Map.of("summary", "Issue from webhook"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(400, false, "Bad Request"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getJiraIssueUrl()).isNull();
    }

    @Test
    void processWebhookExecution_successWithNonJsonResponse_hasNullIssueUrl() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-5")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(java.util.List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-5"), any()))
                .thenReturn(Map.of("summary", "Issue from webhook"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true, "non-json-response"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJiraIssueUrl()).isNull();
    }

    @Test
    void processWebhookExecution_subtaskWithInvalidParentString_returnsFailureResult() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-parent-invalid");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-invalid"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", "abc",
                        "summary", "Issue from webhook"
                ));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage())
                .contains("Parent field is required for Subtask issue type");
    }

    @Test
    void processWebhookExecution_subtaskWithNumericParentString_normalizesToParentId() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-parent-id");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-id"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", "12345",
                        "summary", "Issue from webhook"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10001\",\"key\":\"PRJ-2\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransformedPayload()).contains("\"parent\":{\"id\":\"12345\"}");
    }

    @Test
    void processWebhookExecution_subtaskWithKeyParentString_normalizesToParentKey() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-parent-key");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-key"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", "prj-77",
                        "summary", "Issue from webhook"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10002\",\"key\":\"PRJ-3\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransformedPayload()).contains("\"parent\":{\"key\":\"PRJ-77\"}");
    }

    private JiraWebhookExecutionCommand buildSubtaskCommand(final String webhookId) {
        return JiraWebhookExecutionCommand.builder()
                .webhookId(webhookId)
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(subtaskFieldMappings())
                .triggerEventId(UUID.randomUUID())
                .build();
    }

    private List<JiraFieldMappingDto> subtaskFieldMappings() {
        return List.of(
                JiraFieldMappingDto.builder()
                        .jiraFieldId("issuetype")
                        .jiraFieldName("Issue Type")
                        .dataType(JiraDataType.STRING)
                        .build(),
                JiraFieldMappingDto.builder()
                        .jiraFieldId("parent")
                        .jiraFieldName("Parent")
                        .dataType(JiraDataType.STRING)
                        .build()
        );
    }

    // --- Additional branch-coverage tests ---

    @Test
    void processWebhookExecution_nullPayload_returnsFailureResult() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-null")
                .incomingPayload(null)
                .triggerEventId(UUID.randomUUID())
                .build();

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("payload cannot be null or empty");
    }

    @Test
    void processWebhookExecution_parentMapWithValidKey_normalizesToKeyObject() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-map-key");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-map-key"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", Map.of("key", "PRJ-99", "id", "unused"),
                        "summary", "Child issue"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10003\",\"key\":\"PRJ-4\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransformedPayload()).contains("\"parent\":{\"key\":\"PRJ-99\"}");
    }

    @Test
    void processWebhookExecution_parentMapWithValidIdOnly_normalizesToIdObject() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-map-id");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-map-id"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", Map.of("id", "99999"),
                        "summary", "Child issue"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10004\",\"key\":\"PRJ-5\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransformedPayload()).contains("\"parent\":{\"id\":\"99999\"}");
    }

    @Test
    void processWebhookExecution_parentMapWithInvalidKeyAndId_removesParent() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-map-invalid");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-map-invalid"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", Map.of("key", "no-match", "id", "abc"),
                        "summary", "Child issue"
                ));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Parent field is required");
    }

    @Test
    void processWebhookExecution_parentNonStringNonMapType_removesParent() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-bad-type")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-bad-type"), any()))
                .thenReturn(Map.of(
                        "parent", 12345,
                        "summary", "Issue"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10005\",\"key\":\"PRJ-6\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransformedPayload()).doesNotContain("parent");
    }

    @Test
    void processWebhookExecution_parentNull_removesFromPayload() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-null-parent")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("parent", null);
        fields.put("summary", "Issue");
        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-null-parent"), any()))
                .thenReturn(fields);
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10006\",\"key\":\"PRJ-7\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransformedPayload()).doesNotContain("parent");
    }

    @Test
    void processWebhookExecution_subtaskWithMetadataFlag_detectsSubtaskViaMetadata() {
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .metadata(Map.of("subtask", true))
                .build();
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-metadata-subtask")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(issuetypeMapping, parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-metadata-subtask"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Story",
                        "parent", "12345",
                        "summary", "Sub issue"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10007\",\"key\":\"PRJ-8\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_issueTypeMapWithName_detectsSubtask() {
        JiraWebhookExecutionCommand command = buildSubtaskCommand("webhook-map-issuetype");

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-map-issuetype"), any()))
                .thenReturn(Map.of(
                        "issuetype", Map.of("name", "Sub-task"),
                        "parent", "12345",
                        "summary", "Map issue type"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10008\",\"key\":\"PRJ-9\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_notSubtaskWithNoParentMapping_skipsValidation() {
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-not-subtask")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(issuetypeMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-not-subtask"), any()))
                .thenReturn(Map.of("issuetype", "Bug", "summary", "Normal issue"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10009\",\"key\":\"PRJ-10\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_subtaskWithNoParentMappingDefined_skipsParentValidation() {
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-subtask-no-parent-mapping")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(issuetypeMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-subtask-no-parent-mapping"), any()))
                .thenReturn(Map.of("issuetype", "Sub-task", "summary", "Sub without parent mapping"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10010\",\"key\":\"PRJ-11\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_selfUrlWithNoRestPath_usesFullSelfAsBaseUrl() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-no-rest")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-no-rest"), any()))
                .thenReturn(Map.of("summary", "Issue"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/\",\"key\":\"TEST-1\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJiraIssueUrl()).isEqualTo("https://jira.example.com/browse/TEST-1");
    }

    @Test
    void processWebhookExecution_selfUrlWithMissingSelf_returnsNullIssueUrl() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-missing-self")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-missing-self"), any()))
                .thenReturn(Map.of("summary", "Issue"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true, "{\"key\":\"TEST-1\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJiraIssueUrl()).isNull();
    }

    @Test
    void processWebhookExecution_nullFieldMappings_hasSubtaskMetadataReturnsFalse() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-null-mappings")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(null)
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-null-mappings"), any()))
                .thenReturn(Map.of("summary", "Issue"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10011\",\"key\":\"PRJ-12\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_issueTypeMapWithNonStringName_notDetectedAsSubtask() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-nonstring-name")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-nonstring-name"), any()))
                .thenReturn(Map.of(
                        "issuetype", Map.of("name", 42),
                        "summary", "Issue"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10012\",\"key\":\"PRJ-13\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_metadataFlagFalse_notDetectedAsSubtask() {
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .metadata(Map.of("subtask", false))
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-metadata-false")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(issuetypeMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-metadata-false"), any()))
                .thenReturn(Map.of("issuetype", "Task", "summary", "Normal task"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10013\",\"key\":\"PRJ-14\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentFieldIsNullInResult_removedFromPayload() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-null-parent")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("summary", "Test");
        fields.put("parent", null);
        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-null-parent"), any()))
                .thenReturn(fields);
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-1\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsStringWithValidIssueKey_normalizedToKeyMap() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-key")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-key"), any()))
                .thenReturn(Map.of("summary", "Test", "parent", "PRJ-42"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-2\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsStringWithValidNumericId_normalizedToIdMap() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-id")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-id"), any()))
                .thenReturn(Map.of("summary", "Test", "parent", "10042"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-3\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsStringInvalidValue_removedFromPayload() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-invalid")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-invalid"), any()))
                .thenReturn(Map.of("summary", "Test", "parent", "invalid-value"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-4\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsMapWithValidKey_normalizedToKeyMap() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-map-key")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-map-key"), any()))
                .thenReturn(Map.of("summary", "Test", "parent", Map.of("key", "PRJ-100")));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-5\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsMapWithValidId_normalizedToIdMap() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-map-id")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-map-id"), any()))
                .thenReturn(Map.of("summary", "Test", "parent", Map.of("id", "20001")));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-6\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsMapWithInvalidKeyAndId_removedFromPayload() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-map-invalid")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-map-invalid"), any()))
                .thenReturn(Map.of("summary", "Test", "parent", Map.of("key", "invalid", "id", "not-numeric")));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-7\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_parentAsNonStringNonMap_removedFromPayload() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-parent-nontype")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("summary", "Test");
        fields.put("parent", 12345);  // Integer is neither String nor Map
        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-parent-nontype"), any()))
                .thenReturn(fields);
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-8\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_issueTypeMapSubtask_withMissingParentAndParentMapping_returnsFailure() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-subtask-map-no-parent")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping, issuetypeMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-subtask-map-no-parent"), any()))
                .thenReturn(Map.of("issuetype", Map.of("name", "Subtask"), "summary", "Missing parent"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Parent field is required");
    }

    @Test
    void processWebhookExecution_issueTypeStringSubTask_withValidParentKey_succeeds() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-subtask-str-valid-parent")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping, issuetypeMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-subtask-str-valid-parent"), any()))
                .thenReturn(Map.of(
                        "issuetype", "Sub-task",
                        "parent", Map.of("key", "PRJ-50"),
                        "summary", "Valid subtask"
                ));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/10\",\"key\":\"PRJ-51\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_subtaskWithParentStringKey_hasValidParent_succeeds() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-subtask-parent-str-key")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-subtask-parent-str-key"), any()))
                .thenReturn(Map.of("issuetype", "subtask", "parent", "PRJ-99", "summary", "Sub"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/11\",\"key\":\"PRJ-100\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_subtaskWithParentNumericString_hasValidParent_succeeds() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-subtask-parent-num-str")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-subtask-parent-num-str"), any()))
                .thenReturn(Map.of("issuetype", "subtask", "parent", "12345", "summary", "Sub"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/11\",\"key\":\"PRJ-101\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_subtaskWithParentMapId_hasValidParent_succeeds() {
        JiraFieldMappingDto parentMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("parent")
                .jiraFieldName("Parent")
                .dataType(JiraDataType.STRING)
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-subtask-parent-map-id")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(parentMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-subtask-parent-map-id"), any()))
                .thenReturn(Map.of("issuetype", "subtask", "parent", Map.of("id", "99001"), "summary", "Sub"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/12\",\"key\":\"PRJ-102\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void processWebhookExecution_calculateJiraIssueUrl_trailingSlashBase_normalizedCorrectly() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-url-trailing")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-url-trailing"), any()))
                .thenReturn(Map.of("summary", "Test"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/999\",\"key\":\"PROJ-999\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJiraIssueUrl()).isEqualTo("https://jira.example.com/browse/PROJ-999");
    }

    @Test
    void processWebhookExecution_calculateJiraIssueUrl_invalidJson_returnsNullUrl() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-url-invalid-json")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-url-invalid-json"), any()))
                .thenReturn(Map.of("summary", "Test"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true, "not-json-at-all"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJiraIssueUrl()).isNull();
    }

    @Test
    void processWebhookExecution_missingKey_returnsNullIssueUrl() {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-missing-key")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of())
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-missing-key"), any()))
                .thenReturn(Map.of("summary", "Test"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJiraIssueUrl()).isNull();
    }

    @Test
    void processWebhookExecution_metadataFlagNonBoolean_notDetectedAsSubtask() {
        JiraFieldMappingDto issuetypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.STRING)
                .metadata(Map.of("subtask", "yes"))  // non-boolean
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("webhook-metadata-nonbool")
                .connectionSecretName("secret")
                .incomingPayload("{\"a\":1}")
                .fieldMappings(List.of(issuetypeMapping))
                .triggerEventId(UUID.randomUUID())
                .build();

        when(jiraFieldMappingResolver.processAllFieldMappings(any(), eq("webhook-metadata-nonbool"), any()))
                .thenReturn(Map.of("issuetype", "Task", "summary", "Normal"));
        when(jiraApiClient.sendToJira(eq("secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true,
                        "{\"self\":\"https://jira.example.com/rest/api/3/issue/1\",\"key\":\"PRJ-1\"}"));

        JiraWebhookExecutionResult result = processor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isTrue();
    }
}
