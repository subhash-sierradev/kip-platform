package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookDetailResponse")
class JiraWebhookDetailResponseTest {

    @Test
    @DisplayName("builder populates full detail response")
    void builderPopulatesFullDetailResponse() {
        JiraFieldMappingDto mapping = JiraFieldMappingDto.builder()
                .id("m1")
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .template("{{title}}")
                .build();

        Instant createdDate = Instant.parse("2026-03-06T00:00:00Z");
        Instant modifiedDate = Instant.parse("2026-03-06T03:30:00Z");

        JiraWebhookDetailResponse response = JiraWebhookDetailResponse.builder()
                .id("wh-1")
                .name("Webhook One")
                .description("Creates Jira issues")
                .webhookUrl("https://example.com/webhook")
                .connectionId("conn-1")
                .jiraFieldMappings(List.of(mapping))
                .samplePayload("{\"sample\":true}")
                .isEnabled(true)
                .isDeleted(false)
                .normalizedName("webhook-one")
                .createdBy("admin")
                .createdDate(createdDate)
                .lastModifiedBy("editor")
                .lastModifiedDate(modifiedDate)
                .tenantId("tenant-1")
                .version(7L)
                .build();

        assertThat(response.getId()).isEqualTo("wh-1");
        assertThat(response.getJiraFieldMappings()).hasSize(1);
        assertThat(response.getJiraFieldMappings().getFirst().getJiraFieldName()).isEqualTo("Summary");
        assertThat(response.getIsEnabled()).isTrue();
        assertThat(response.getVersion()).isEqualTo(7L);
    }
}
