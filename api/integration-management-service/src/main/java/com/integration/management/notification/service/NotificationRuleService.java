package com.integration.management.notification.service;

import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRuleService {

    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationEventCatalogRepository eventCatalogRepository;
    private final NotificationRecipientPolicyRepository recipientPolicyRepository;
    private final NotificationRecipientUserRepository recipientUserRepository;
    private final NotificationMapper notificationMapper;

    @Cacheable(value = "notificationRulesByTenantCache", key = "#tenantId")
    public List<NotificationRuleResponse> getRulesForTenant(String tenantId) {
        log.debug("Fetching notification rules for tenant: {}", tenantId);
        return notificationRuleRepository.findByTenantIdOrderByLastModifiedDateDesc(tenantId)
                .stream()
                .map(notificationMapper::toRuleResponse)
                .toList();
    }

    @CacheEvict(value = "notificationRulesByTenantCache", key = "#tenantId")
    @Transactional
    public NotificationRuleResponse create(String tenantId, String userId, CreateNotificationRuleRequest request) {
        log.info("Creating notification rule for tenant: {} with event: {}", tenantId, request.getEventId());
        NotificationEventCatalog event = eventCatalogRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification event catalog entry not found: " + request.getEventId()));
        NotificationRule rule = NotificationRule.builder()
                .tenantId(tenantId)
                .event(event)
                .severity(request.getSeverity())
                .isEnabled(request.getIsEnabled())
                .createdBy(userId)
                .lastModifiedBy(userId)
                .build();
        return notificationMapper.toRuleResponse(notificationRuleRepository.saveAndFlush(rule));
    }

    @Caching(evict = {
        @CacheEvict(value = "notificationRulesByTenantCache", key = "#tenantId", beforeInvocation = true),
        @CacheEvict(value = "notificationPoliciesByTenantCache", key = "#tenantId", beforeInvocation = true)
    })
    @Transactional
    public void delete(String tenantId, UUID ruleId) {
        log.info("Deleting notification rule: {} for tenant: {}", ruleId, tenantId);
        NotificationRule rule = notificationRuleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification rule not found: " + ruleId));
        recipientPolicyRepository.findByRuleId(ruleId).ifPresent(policy -> {
            recipientUserRepository.deleteAll(
                    recipientUserRepository.findByRecipientPolicyId(policy.getId()));
            recipientPolicyRepository.delete(policy);
        });
        notificationRuleRepository.delete(rule);
    }

    @CacheEvict(value = "notificationRulesByTenantCache", key = "#tenantId", beforeInvocation = true)
    @Transactional
    public NotificationRuleResponse updateRule(String tenantId, String userId, UUID ruleId,
            UpdateNotificationRuleRequest request) {
        log.info("Updating notification rule: {} for tenant: {}", ruleId, tenantId);
        NotificationRule rule = notificationRuleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification rule not found: " + ruleId));
        rule.setSeverity(request.getSeverity());
        rule.setLastModifiedBy(userId);
        return notificationMapper.toRuleResponse(notificationRuleRepository.save(rule));
    }

    @CacheEvict(value = "notificationRulesByTenantCache", key = "#tenantId", beforeInvocation = true)
    @Transactional
    public NotificationRuleResponse toggleEnabled(String tenantId, UUID ruleId) {
        log.info("Toggling enabled state for notification rule: {} for tenant: {}", ruleId, tenantId);
        NotificationRule rule = notificationRuleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification rule not found: " + ruleId));
        rule.setIsEnabled(!rule.getIsEnabled());
        return notificationMapper.toRuleResponse(notificationRuleRepository.save(rule));
    }

    @Caching(evict = {
        @CacheEvict(value = "notificationRulesByTenantCache", key = "#tenantId"),
        @CacheEvict(value = "notificationPoliciesByTenantCache", key = "#tenantId")
    })
    @Transactional
    public List<NotificationRuleResponse> createBatch(
            String tenantId, String userId, BatchCreateNotificationRuleRequest request) {
        log.info("Creating {} notification rules in batch for tenant: {}",
                request.getRules().size(), tenantId);
        RecipientType recipientType = toRecipientType(request.getRecipientType());
        Map<UUID, NotificationEventCatalog> eventMap = fetchAndValidateEvents(request.getRules());
        List<NotificationRule> savedRules = saveRules(tenantId, userId, request.getRules(), eventMap);
        savePoliciesAndUsers(tenantId, userId, savedRules, recipientType, request.getUserIds());
        return savedRules.stream().map(notificationMapper::toRuleResponse).toList();
    }

    private RecipientType toRecipientType(String value) {
        try {
            return RecipientType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid recipient type: " + value);
        }
    }

    private Map<UUID, NotificationEventCatalog> fetchAndValidateEvents(
            List<CreateNotificationRuleRequest> ruleRequests) {
        List<UUID> eventIds = ruleRequests.stream()
                .map(CreateNotificationRuleRequest::getEventId).toList();
        Map<UUID, NotificationEventCatalog> eventMap = eventCatalogRepository.findAllById(eventIds)
                .stream().collect(Collectors.toMap(NotificationEventCatalog::getId, e -> e));
        List<UUID> missing = eventIds.stream().filter(id -> !eventMap.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new EntityNotFoundException(
                    "Notification event catalog entries not found: " + missing);
        }
        return eventMap;
    }

    private List<NotificationRule> saveRules(String tenantId, String userId,
            List<CreateNotificationRuleRequest> ruleRequests,
            Map<UUID, NotificationEventCatalog> eventMap) {
        List<NotificationRule> rules = ruleRequests.stream()
                .map(req -> buildRule(tenantId, userId, req, eventMap))
                .toList();
        return notificationRuleRepository.saveAll(rules);
    }

    private NotificationRule buildRule(String tenantId, String userId,
            CreateNotificationRuleRequest req, Map<UUID, NotificationEventCatalog> eventMap) {
        return NotificationRule.builder()
                .tenantId(tenantId)
                .event(eventMap.get(req.getEventId()))
                .severity(req.getSeverity())
                .isEnabled(req.getIsEnabled())
                .createdBy(userId)
                .lastModifiedBy(userId)
                .build();
    }

    private void savePoliciesAndUsers(String tenantId, String userId, List<NotificationRule> savedRules,
            RecipientType recipientType, List<String> userIds) {
        List<NotificationRecipientPolicy> policies = savedRules.stream()
                .<NotificationRecipientPolicy>map(rule -> NotificationRecipientPolicy.builder()
                        .tenantId(tenantId)
                        .rule(rule)
                        .recipientType(recipientType)
                        .createdBy(userId)
                        .lastModifiedBy(userId)
                        .build())
                .toList();
        List<NotificationRecipientPolicy> savedPolicies = recipientPolicyRepository.saveAll(policies);
        if (RecipientType.SELECTED_USERS.equals(recipientType)
                && userIds != null && !userIds.isEmpty()) {
            recipientUserRepository.saveAll(buildUserRecipients(tenantId, savedPolicies, userIds));
        }
    }

    private List<NotificationRecipientUser> buildUserRecipients(String tenantId,
            List<NotificationRecipientPolicy> savedPolicies, List<String> userIds) {
        return savedPolicies.stream()
                .flatMap(policy -> userIds.stream()
                        .map(uid -> NotificationRecipientUser.builder()
                                .tenantId(tenantId)
                                .recipientPolicy(policy)
                                .userId(uid)
                                .build()))
                .toList();
    }
}
