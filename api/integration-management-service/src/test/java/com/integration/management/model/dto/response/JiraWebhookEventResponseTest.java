package com.integration.management.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookEventResponse")
class JiraWebhookEventResponseTest {

    @Test
    @DisplayName("builder uses default retryAttempt when not provided")
    void builderUsesDefaultRetryAttemptWhenNotProvided() {
        JiraWebhookEventResponse response = JiraWebhookEventResponse.builder()
                .id("evt-1")
                .tenantId("tenant-1")
                .triggeredBy("user-1")
                .triggeredAt(Instant.parse("2026-03-06T01:15:00Z"))
                .webhookId("wh-1")
                .status("PENDING")
                .build();

        assertThat(response.getRetryAttempt()).isEqualTo(0);
        assertThat(response.getWebhookId()).isEqualTo("wh-1");
    }

    @Test
    @DisplayName("builder keeps explicit retryAttempt and payload fields")
    void builderKeepsExplicitRetryAttemptAndPayloadFields() {
        JiraWebhookEventResponse response = JiraWebhookEventResponse.builder()
                .id("evt-2")
                .tenantId("tenant-1")
                .triggeredBy("user-2")
                .triggeredAt(Instant.parse("2026-03-06T02:00:00Z"))
                .webhookId("wh-2")
                .incomingPayload("{\"key\":\"value\"}")
                .transformedPayload("{\"fields\":{}}")
                .responseStatusCode(201)
                .responseBody("created")
                .status("SUCCESS")
                .retryAttempt(2)
                .originalEventId("evt-1")
                .jiraIssueUrl("https://jira.example.com/browse/ABC-2")
                .build();

        assertThat(response.getRetryAttempt()).isEqualTo(2);
        assertThat(response.getResponseStatusCode()).isEqualTo(201);
        assertThat(response.getOriginalEventId()).isEqualTo("evt-1");
        assertThat(response.getJiraIssueUrl()).contains("ABC-2");
    }
}
