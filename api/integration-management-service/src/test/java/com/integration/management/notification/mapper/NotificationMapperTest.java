package com.integration.management.notification.mapper;

import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationMapper")
class NotificationMapperTest {

    @Test
    @DisplayName("toRuleResponse should map nested event fields")
    void toRuleResponse_mapsEventFields() {
        NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);
        UUID eventId = UUID.randomUUID();
        NotificationEventCatalog event = NotificationEventCatalog.builder()
                .id(eventId)
                .eventKey("EVT")
                .entityType(NotificationEntityType.ARCGIS_INTEGRATION)
                .displayName("d")
                .build();

        UUID ruleId = UUID.randomUUID();
        NotificationRule rule = NotificationRule.builder()
                .id(ruleId)
                .event(event)
                .severity(NotificationSeverity.ERROR)
                .isEnabled(true)
                .build();

        NotificationRuleResponse response = mapper.toRuleResponse(rule);

        assertThat(response.getId()).isEqualTo(ruleId);
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getEventKey()).isEqualTo("EVT");
        assertThat(response.getEntityType()).isEqualTo(NotificationEntityType.ARCGIS_INTEGRATION.name());
        assertThat(response.getSeverity()).isEqualTo(NotificationSeverity.ERROR);
        assertThat(response.getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("toRecipientPolicyResponse should map rule id, event key, and recipient type name")
    void toRecipientPolicyResponse_mapsFields() {
        NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

        NotificationEventCatalog event = NotificationEventCatalog.builder()
                .id(UUID.randomUUID())
                .eventKey("EVT")
                .entityType(NotificationEntityType.JIRA_WEBHOOK)
                .displayName("d")
                .build();

        UUID ruleId = UUID.randomUUID();
        NotificationRule rule = NotificationRule.builder()
                .id(ruleId)
                .event(event)
                .severity(NotificationSeverity.INFO)
                .isEnabled(true)
                .build();

        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .id(UUID.randomUUID())
                .rule(rule)
                .recipientType(RecipientType.ALL_USERS)
                .build();

        RecipientPolicyResponse response = mapper.toRecipientPolicyResponse(policy);

        assertThat(response.getRuleId()).isEqualTo(ruleId);
        assertThat(response.getEventKey()).isEqualTo("EVT");
        assertThat(response.getRecipientType()).isEqualTo(RecipientType.ALL_USERS.name());
    }
}
