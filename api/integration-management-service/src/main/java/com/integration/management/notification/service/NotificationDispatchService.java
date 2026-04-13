package com.integration.management.notification.service;

import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.repository.AppNotificationRepository;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import com.integration.management.notification.sse.SseEmitterRegistry;
import com.integration.management.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationTemplateService notificationTemplateService;
    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationRecipientPolicyRepository recipientPolicyRepository;
    private final NotificationRecipientUserRepository recipientUserRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final UserProfileService userProfileService;
    private final NotificationMapper notificationMapper;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Transactional
    public void dispatch(String eventKey, String tenantId, String triggeredByUserId,
                         Map<String, Object> metadata) {
        log.info("Dispatching notification for event: {} tenant: {}", eventKey, tenantId);

        Map<String, Object> effectiveMetadata = enrichMetadata(eventKey, triggeredByUserId, metadata);

        Optional<NotificationRule> ruleOpt =
                notificationRuleRepository
                        .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, tenantId);

        if (ruleOpt.isEmpty()) {
            log.warn("Skipping event. No enabled rule found. eventKey={}, tenantId={}", eventKey, tenantId);
            return;
        }

        NotificationRule rule = ruleOpt.get();

        Optional<NotificationRecipientPolicy> policyOpt =
                recipientPolicyRepository.findByRuleId(rule.getId());

        if (policyOpt.isEmpty()) {
            log.warn("Skipping event. No recipient policy found. ruleId={}", rule.getId());
            return;
        }

        NotificationRecipientPolicy policy = policyOpt.get();

        // 3. Resolve target user IDs
        List<String> targetUserIds = resolveTargetUserIds(policy, tenantId);

        // 3a. Exclude the initiator when notify_initiator = false on the event catalog
        if (!Boolean.TRUE.equals(rule.getEvent().getNotifyInitiator())
                && triggeredByUserId != null && !triggeredByUserId.isBlank()) {
            targetUserIds = targetUserIds.stream()
                    .filter(uid -> !uid.equals(triggeredByUserId))
                    .toList();
            log.debug("Excluded initiator '{}' from notification recipients (notify_initiator=false)",
                    triggeredByUserId);
        }

        if (targetUserIds.isEmpty()) {
            log.debug("No target users resolved for policy: {} recipientType: {}",
                    policy.getId(), policy.getRecipientType());
            return;
        }

        // 4. Build title/message — prefer DB template, fall back to defaults
        NotificationTemplateResponse template = resolveTemplate(tenantId, eventKey);
        String title = buildTitle(eventKey, template, effectiveMetadata);
        String message = buildMessage(eventKey, template, effectiveMetadata);
        NotificationEventKey notifType = resolveNotificationEventKey(eventKey);

        // 5. Persist all notifications; collect (userId → response) pairs for SSE
        // dispatch
        List<SsePayload> ssePending = new ArrayList<>();
        for (String userId : targetUserIds) {
            try {
                AppNotification notification = AppNotification.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .type(notifType)
                        .severity(rule.getSeverity())
                        .title(title)
                        .message(message)
                        .metadata(effectiveMetadata)
                        .isRead(false)
                        .build();
                AppNotification persisted = appNotificationRepository.save(notification);
                ssePending.add(new SsePayload(userId, notificationMapper.toAppNotificationResponse(persisted)));
                log.debug("Notification persisted for user: {}", userId);
            } catch (Exception ex) {
                log.error("Failed to persist notification for user: {}", userId, ex);
            }
        }

        if (ssePending.isEmpty()) {
            return;
        }

        // 6. Register afterCommit callback — SSE is pushed only after the DB
        // transaction
        // fully commits, guaranteeing the frontend always sees the saved rows when it
        // immediately calls GET /notifications/count upon receiving the push event.
        // This fixes the race condition that caused stale (0) bell counts on sandbox.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (SsePayload payload : ssePending) {
                    sseEmitterRegistry.send(payload.userId(), payload.response());
                    log.debug("SSE pushed to user: {}", payload.userId());
                }
                log.info("Dispatched '{}' notification to {} users for tenant: {}",
                        eventKey, ssePending.size(), tenantId);
            }
        });
    }

    private record SsePayload(String userId, AppNotificationResponse response) {
    }

    private Map<String, Object> enrichMetadata(String eventKey, String triggeredByUserId,
                                               Map<String, Object> metadata) {
        Map<String, Object> effectiveMetadata = new HashMap<>();
        if (metadata != null && !metadata.isEmpty()) {
            effectiveMetadata.putAll(metadata);
        }

        if (!effectiveMetadata.containsKey("configSection")) {
            Object configKey = effectiveMetadata.get("configKey");
            if (configKey instanceof String s && !s.isBlank()) {
                effectiveMetadata.put("configSection", s);
            } else if (configKey != null) {
                effectiveMetadata.put("configSection", configKey);
            }
        }

        if (triggeredByUserId == null || triggeredByUserId.isBlank()) {
            return effectiveMetadata;
        }

        effectiveMetadata.putIfAbsent("triggeredBy", triggeredByUserId);

        String byKey = resolveByKey(eventKey);
        if (byKey != null) {
            effectiveMetadata.putIfAbsent(byKey, triggeredByUserId);
        }

        return effectiveMetadata;
    }

    private String resolveByKey(String eventKey) {
        if (eventKey == null) {
            return null;
        }
        if (eventKey.endsWith("_CREATED")) {
            return "createdBy";
        }
        if (eventKey.endsWith("_UPDATED")) {
            return "updatedBy";
        }
        if (eventKey.endsWith("_ENABLED")) {
            return "enabledBy";
        }
        if (eventKey.endsWith("_DISABLED")) {
            return "disabledBy";
        }
        if (eventKey.endsWith("_DELETED")) {
            return "deletedBy";
        }
        return null;
    }

    private List<String> resolveTargetUserIds(NotificationRecipientPolicy policy, String tenantId) {
        return switch (policy.getRecipientType()) {
            case ALL_USERS -> userProfileService.getAllUsersByTenant(tenantId)
                    .stream()
                    .map(UserProfileResponse::getKeycloakUserId)
                    .toList();
            case ADMINS_ONLY -> userProfileService.getAllUsersByTenant(tenantId)
                    .stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsTenantAdmin()))
                    .map(UserProfileResponse::getKeycloakUserId)
                    .toList();
            case SELECTED_USERS -> recipientUserRepository
                    .findByRecipientPolicyId(policy.getId())
                    .stream()
                    .map(NotificationRecipientUser::getUserId)
                    .toList();
        };
    }

    private NotificationEventKey resolveNotificationEventKey(String eventKey) {
        try {
            return NotificationEventKey.valueOf(eventKey);
        } catch (IllegalArgumentException ex) {
            // eventKey has no matching NotificationEventKey entry — this indicates a missing enum value.
            // Log a warning so it is visible; fall back to SITE_CONFIG_UPDATED to avoid a hard failure.
            log.warn("Unknown eventKey '{}' has no matching NotificationEventKey entry — "
                    + "add it to NotificationEventKey enum. Falling back to SITE_CONFIG_UPDATED.", eventKey);
            return NotificationEventKey.SITE_CONFIG_UPDATED;
        }
    }

    private NotificationTemplateResponse resolveTemplate(String tenantId, String eventKey) {
        return notificationTemplateService.getTemplatesForTenant(tenantId).stream()
                .filter(t -> t.getEventKey().equals(eventKey))
                .findFirst()
                .orElse(null);
    }

    private String buildTitle(String eventKey, NotificationTemplateResponse template, Map<String, Object> metadata) {
        if (template != null) {
            return renderTemplate(template.getTitleTemplate(), metadata);
        }
        return eventKey.replace('_', ' ').toLowerCase();
    }

    private String buildMessage(String eventKey, NotificationTemplateResponse template, Map<String, Object> metadata) {
        if (template != null) {
            return renderTemplate(template.getMessageTemplate(), metadata);
        }
        return "Event " + eventKey + " occurred.";
    }

    private String renderTemplate(String template, Map<String, Object> metadata) {
        if (template == null) {
            return "";
        }
        String result = template.replace("{{timestamp}}", Instant.now().toString());
        if (metadata == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

}
