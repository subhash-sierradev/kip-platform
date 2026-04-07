package com.integration.management.notification.aop;

import com.integration.management.entity.JiraWebhook;
import com.integration.management.repository.JiraWebhookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookNotificationMetadataProvider")
class JiraWebhookNotificationMetadataProviderTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock private JiraWebhookRepository jiraWebhookRepository;

    @InjectMocks
    private JiraWebhookNotificationMetadataProvider provider;

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns metadata map with webhook name and id when found")
        void returns_metadata_when_found() {
            String webhookId = "webhook-abc";
            JiraWebhook webhook = JiraWebhook.builder().name("My Webhook").build();
            webhook.setId(webhookId);

            when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(webhookId, TENANT_ID))
                    .thenReturn(Optional.of(webhook));

            Map<String, Object> result = provider.resolve(webhookId, TENANT_ID);

            assertThat(result).containsEntry("webhookName", "My Webhook")
                    .containsEntry("webhookId", webhookId);
        }

        @Test
        @DisplayName("returns empty map when webhook not found")
        void returns_empty_map_when_not_found() {
            String webhookId = "missing-webhook";
            when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(webhookId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThat(provider.resolve(webhookId, TENANT_ID)).isEmpty();
        }

        @Test
        @DisplayName("substitutes empty strings when webhook name and id are null")
        void substitutes_empty_strings_when_null() {
            String webhookId = "webhook-null";
            JiraWebhook webhook = JiraWebhook.builder().name(null).build();
            webhook.setId(null);

            when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(webhookId, TENANT_ID))
                    .thenReturn(Optional.of(webhook));

            Map<String, Object> result = provider.resolve(webhookId, TENANT_ID);

            assertThat(result).containsEntry("webhookName", "")
                    .containsEntry("webhookId", "");
        }
    }
}
