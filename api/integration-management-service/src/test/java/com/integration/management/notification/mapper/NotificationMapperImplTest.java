package com.integration.management.notification.mapper;

import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.management.notification.entity.NotificationTemplate;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationEventCatalogResponse;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationMapperImpl")
class NotificationMapperImplTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    @DisplayName("toEventCatalogResponse returns null for null input")
    void toEventCatalogResponse_returnsNull_forNullInput() {
        assertThat(mapper.toEventCatalogResponse(null)).isNull();
    }

    @Test
    @DisplayName("toEventCatalogResponse maps entityType only when non-null")
    void toEventCatalogResponse_mapsEntityTypeOnlyWhenNonNull() {
        NotificationEventCatalog withType = NotificationEventCatalog.builder()
                .id(UUID.randomUUID())
                .eventKey("SITE_CONFIG_UPDATED")
                .entityType(NotificationEntityType.SITE_CONFIG)
                .displayName("Site config updated")
                .build();

        NotificationEventCatalogResponse mappedWithType = mapper.toEventCatalogResponse(withType);
        assertThat(mappedWithType.getEntityType()).isEqualTo(NotificationEntityType.SITE_CONFIG.name());

        NotificationEventCatalog withoutType = NotificationEventCatalog.builder()
                .id(UUID.randomUUID())
                .eventKey("SITE_CONFIG_UPDATED")
                .entityType(null)
                .displayName("Site config updated")
                .build();

        NotificationEventCatalogResponse mappedWithoutType = mapper.toEventCatalogResponse(withoutType);
        assertThat(mappedWithoutType.getEntityType()).isNull();
    }

    @Test
    @DisplayName("toRuleResponse handles null event and null entityType")
    void toRuleResponse_handlesNullEvent_andNullEntityType() {
        NotificationRule noEvent = NotificationRule.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .event(null)
                .severity(NotificationSeverity.INFO)
                .isEnabled(true)
                .build();

        NotificationRuleResponse mappedNoEvent = mapper.toRuleResponse(noEvent);
        assertThat(mappedNoEvent.getEventId()).isNull();
        assertThat(mappedNoEvent.getEventKey()).isNull();
        assertThat(mappedNoEvent.getEntityType()).isNull();

        NotificationEventCatalog eventNoType = NotificationEventCatalog.builder()
                .id(UUID.randomUUID())
                .eventKey("SITE_CONFIG_UPDATED")
                .entityType(null)
                .displayName("Site config updated")
                .build();

        NotificationRule withEventNoType = NotificationRule.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .event(eventNoType)
                .severity(NotificationSeverity.INFO)
                .isEnabled(true)
                .build();

        NotificationRuleResponse mappedWithEventNoType = mapper.toRuleResponse(withEventNoType);
        assertThat(mappedWithEventNoType.getEventId()).isEqualTo(eventNoType.getId());
        assertThat(mappedWithEventNoType.getEventKey()).isEqualTo(eventNoType.getEventKey());
        assertThat(mappedWithEventNoType.getEntityType()).isNull();
    }

    @Test
    @DisplayName("toTemplateResponse handles null event")
    void toTemplateResponse_handlesNullEvent() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .event(null)
                .titleTemplate("Hello")
                .messageTemplate("World")
                .build();

        NotificationTemplateResponse mapped = mapper.toTemplateResponse(template);
        assertThat(mapped.getEventId()).isNull();
        assertThat(mapped.getEventKey()).isNull();
        assertThat(mapped.getTitleTemplate()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("toAppNotificationResponse copies metadata only when non-null")
    void toAppNotificationResponse_copiesMetadataOnlyWhenNonNull() {
        AppNotification withoutMetadata = AppNotification.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .userId("user-1")
                .type(NotificationEventKey.SITE_CONFIG_UPDATED)
                .severity(NotificationSeverity.INFO)
                .title("t")
                .message("m")
                .metadata(null)
                .build();

        AppNotificationResponse mappedWithoutMetadata = mapper.toAppNotificationResponse(withoutMetadata);
        assertThat(mappedWithoutMetadata.getMetadata()).isNull();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("configKey", "foo.bar");

        AppNotification withMetadata = AppNotification.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .userId("user-1")
                .type(NotificationEventKey.SITE_CONFIG_UPDATED)
                .severity(NotificationSeverity.INFO)
                .title("t")
                .message("m")
                .metadata(metadata)
                .build();

        AppNotificationResponse mappedWithMetadata = mapper.toAppNotificationResponse(withMetadata);
        assertThat(mappedWithMetadata.getMetadata()).containsEntry("configKey", "foo.bar");
        assertThat(mappedWithMetadata.getMetadata()).isNotSameAs(metadata);
    }

    @Test
    @DisplayName("toRecipientPolicyResponse handles null rule and null nested event")
    void toRecipientPolicyResponse_handlesNullRule_andNullNestedEvent() {
        NotificationRecipientPolicy noRule = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .rule(null)
                .recipientType(RecipientType.ALL_USERS)
                .build();

        RecipientPolicyResponse mappedNoRule = mapper.toRecipientPolicyResponse(noRule);
        assertThat(mappedNoRule.getRuleId()).isNull();
        assertThat(mappedNoRule.getEventKey()).isNull();
        assertThat(mappedNoRule.getRecipientType()).isEqualTo(RecipientType.ALL_USERS.name());

        NotificationRule ruleNoEvent = NotificationRule.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .event(null)
                .severity(NotificationSeverity.INFO)
                .isEnabled(true)
                .build();

        NotificationRecipientPolicy withRuleNoEvent = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .rule(ruleNoEvent)
                .recipientType(RecipientType.ALL_USERS)
                .build();

        RecipientPolicyResponse mappedWithRuleNoEvent = mapper.toRecipientPolicyResponse(withRuleNoEvent);
        assertThat(mappedWithRuleNoEvent.getRuleId()).isEqualTo(ruleNoEvent.getId());
        assertThat(mappedWithRuleNoEvent.getEventKey()).isNull();
    }
}
