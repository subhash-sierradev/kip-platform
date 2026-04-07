package com.integration.management.notification.service;

import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.request.CreateRecipientPolicyRequest;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import com.integration.management.notification.model.dto.response.RecipientUserInfo;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import com.integration.management.service.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRecipientPolicyService {

    private final NotificationRecipientPolicyRepository recipientPolicyRepository;
    private final NotificationRecipientUserRepository recipientUserRepository;
    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationMapper notificationMapper;
    private final UserProfileService userProfileService;

    private List<RecipientUserInfo> resolveUsers(List<String> userIds, String tenantId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserProfileResponse> profileMap = userProfileService.getUserProfileMapByTenant(tenantId);
        return userIds.stream()
            .map(uid -> {
                UserProfileResponse profile = profileMap.get(uid);
                return RecipientUserInfo.builder()
                    .userId(uid)
                    .displayName(profile != null ? profile.getDisplayName() : uid)
                    .email(profile != null ? profile.getEmail() : null)
                    .build();
            })
            .toList();
    }

    @Cacheable(value = "notificationPoliciesByTenantCache", key = "#tenantId")
    @Transactional
    public List<RecipientPolicyResponse> getPoliciesForTenant(String tenantId) {
        List<NotificationRecipientPolicy> policies =
            recipientPolicyRepository.findByTenantIdOrderByLastModifiedDateDesc(tenantId);
        if (policies.isEmpty()) {
            return Collections.emptyList();
        }
        // Batch-fetch all recipient-user rows for all policies in one IN query
        List<UUID> policyIds = policies.stream().map(NotificationRecipientPolicy::getId).toList();
        Map<UUID, List<String>> userIdsByPolicyId = recipientUserRepository
            .findByRecipientPolicyIdIn(policyIds)
            .stream()
            .collect(Collectors.groupingBy(
                ru -> ru.getRecipientPolicy().getId(),
                Collectors.mapping(NotificationRecipientUser::getUserId, Collectors.toList())
            ));
        return policies.stream()
            .map(policy -> {
                RecipientPolicyResponse response = notificationMapper.toRecipientPolicyResponse(policy);
                List<String> userIds =
                    userIdsByPolicyId.getOrDefault(policy.getId(), Collections.emptyList());
                response.setUsers(resolveUsers(userIds, tenantId));
                return response;
            })
            .toList();
    }

    @CacheEvict(value = "notificationPoliciesByTenantCache", key = "#tenantId")
    @Transactional
    public RecipientPolicyResponse create(String tenantId, String userId, CreateRecipientPolicyRequest request) {
        log.info("Creating recipient policy for tenant: {}, rule: {}", tenantId, request.getRuleId());
        RecipientType recipientType;
        try {
            recipientType = RecipientType.valueOf(request.getRecipientType());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid recipient type: " + request.getRecipientType());
        }
        NotificationRule rule = notificationRuleRepository.findById(request.getRuleId())
            .filter(r -> r.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException(
                "Notification rule not found: " + request.getRuleId()));
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
            .tenantId(tenantId)
            .rule(rule)
            .recipientType(recipientType)
            .createdBy(userId)
            .lastModifiedBy(userId)
            .build();
        NotificationRecipientPolicy savedPolicy = recipientPolicyRepository.save(policy);
        if (RecipientType.SELECTED_USERS.equals(recipientType) && request.getUserIds() != null) {
            for (String recipientUserId : request.getUserIds()) {
                recipientUserRepository.save(NotificationRecipientUser.builder()
                    .tenantId(tenantId)
                    .recipientPolicy(savedPolicy)
                    .userId(recipientUserId)
                    .build());
            }
        }
        RecipientPolicyResponse response = notificationMapper.toRecipientPolicyResponse(savedPolicy);
        if (RecipientType.SELECTED_USERS.equals(recipientType) && request.getUserIds() != null) {
            response.setUsers(resolveUsers(request.getUserIds(), tenantId));
        }
        return response;
    }

    @CacheEvict(value = "notificationPoliciesByTenantCache", key = "#tenantId", beforeInvocation = true)
    @Transactional
    public void delete(String tenantId, UUID policyId) {
        log.info("Deleting recipient policy: {} for tenant: {}", policyId, tenantId);
        NotificationRecipientPolicy policy = recipientPolicyRepository.findById(policyId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException(
                "Recipient policy not found: " + policyId));
        List<NotificationRecipientUser> userRows = recipientUserRepository.findByRecipientPolicyId(policyId);
        if (!userRows.isEmpty()) {
            recipientUserRepository.deleteAll(userRows);
        }
        recipientPolicyRepository.delete(policy);
    }

    @CacheEvict(value = "notificationPoliciesByTenantCache", key = "#tenantId", beforeInvocation = true)
    @Transactional
    public RecipientPolicyResponse update(String tenantId, String userId, UUID policyId,
            CreateRecipientPolicyRequest request) {
        log.info("Updating recipient policy: {} for tenant: {}", policyId, tenantId);
        RecipientType recipientType;
        try {
            recipientType = RecipientType.valueOf(request.getRecipientType());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid recipient type: " + request.getRecipientType());
        }
        NotificationRecipientPolicy policy = recipientPolicyRepository.findById(policyId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Recipient policy not found: " + policyId));
        policy.setRecipientType(recipientType);
        policy.setLastModifiedBy(userId);
        policy.setLastModifiedDate(Instant.now());
        NotificationRecipientPolicy saved = recipientPolicyRepository.save(policy);
        List<NotificationRecipientUser> existing = recipientUserRepository.findByRecipientPolicyId(policyId);
        if (!existing.isEmpty()) {
            recipientUserRepository.deleteAllInBatch(existing);
        }
        if (RecipientType.SELECTED_USERS.equals(recipientType) && request.getUserIds() != null) {
            for (String recipientUserId : request.getUserIds()) {
                recipientUserRepository.save(NotificationRecipientUser.builder()
                    .tenantId(tenantId)
                    .recipientPolicy(saved)
                    .userId(recipientUserId)
                    .build());
            }
        }
        RecipientPolicyResponse response = notificationMapper.toRecipientPolicyResponse(saved);
        if (RecipientType.SELECTED_USERS.equals(recipientType) && request.getUserIds() != null) {
            response.setUsers(resolveUsers(request.getUserIds(), tenantId));
        }
        return response;
    }
}
