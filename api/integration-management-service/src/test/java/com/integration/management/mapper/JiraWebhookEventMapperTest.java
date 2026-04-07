package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.model.dto.response.JiraWebhookEventResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookEventMapper")
class JiraWebhookEventMapperTest {

    @Test
    @DisplayName("toResponse returns null for null")
    void toResponse_null_returnsNull() {
        JiraWebhookEventMapper mapper = new JiraWebhookEventMapperImpl();
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toResponse maps entity fields")
    void toResponse_mapsFields() {
        JiraWebhookEventMapper mapper = new JiraWebhookEventMapperImpl();

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000101");
        Instant triggeredAt = Instant.parse("2026-03-06T00:00:00Z");

        JiraWebhookEvent entity = JiraWebhookEvent.builder()
                .id(id)
                .tenantId("tenant-1")
                .triggeredBy("user-1")
                .triggeredAt(triggeredAt)
                .webhookId("wh-1")
                .incomingPayload("in")
                .transformedPayload("out")
                .responseStatusCode(200)
                .responseBody("OK")
                .status(TriggerStatus.SUCCESS)
                .retryAttempt(2)
                .originalEventId("orig-1")
                .jiraIssueUrl("https://jira.example/browse/ABC-1")
                .build();

        JiraWebhookEventResponse response = mapper.toResponse(entity);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id.toString());
        assertThat(response.getTenantId()).isEqualTo("tenant-1");
        assertThat(response.getTriggeredBy()).isEqualTo("user-1");
        assertThat(response.getTriggeredAt()).isEqualTo(triggeredAt);
        assertThat(response.getWebhookId()).isEqualTo("wh-1");
        assertThat(response.getIncomingPayload()).isEqualTo("in");
        assertThat(response.getTransformedPayload()).isEqualTo("out");
        assertThat(response.getResponseStatusCode()).isEqualTo(200);
        assertThat(response.getResponseBody()).isEqualTo("OK");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getRetryAttempt()).isEqualTo(2);
        assertThat(response.getOriginalEventId()).isEqualTo("orig-1");
        assertThat(response.getJiraIssueUrl()).isEqualTo("https://jira.example/browse/ABC-1");
    }
}
