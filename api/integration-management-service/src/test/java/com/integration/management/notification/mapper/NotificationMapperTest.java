package com.integration.management.notification.mapper;

import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.management.notification.entity.NotificationTemplate;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationMapper")
class NotificationMapperTest {

    private static NotificationMapperImpl mapper() {
        return new NotificationMapperImpl();
    }

    @Test
    @DisplayName("toRuleResponse should map nested event fields")
    void toRuleResponse_mapsEventFields() {
        NotificationMapperImpl mapper = mapper();
        UUID eventId = UUID.randomUUID();
        NotificationEventCatalog event = NotificationEventCatalog.builder()
                .id(eventId).eventKey("EVT").entityType(NotificationEntityType.ARCGIS_INTEGRATION).displayName("d").build();
        UUID ruleId = UUID.randomUUID();
        NotificationRule rule = NotificationRule.builder()
                .id(ruleId).event(event).severity(NotificationSeverity.ERROR).isEnabled(true).build();

        NotificationRuleResponse response = mapper.toRuleResponse(rule);

        assertThat(response.getId()).isEqualTo(ruleId);
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getEventKey()).isEqualTo("EVT");
        assertThat(response.getEntityType()).isEqualTo(NotificationEntityType.ARCGIS_INTEGRATION.name());
        assertThat(response.getSeverity()).isEqualTo(NotificationSeverity.ERROR);
    }

    @Test
    @DisplayName("toRuleResponse with null entityType skips entityType field")
    void toRuleResponse_nullEntityType_skipsEntityType() {
        NotificationMapperImpl mapper = mapper();
        NotificationEventCatalog event = NotificationEventCatalog.builder()
                .id(UUID.randomUUID()).eventKey("EVT").entityType(null).displayName("d").build();
        NotificationRule rule = NotificationRule.builder()
                .id(UUID.randomUUID()).event(event).severity(NotificationSeverity.INFO).isEnabled(true).build();

        assertThat(mapper.toRuleResponse(rule).getEntityType()).isNull();
    }

    @Test
    @DisplayName("toRuleResponse with null event returns null eventId and eventKey")
    void toRuleResponse_nullEvent_returnsNullEventFields() {
        NotificationMapperImpl mapper = mapper();
        NotificationRule rule = NotificationRule.builder()
                .id(UUID.randomUUID()).event(null).severity(NotificationSeverity.INFO).isEnabled(true).build();

        NotificationRuleResponse response = mapper.toRuleResponse(rule);
        assertThat(response.getEventId()).isNull();
        assertThat(response.getEventKey()).isNull();
    }

    @Test
    @DisplayName("toTemplateResponse with null event returns null eventId and eventKey")
    void toTemplateResponse_nullEvent_returnsNullEventFields() {
        NotificationMapperImpl mapper = mapper();
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID()).event(null).titleTemplate("Hello").messageTemplate("World").build();

        NotificationTemplateResponse response = mapper.toTemplateResponse(template);
        assertThat(response.getEventId()).isNull();
        assertThat(response.getEventKey()).isNull();
        assertThat(response.getTitleTemplate()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("toAppNotificationResponse with non-null metadata maps metadata")
    void toAppNotificationResponse_nonNullMetadata_mapsMetadata() {
        NotificationMapperImpl mapper = mapper();
        AppNotification notification = AppNotification.builder()
                .id(UUID.randomUUID()).tenantId("t1").userId("u1")
                .type(NotificationEventKey.SITE_CONFIG_UPDATED).severity(NotificationSeverity.INFO)
                .title("T").message("M").metadata(Map.of("key", "value")).isRead(false).build();

        AppNotificationResponse response = mapper.toAppNotificationResponse(notification);
        assertThat(response.getMetadata()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("toAppNotificationResponse with null metadata does not set metadata")
    void toAppNotificationResponse_nullMetadata_noMetadata() {
        NotificationMapperImpl mapper = mapper();
        AppNotification notification = AppNotification.builder()
                .id(UUID.randomUUID()).tenantId("t1").userId("u1")
                .type(NotificationEventKey.SITE_CONFIG_UPDATED).severity(NotificationSeverity.INFO)
                .title("T").message("M").metadata(null).isRead(false).build();

        assertThat(mapper.toAppNotificationResponse(notification).getMetadata()).isNull();
    }

    @Test
    @DisplayName("toRecipientPolicyResponse with null rule returns null ruleId and eventKey")
    void toRecipientPolicyResponse_nullRule_returnsNullRuleIdAndEventKey() {
        NotificationMapperImpl mapper = mapper();
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID()).rule(null).recipientType(RecipientType.SELECTED_USERS).build();

        RecipientPolicyResponse response = mapper.toRecipientPolicyResponse(policy);
        assertThat(response.getRuleId()).isNull();
        assertThat(response.getEventKey()).isNull();
        assertThat(response.getRecipientType()).isEqualTo(RecipientType.SELECTED_USERS.name());
    }

    @Test
    @DisplayName("toRecipientPolicyResponse with rule having null event returns null eventKey")
    void toRecipientPolicyResponse_ruleWithNullEvent_returnsNullEventKey() {
        NotificationMapperImpl mapper = mapper();
        NotificationRule rule = NotificationRule.builder()
                .id(UUID.randomUUID()).event(null).severity(NotificationSeverity.INFO).isEnabled(true).build();
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID()).rule(rule).recipientType(RecipientType.ADMINS_ONLY).build();

        RecipientPolicyResponse response = mapper.toRecipientPolicyResponse(policy);
        assertThat(response.getEventKey()).isNull();
        assertThat(response.getRuleId()).isEqualTo(rule.getId());
    }

    @Test
    @DisplayName("toRecipientPolicyResponse should map rule id, event key, and recipient type name")
    void toRecipientPolicyResponse_mapsFields() {
        NotificationMapperImpl mapper = mapper();
        NotificationEventCatalog event = NotificationEventCatalog.builder()
                .id(UUID.randomUUID()).eventKey("EVT").entityType(NotificationEntityType.JIRA_WEBHOOK).displayName("d").build();
        UUID ruleId = UUID.randomUUID();
        NotificationRule rule = NotificationRule.builder()
                .id(ruleId).event(event).severity(NotificationSeverity.INFO).isEnabled(true).build();
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID()).rule(rule).recipientType(RecipientType.ALL_USERS).build();

        RecipientPolicyResponse response = mapper.toRecipientPolicyResponse(policy);
        assertThat(response.getRuleId()).isEqualTo(ruleId);
        assertThat(response.getEventKey()).isEqualTo("EVT");
        assertThat(response.getRecipientType()).isEqualTo(RecipientType.ALL_USERS.name());
    }

    @Test
    @DisplayName("toRuleResponse returns null for null entity")
    void toRuleResponse_null_returnsNull() {
        assertThat(mapper().toRuleResponse(null)).isNull();
    }

    @Test
    @DisplayName("toTemplateResponse returns null for null entity")
    void toTemplateResponse_null_returnsNull() {
        assertThat(mapper().toTemplateResponse(null)).isNull();
    }

    @Test
    @DisplayName("toTemplateResponse with non-null event maps event fields")
    void toTemplateResponse_nonNullEvent_mapsEventFields() {
        NotificationMapperImpl mapper = mapper();
        NotificationEventCatalog event = NotificationEventCatalog.builder()
                .id(UUID.randomUUID()).eventKey("EVT2").build();
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID()).event(event).titleTemplate("Title").messageTemplate("Msg").build();

        NotificationTemplateResponse response = mapper.toTemplateResponse(template);
        assertThat(response.getEventKey()).isEqualTo("EVT2");
        assertThat(response.getEventId()).isNotNull();
    }

    @Test
    @DisplayName("toAppNotificationResponse returns null for null entity")
    void toAppNotificationResponse_null_returnsNull() {
        assertThat(mapper().toAppNotificationResponse(null)).isNull();
    }

    @Test
    @DisplayName("toRecipientPolicyResponse returns null for null entity")
    void toRecipientPolicyResponse_null_returnsNull() {
        assertThat(mapper().toRecipientPolicyResponse(null)).isNull();
    }
}
