package com.integration.management.notification.service;

import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.request.BatchCreateNotificationRuleRequest;
import com.integration.management.notification.model.dto.request.CreateNotificationRuleRequest;
import com.integration.management.notification.model.dto.request.UpdateNotificationRuleRequest;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.repository.NotificationEventCatalogRepository;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRuleService")
class NotificationRuleServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-1";

    @Mock private NotificationRuleRepository notificationRuleRepository;
    @Mock private NotificationEventCatalogRepository eventCatalogRepository;
    @Mock private NotificationRecipientPolicyRepository recipientPolicyRepository;
    @Mock private NotificationRecipientUserRepository recipientUserRepository;
    @Mock private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationRuleService notificationRuleService;

    @Nested
    @DisplayName("getRulesForTenant")
    class GetRulesForTenant {

        @Test
        @DisplayName("maps repository results to response DTOs")
        void maps_repository_results_to_response_dtos() {
            NotificationRule rule = buildRule(UUID.randomUUID(), true);
            NotificationRuleResponse response = buildRuleResponse(rule.getId(), true);
            when(notificationRuleRepository
                    .findByTenantIdOrderByLastModifiedDateDesc(TENANT_ID))
                    .thenReturn(List.of(rule));
            when(notificationMapper.toRuleResponse(rule)).thenReturn(response);

            List<NotificationRuleResponse> result =
                    notificationRuleService.getRulesForTenant(TENANT_ID);

            assertThat(result).containsExactly(response);
        }

        @Test
        @DisplayName("returns empty list when no rules exist")
        void returns_empty_list_when_no_rules() {
            when(notificationRuleRepository
                    .findByTenantIdOrderByLastModifiedDateDesc(TENANT_ID))
                    .thenReturn(List.of());

            assertThat(notificationRuleService.getRulesForTenant(TENANT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates rule when event catalog entry found")
        void creates_rule_when_event_found() {
            UUID eventId = UUID.randomUUID();
            NotificationEventCatalog event = buildEvent(eventId);
            NotificationRule saved = buildRule(UUID.randomUUID(), true);
            NotificationRuleResponse response = buildRuleResponse(saved.getId(), true);

            CreateNotificationRuleRequest request = CreateNotificationRuleRequest.builder()
                    .eventId(eventId)
                    .severity(NotificationSeverity.INFO)
                    .isEnabled(true)
                    .build();

            when(eventCatalogRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(notificationRuleRepository.saveAndFlush(any(NotificationRule.class)))
                    .thenReturn(saved);
            when(notificationMapper.toRuleResponse(saved)).thenReturn(response);

            NotificationRuleResponse result =
                    notificationRuleService.create(TENANT_ID, USER_ID, request);

            assertThat(result).isEqualTo(response);
            verify(notificationRuleRepository).saveAndFlush(any(NotificationRule.class));
        }

        @Test
        @DisplayName("throws EntityNotFoundException when event not found")
        void throws_when_event_not_found() {
            UUID eventId = UUID.randomUUID();
            CreateNotificationRuleRequest request = CreateNotificationRuleRequest.builder()
                    .eventId(eventId).severity(NotificationSeverity.INFO).isEnabled(true).build();
            when(eventCatalogRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    notificationRuleService.create(TENANT_ID, USER_ID, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(eventId.toString());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("cascades delete: users -> policy -> rule")
        void cascades_delete_users_policy_rule() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, true);
            NotificationRecipientPolicy policy = buildPolicy(rule);
            List<NotificationRecipientUser> users = List.of(buildRecipientUser(policy));

            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(ruleId))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(users);

            notificationRuleService.delete(TENANT_ID, ruleId);

            verify(recipientUserRepository).deleteAll(users);
            verify(recipientPolicyRepository).delete(policy);
            verify(notificationRuleRepository).delete(rule);
        }

        @Test
        @DisplayName("deletes rule directly when no policy exists")
        void deletes_rule_directly_when_no_policy() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, true);

            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(ruleId)).thenReturn(Optional.empty());

            notificationRuleService.delete(TENANT_ID, ruleId);

            verify(recipientUserRepository, never()).deleteAll(anyList());
            verify(notificationRuleRepository).delete(rule);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when rule belongs to different tenant")
        void throws_when_rule_belongs_to_different_tenant() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, true);
            rule.setTenantId("other-tenant");

            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> notificationRuleService.delete(TENANT_ID, ruleId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when rule not found")
        void throws_when_rule_not_found() {
            UUID ruleId = UUID.randomUUID();
            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationRuleService.delete(TENANT_ID, ruleId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateRule")
    class UpdateRule {

        @Test
        @DisplayName("updates severity and saves")
        void updates_severity_and_saves() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, true);
            NotificationRuleResponse response = buildRuleResponse(ruleId, true);
            UpdateNotificationRuleRequest request =
                    UpdateNotificationRuleRequest.builder()
                            .severity(NotificationSeverity.WARNING).build();

            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
            when(notificationRuleRepository.save(rule)).thenReturn(rule);
            when(notificationMapper.toRuleResponse(rule)).thenReturn(response);

            NotificationRuleResponse result =
                    notificationRuleService.updateRule(TENANT_ID, USER_ID, ruleId, request);

            assertThat(result).isEqualTo(response);
            assertThat(rule.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
            verify(notificationRuleRepository).save(rule);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when wrong tenant")
        void throws_when_wrong_tenant() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, true);
            rule.setTenantId("other-tenant");
            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> notificationRuleService.updateRule(
                    TENANT_ID, USER_ID, ruleId,
                    UpdateNotificationRuleRequest.builder()
                            .severity(NotificationSeverity.INFO).build()))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleEnabled")
    class ToggleEnabled {

        @Test
        @DisplayName("toggles enabled=true to false")
        void toggles_enabled_to_disabled() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, true);
            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
            when(notificationRuleRepository.save(rule)).thenReturn(rule);
            when(notificationMapper.toRuleResponse(rule))
                    .thenReturn(buildRuleResponse(ruleId, false));

            NotificationRuleResponse result =
                    notificationRuleService.toggleEnabled(TENANT_ID, ruleId);

            assertThat(rule.getIsEnabled()).isFalse();
            assertThat(result.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("toggles enabled=false to true")
        void toggles_disabled_to_enabled() {
            UUID ruleId = UUID.randomUUID();
            NotificationRule rule = buildRule(ruleId, false);
            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
            when(notificationRuleRepository.save(rule)).thenReturn(rule);
            when(notificationMapper.toRuleResponse(rule))
                    .thenReturn(buildRuleResponse(ruleId, true));

            NotificationRuleResponse result =
                    notificationRuleService.toggleEnabled(TENANT_ID, ruleId);

            assertThat(rule.getIsEnabled()).isTrue();
            assertThat(result.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("throws EntityNotFoundException when rule not found")
        void throws_when_rule_not_found() {
            UUID ruleId = UUID.randomUUID();
            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationRuleService.toggleEnabled(TENANT_ID, ruleId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createBatch")
    class CreateBatch {

        @Test
        @DisplayName("creates rules and ALL_USERS policies for all events")
        void creates_rules_and_all_users_policies() {
            UUID eventId = UUID.randomUUID();
            NotificationEventCatalog event = buildEvent(eventId);
            NotificationRule savedRule = buildRule(UUID.randomUUID(), true);

            BatchCreateNotificationRuleRequest request = BatchCreateNotificationRuleRequest.builder()
                    .rules(List.of(CreateNotificationRuleRequest.builder()
                            .eventId(eventId).severity(NotificationSeverity.INFO)
                            .isEnabled(true).build()))
                    .recipientType("ALL_USERS")
                    .build();

            when(eventCatalogRepository.findAllById(any())).thenReturn(List.of(event));
            when(notificationRuleRepository.saveAll(any())).thenReturn(List.of(savedRule));
            when(recipientPolicyRepository.saveAll(any())).thenReturn(List.of());
            when(notificationMapper.toRuleResponse(savedRule))
                    .thenReturn(buildRuleResponse(savedRule.getId(), true));

            List<NotificationRuleResponse> result =
                    notificationRuleService.createBatch(TENANT_ID, USER_ID, request);

            assertThat(result).hasSize(1);
            verify(notificationRuleRepository).saveAll(any());
            verify(recipientPolicyRepository).saveAll(any());
            verify(recipientUserRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("saves recipient users when SELECTED_USERS with user IDs")
        void saves_recipient_users_for_selected_users() {
            UUID eventId = UUID.randomUUID();
            NotificationEventCatalog event = buildEvent(eventId);
            NotificationRule savedRule = buildRule(UUID.randomUUID(), true);
            NotificationRecipientPolicy savedPolicy = buildPolicy(savedRule);

            BatchCreateNotificationRuleRequest request = BatchCreateNotificationRuleRequest.builder()
                    .rules(List.of(CreateNotificationRuleRequest.builder()
                            .eventId(eventId).severity(NotificationSeverity.INFO)
                            .isEnabled(true).build()))
                    .recipientType("SELECTED_USERS")
                    .userIds(List.of("user-a", "user-b"))
                    .build();

            when(eventCatalogRepository.findAllById(any())).thenReturn(List.of(event));
            when(notificationRuleRepository.saveAll(any())).thenReturn(List.of(savedRule));
            when(recipientPolicyRepository.saveAll(any())).thenReturn(List.of(savedPolicy));
            when(notificationMapper.toRuleResponse(savedRule))
                    .thenReturn(buildRuleResponse(savedRule.getId(), true));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationRecipientUser>> captor =
                    ArgumentCaptor.forClass(List.class);
            notificationRuleService.createBatch(TENANT_ID, USER_ID, request);

            verify(recipientUserRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when event not found")
        void throws_when_event_not_found() {
            UUID eventId = UUID.randomUUID();
            BatchCreateNotificationRuleRequest request = BatchCreateNotificationRuleRequest.builder()
                    .rules(List.of(CreateNotificationRuleRequest.builder()
                            .eventId(eventId).severity(NotificationSeverity.INFO)
                            .isEnabled(true).build()))
                    .recipientType("ALL_USERS")
                    .build();
            when(eventCatalogRepository.findAllById(any())).thenReturn(List.of());

            assertThatThrownBy(() ->
                    notificationRuleService.createBatch(TENANT_ID, USER_ID, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for invalid recipient type")
        void throws_for_invalid_recipient_type() {
            BatchCreateNotificationRuleRequest request = BatchCreateNotificationRuleRequest.builder()
                    .rules(List.of()).recipientType("INVALID_TYPE").build();

            assertThatThrownBy(() ->
                    notificationRuleService.createBatch(TENANT_ID, USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid recipient type");
        }
    }

    private NotificationRule buildRule(UUID id, boolean isEnabled) {
        NotificationRule rule = NotificationRule.builder()
                .severity(NotificationSeverity.INFO)
                .isEnabled(isEnabled)
                .build();
        rule.setId(id);
        rule.setTenantId(TENANT_ID);
        return rule;
    }

    private NotificationRuleResponse buildRuleResponse(UUID id, boolean isEnabled) {
        return NotificationRuleResponse.builder()
                .id(id).tenantId(TENANT_ID)
                .severity(NotificationSeverity.INFO).isEnabled(isEnabled).build();
    }

    private NotificationEventCatalog buildEvent(UUID id) {
        return NotificationEventCatalog.builder()
                .id(id).eventKey("ARCGIS_INTEGRATION_CREATED")
                .entityType(NotificationEntityType.ARCGIS_INTEGRATION)
                .displayName("ArcGIS Created").isEnabled(true).build();
    }

    private NotificationRecipientPolicy buildPolicy(NotificationRule rule) {
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .rule(rule).recipientType(RecipientType.ALL_USERS).build();
        policy.setId(UUID.randomUUID());
        policy.setTenantId(TENANT_ID);
        return policy;
    }

    private NotificationRecipientUser buildRecipientUser(NotificationRecipientPolicy policy) {
        return NotificationRecipientUser.builder()
                .tenantId(TENANT_ID).recipientPolicy(policy).userId("user-a").build();
    }
}
