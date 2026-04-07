package com.integration.management.notification.entity;

import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEntityCoverageTest {

    @Test
    void notification_entities_buildersAndDefaults_smokeTest() {
        NotificationEventCatalog eventCatalog = NotificationEventCatalog.builder()
                .id(UUID.randomUUID())
                .eventKey("KEY")
                .entityType(NotificationEntityType.SITE_CONFIG)
                .displayName("Display")
                .description("Desc")
                .build();
        assertThat(eventCatalog.getIsEnabled()).isTrue();
        assertThat(eventCatalog.getNotifyInitiator()).isFalse();

        NotificationRule rule = NotificationRule.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .event(eventCatalog)
                .severity(NotificationSeverity.INFO)
                .build();
        assertThat(rule.getIsEnabled()).isTrue();

        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .rule(rule)
                .recipientType(RecipientType.ADMINS_ONLY)
                .build();
        assertThat(policy.getRecipientType()).isEqualTo(RecipientType.ADMINS_ONLY);

        NotificationRecipientUser recipientUser = NotificationRecipientUser.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .recipientPolicy(policy)
                .userId("user")
                .build();
        assertThat(recipientUser.getUserId()).isEqualTo("user");

        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .event(eventCatalog)
                .titleTemplate("Title")
                .messageTemplate("Message")
                .allowedPlaceholders("{a},{b}")
                .build();
        assertThat(template.getTitleTemplate()).isEqualTo("Title");

        AppNotification notification = AppNotification.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .userId("user")
                .type(NotificationEventKey.SITE_CONFIG_UPDATED)
                .severity(NotificationSeverity.WARNING)
                .title("Title")
                .message("Message")
                .metadata(Map.of("k", "v"))
                .build();
        assertThat(notification.getIsRead()).isFalse();
    }
}
