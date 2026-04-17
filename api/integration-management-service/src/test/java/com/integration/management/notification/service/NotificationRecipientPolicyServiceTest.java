package com.integration.management.notification.service;

import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.request.CreateRecipientPolicyRequest;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import com.integration.management.service.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRecipientPolicyService")
class NotificationRecipientPolicyServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-1";

    @Mock private NotificationRecipientPolicyRepository recipientPolicyRepository;
    @Mock private NotificationRecipientUserRepository recipientUserRepository;
    @Mock private NotificationRuleRepository notificationRuleRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private UserProfileService userProfileService;

    @InjectMocks
    private NotificationRecipientPolicyService notificationRecipientPolicyService;

    @Nested
    @DisplayName("getPoliciesForTenant")
    class GetPoliciesForTenant {

        @Test
        @DisplayName("returns empty list when no policies exist")
        void returns_empty_list_when_no_policies() {
            when(recipientPolicyRepository.findByTenantIdOrderByLastModifiedDateDesc(TENANT_ID))
                    .thenReturn(List.of());

            assertThat(notificationRecipientPolicyService.getPoliciesForTenant(TENANT_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("builds response with resolved user info for SELECTED_USERS policy")
        void builds_response_with_user_info() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.SELECTED_USERS);
            RecipientPolicyResponse response = buildPolicyResponse(policy.getId());

            when(recipientPolicyRepository.findByTenantIdOrderByLastModifiedDateDesc(TENANT_ID))
                    .thenReturn(List.of(policy));
            when(recipientUserRepository.findByRecipientPolicyIdIn(List.of(policy.getId())))
                    .thenReturn(List.of(
                            buildRecipientUser(policy, "user-a"),
                            buildRecipientUser(policy, "user-b")));
            when(notificationMapper.toRecipientPolicyResponse(policy)).thenReturn(response);
            when(userProfileService.getUserProfileMapByTenant(TENANT_ID))
                    .thenReturn(Map.of(
                            "user-a", buildProfile("user-a", "Alice"),
                            "user-b", buildProfile("user-b", "Bob")));

            List<RecipientPolicyResponse> result =
                    notificationRecipientPolicyService.getPoliciesForTenant(TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getUsers()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates ALL_USERS policy without user rows")
        void creates_all_users_policy_without_user_rows() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy saved = buildPolicy(rule, RecipientType.ALL_USERS);
            RecipientPolicyResponse response = buildPolicyResponse(saved.getId());
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(rule.getId()).recipientType("ALL_USERS").build();

            when(notificationRuleRepository.findById(rule.getId()))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toRecipientPolicyResponse(saved)).thenReturn(response);

            RecipientPolicyResponse result =
                    notificationRecipientPolicyService.create(TENANT_ID, USER_ID, request);

            assertThat(result).isEqualTo(response);
            verify(recipientUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates SELECTED_USERS policy and saves user rows")
        void creates_selected_users_policy_and_saves_user_rows() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy saved = buildPolicy(rule, RecipientType.SELECTED_USERS);
            RecipientPolicyResponse response = buildPolicyResponse(saved.getId());
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(rule.getId()).recipientType("SELECTED_USERS")
                    .userIds(List.of("user-a", "user-b")).build();

            when(notificationRuleRepository.findById(rule.getId()))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toRecipientPolicyResponse(saved)).thenReturn(response);
            when(userProfileService.getUserProfileMapByTenant(TENANT_ID)).thenReturn(Map.of());

            notificationRecipientPolicyService.create(TENANT_ID, USER_ID, request);

            verify(recipientUserRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("creates SELECTED_USERS policy with null userIds - skips user rows and resolveUsers")
        void creates_selected_users_policy_with_null_userIds_skips_user_rows() {
            // Covers: RecipientType.SELECTED_USERS && getUserIds() == null → skip user save + resolveUsers
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy saved = buildPolicy(rule, RecipientType.SELECTED_USERS);
            RecipientPolicyResponse response = buildPolicyResponse(saved.getId());
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(rule.getId()).recipientType("SELECTED_USERS")
                    .userIds(null)  // null userIds
                    .build();

            when(notificationRuleRepository.findById(rule.getId()))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toRecipientPolicyResponse(saved)).thenReturn(response);

            RecipientPolicyResponse result =
                    notificationRecipientPolicyService.create(TENANT_ID, USER_ID, request);

            assertThat(result).isEqualTo(response);
            verify(recipientUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for invalid recipient type")
        void throws_for_invalid_recipient_type() {
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(UUID.randomUUID()).recipientType("INVALID").build();

            assertThatThrownBy(() ->
                    notificationRecipientPolicyService.create(TENANT_ID, USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid recipient type");
        }

        @Test
        @DisplayName("throws EntityNotFoundException when rule not found for tenant")
        void throws_when_rule_not_found() {
            UUID ruleId = UUID.randomUUID();
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(ruleId).recipientType("ALL_USERS").build();
            when(notificationRuleRepository.findById(ruleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    notificationRecipientPolicyService.create(TENANT_ID, USER_ID, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes user rows then policy")
        void deletes_users_then_policy() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.SELECTED_USERS);
            NotificationRecipientUser user = buildRecipientUser(policy, "user-a");

            when(recipientPolicyRepository.findById(policy.getId()))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of(user));

            notificationRecipientPolicyService.delete(TENANT_ID, policy.getId());

            verify(recipientUserRepository).deleteAll(List.of(user));
            verify(recipientPolicyRepository).delete(policy);
        }

        @Test
        @DisplayName("deletes policy without user row deletion when empty")
        void deletes_policy_without_user_deletion_when_empty() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);

            when(recipientPolicyRepository.findById(policy.getId()))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of());

            notificationRecipientPolicyService.delete(TENANT_ID, policy.getId());

            verify(recipientUserRepository, never()).deleteAll(any());
            verify(recipientPolicyRepository).delete(policy);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when not found")
        void throws_when_not_found() {
            UUID policyId = UUID.randomUUID();
            when(recipientPolicyRepository.findById(policyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    notificationRecipientPolicyService.delete(TENANT_ID, policyId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("replaces policy type and recreates user rows for SELECTED_USERS")
        void replaces_type_and_recreates_user_rows() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            NotificationRecipientPolicy updated = buildPolicy(rule, RecipientType.SELECTED_USERS);
            RecipientPolicyResponse response = buildPolicyResponse(policy.getId());
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(rule.getId()).recipientType("SELECTED_USERS")
                    .userIds(List.of("user-a")).build();

            when(recipientPolicyRepository.findById(policy.getId()))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of());
            when(recipientPolicyRepository.save(policy)).thenReturn(updated);
            when(notificationMapper.toRecipientPolicyResponse(updated)).thenReturn(response);
            when(userProfileService.getUserProfileMapByTenant(TENANT_ID)).thenReturn(Map.of());

            notificationRecipientPolicyService
                    .update(TENANT_ID, USER_ID, policy.getId(), request);

            verify(recipientUserRepository).save(any());
        }

        @Test
        @DisplayName("updates to ALL_USERS and deletes existing user rows")
        void updates_to_all_users_and_deletes_existing_rows() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy =
                    buildPolicy(rule, RecipientType.SELECTED_USERS);
            NotificationRecipientUser existing = buildRecipientUser(policy, "user-a");
            RecipientPolicyResponse response = buildPolicyResponse(policy.getId());
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(rule.getId()).recipientType("ALL_USERS").build();

            when(recipientPolicyRepository.findById(policy.getId()))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of(existing));
            when(recipientPolicyRepository.save(policy)).thenReturn(policy);
            when(notificationMapper.toRecipientPolicyResponse(policy)).thenReturn(response);

            notificationRecipientPolicyService
                    .update(TENANT_ID, USER_ID, policy.getId(), request);

            verify(recipientUserRepository).deleteAllInBatch(List.of(existing));
            verify(recipientUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("updates SELECTED_USERS policy with null userIds - skips user save and resolveUsers")
        void update_selected_users_with_null_userIds_skips_user_rows() {
            // Covers: RecipientType.SELECTED_USERS && getUserIds() == null → skip user save + resolveUsers
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            RecipientPolicyResponse response = buildPolicyResponse(policy.getId());
            CreateRecipientPolicyRequest request = CreateRecipientPolicyRequest.builder()
                    .ruleId(rule.getId()).recipientType("SELECTED_USERS")
                    .userIds(null)  // null userIds
                    .build();

            when(recipientPolicyRepository.findById(policy.getId()))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of());
            when(recipientPolicyRepository.save(policy)).thenReturn(policy);
            when(notificationMapper.toRecipientPolicyResponse(policy)).thenReturn(response);

            notificationRecipientPolicyService.update(TENANT_ID, USER_ID, policy.getId(), request);

            verify(recipientUserRepository, never()).save(any());
        }
    }

    private NotificationRule buildRule() {
        NotificationRule rule = NotificationRule.builder()
                .severity(NotificationSeverity.INFO).isEnabled(true).build();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(TENANT_ID);
        return rule;
    }

    private NotificationRecipientPolicy buildPolicy(NotificationRule rule, RecipientType type) {
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .rule(rule).recipientType(type).build();
        policy.setId(UUID.randomUUID());
        policy.setTenantId(TENANT_ID);
        return policy;
    }

    private NotificationRecipientUser buildRecipientUser(
            NotificationRecipientPolicy policy, String userId) {
        return NotificationRecipientUser.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID)
                .recipientPolicy(policy).userId(userId).build();
    }

    private RecipientPolicyResponse buildPolicyResponse(UUID id) {
        return RecipientPolicyResponse.builder()
                .id(id).tenantId(TENANT_ID).recipientType("ALL_USERS").build();
    }

    private UserProfileResponse buildProfile(String userId, String displayName) {
        return UserProfileResponse.builder()
                .keycloakUserId(userId).displayName(displayName)
                .email(userId + "@test.com").build();
    }
}
