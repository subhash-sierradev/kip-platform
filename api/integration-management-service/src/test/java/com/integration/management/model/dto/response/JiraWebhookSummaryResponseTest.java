package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookSummaryResponse")
class JiraWebhookSummaryResponseTest {

    @Test
    @DisplayName("holds mapped field list and latest event summary")
    void holdsMappedFieldListAndLatestEventSummary() {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .id("m1")
                .jiraFieldId("customfield_10001")
                .jiraFieldName("Location")
                .dataType(JiraDataType.STRING)
                .template("{{location}}")
                .required(true)
                .build();

        JiraWebhookEventSummaryResponse lastEvent = JiraWebhookEventSummaryResponse.builder()
                .id("evt-1")
                .triggeredBy("user-1")
                .triggeredAt(Instant.parse("2026-03-06T01:00:00Z"))
                .status("SUCCESS")
                .jiraIssueUrl("https://jira.example.com/browse/ABC-1")
                .build();

        JiraWebhookSummaryResponse response = JiraWebhookSummaryResponse.builder()
                .id("wh-1")
                .name("Webhook 1")
                .webhookUrl("https://example.com/webhook")
                .jiraFieldMappings(List.of(mapping))
                .isEnabled(true)
                .isDeleted(false)
                .createdBy("admin")
                .createdDate(Instant.parse("2026-03-06T00:00:00Z"))
                .lastEventHistory(lastEvent)
                .build();

        assertThat(response.getJiraFieldMappings()).hasSize(1);
        assertThat(response.getJiraFieldMappings().getFirst().getJiraFieldId()).isEqualTo("customfield_10001");
        assertThat(response.getLastEventHistory()).isNotNull();
        assertThat(response.getLastEventHistory().getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getIsEnabled()).isTrue();
    }
}
