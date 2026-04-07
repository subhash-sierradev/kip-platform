package com.integration.management.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookEventSummaryResponse")
class JiraWebhookEventSummaryResponseTest {

    @Test
    @DisplayName("builder populates summary response fields")
    void builderPopulatesSummaryResponseFields() {
        Instant triggeredAt = Instant.parse("2026-03-06T01:00:00Z");

        JiraWebhookEventSummaryResponse response = JiraWebhookEventSummaryResponse.builder()
                .id("evt-1")
                .triggeredBy("user-1")
                .triggeredAt(triggeredAt)
                .status("SUCCESS")
                .jiraIssueUrl("https://jira.example.com/browse/ABC-1")
                .build();

        assertThat(response.getId()).isEqualTo("evt-1");
        assertThat(response.getTriggeredBy()).isEqualTo("user-1");
        assertThat(response.getTriggeredAt()).isEqualTo(triggeredAt);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getJiraIssueUrl()).contains("ABC-1");
    }
}
