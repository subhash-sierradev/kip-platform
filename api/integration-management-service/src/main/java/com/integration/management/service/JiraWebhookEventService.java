package com.integration.management.service;

import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.management.constants.ManagementSecurityConstants;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.IntegrationPersistenceException;
import com.integration.management.mapper.JiraFieldMappingMapper;
import com.integration.management.mapper.JiraWebhookEventMapper;
import com.integration.management.model.dto.response.JiraWebhookEventResponse;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraWebhookEventService {

    private final JiraWebhookEventRepository triggerHistoryRepository;
    private final JiraWebhookRepository jiraWebhookRepository;
    private final JiraWebhookEventMapper mapper;
    private final JiraFieldMappingMapper jiraFieldMappingMapper;
    private final MessagePublisher messagePublisher;
    private final IntegrationConnectionService integrationConnectionService;

    public ResponseEntity<JiraWebhookEventResponse> retryTrigger(
            final String id, final String tenantId, final String userId) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Trigger ID must not be null or empty");
        }
        log.info("Retrying webhook trigger: {} for tenant: {} by user: {}", id, tenantId, userId);

        JiraWebhookEvent lastEvent = findByOriginalEventIdOrderByRetryAttempt(id);
        JiraWebhook webhook = findWebhook(lastEvent.getWebhookId(), tenantId);

        String originalEventId = StringUtils.hasText(lastEvent.getOriginalEventId())
                ? lastEvent.getOriginalEventId() : id;
        int nextRetry = lastEvent.getRetryAttempt() != null ? lastEvent.getRetryAttempt() + 1 : 1;

        JiraWebhookEvent retryEvent = recordJiraWebhookEvent(
                lastEvent.getWebhookId(), tenantId, userId,
                lastEvent.getIncomingPayload(), originalEventId, nextRetry);

        JiraWebhookExecutionCommand command = buildCommand(
                webhook, retryEvent, lastEvent.getIncomingPayload(), tenantId, userId);

        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command);
        log.info("Published retry command for event: {} to queue", retryEvent.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(retryEvent));
    }

    public ResponseEntity<JiraWebhookEventResponse> executeWebhook(
            final String id, final String jiraWebhookPayload,
            final String tenantId, final String userId) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Webhook ID must not be null or empty");
        }
        log.info("Executing webhook: {} with payload length: {} for caller tenant: {} by user: {}",
                id, jiraWebhookPayload != null ? jiraWebhookPayload.length() : 0, tenantId, userId);

        JiraWebhook webhook;
        if (ManagementSecurityConstants.GLOBAL.equals(tenantId)) {
            webhook = findWebhookByIdIgnoringTenant(id);
        } else {
            webhook = findWebhook(id, tenantId);
        }
        String effectiveTenantId = webhook.getTenantId();
        log.info("Effective tenant for webhook {}: {} (caller tenant: {})", id, effectiveTenantId, tenantId);

        JiraWebhookEvent event = recordJiraWebhookEvent(
                id, effectiveTenantId, userId, jiraWebhookPayload, null, 0);

        JiraWebhookExecutionCommand command = buildCommand(
                webhook, event, jiraWebhookPayload, effectiveTenantId, userId);


        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command);
        log.info("Published execution command for event: {} to queue", event.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(event));
    }

    private JiraWebhook findWebhook(final String webhookId, final String tenantId) {
        return jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(webhookId, tenantId)
                .orElseThrow(() -> new IntegrationNotFoundException(
                        "Jira webhook not found with ID: " + webhookId + " for tenant: " + tenantId));
    }

    private JiraWebhook findWebhookByIdIgnoringTenant(final String webhookId) {
        return jiraWebhookRepository.findByIdIgnoringTenantAndIsDeletedFalse(webhookId)
                .orElseThrow(() -> new IntegrationNotFoundException(
                        "Jira webhook not found with ID: " + webhookId));
    }

    private JiraWebhookExecutionCommand buildCommand(
            final JiraWebhook webhook, final JiraWebhookEvent event,
            final String incomingPayload, final String tenantId, final String userId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(webhook.getConnectionId().toString(), tenantId);
        return JiraWebhookExecutionCommand.builder()
                .webhookId(webhook.getId())
                .connectionSecretName(secretName)
                .webhookName(webhook.getName())
                .samplePayload(webhook.getSamplePayload())
                .fieldMappings(webhook.getJiraFieldMappings() == null
                        ? List.of()
                        : webhook.getJiraFieldMappings().stream()
                                .map(jiraFieldMappingMapper::toDto)
                                .collect(Collectors.toList()))
                .incomingPayload(incomingPayload)
                .triggerEventId(event.getId())
                .tenantId(tenantId)
                .triggeredBy(userId)
                .originalEventId(event.getOriginalEventId())
                .retryAttempt(event.getRetryAttempt() != null ? event.getRetryAttempt() : 0)
                .build();
    }

    public void applyResult(final JiraWebhookExecutionResult result) {
        if (result == null || result.getTriggerEventId() == null) {
            log.warn("Received null or incomplete JiraWebhookExecutionResult — skipping persistence");
            return;
        }
        TriggerStatus status = result.isSuccess() ? TriggerStatus.SUCCESS : TriggerStatus.FAILED;
        updateTriggerResult(result.getTriggerEventId(), ManagementSecurityConstants.SYSTEM_USER, status,
                result.getTransformedPayload(),
                result.getResponseStatusCode() != 0 ? result.getResponseStatusCode() : null,
                result.getResponseBody(),
                result.getJiraIssueUrl());
    }

    @Transactional
    public JiraWebhookEvent recordJiraWebhookEvent(final String webhookId, final String tenantId,
            final String userId, final String incomingPayload,
            final String originalEventId, final int retryAttempt) {
        log.info("Recording original trigger for webhook: {} by user: {}", webhookId, userId);
        try {
            JiraWebhookEvent trigger = JiraWebhookEvent.builder()
                    .triggeredBy(userId)
                    .triggeredAt(Instant.now())
                    .webhookId(webhookId)
                    .tenantId(tenantId)
                    .incomingPayload(incomingPayload)
                    .status(TriggerStatus.PENDING)
                    .retryAttempt(retryAttempt)
                    .build();

            if (StringUtils.hasText(originalEventId)) {
                trigger.setOriginalEventId(originalEventId);
            } else {
                trigger.setOriginalEventId(UUID.randomUUID().toString());
            }

            JiraWebhookEvent saved = triggerHistoryRepository.save(trigger);
            log.info("Successfully recorded original trigger with ID: {}", saved.getId());
            return saved;
        } catch (Exception ex) {
            log.error("Unexpected error while recording trigger: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to record webhook trigger", ex);
        }
    }

    @Transactional
    public void updateTriggerResult(
            final UUID triggerId,
            final String userId,
            final TriggerStatus status,
            final String transformedPayload,
            final Integer responseStatusCode,
            final String responseBody,
            final String jiraIssueUrl) {
        log.info("Updating trigger result for ID: {} with status: {}", triggerId, status);
        try {
            Optional<JiraWebhookEvent> triggerOpt = triggerHistoryRepository.findById(triggerId);
            if (triggerOpt.isEmpty()) {
                log.error("Cannot update trigger result: Webhook trigger history not found with ID: {}",
                        triggerId);
                throw new IntegrationNotFoundException(
                        "Webhook trigger history not found with ID: " + triggerId);
            }

            JiraWebhookEvent trigger = triggerOpt.get();
            trigger.setStatus(status);
            trigger.setTransformedPayload(transformedPayload);
            trigger.setResponseStatusCode(responseStatusCode);
            trigger.setResponseBody(responseBody);
            trigger.setJiraIssueUrl(jiraIssueUrl);
            triggerHistoryRepository.save(trigger);
            log.info("Successfully updated trigger with ID: {} to status: {}", triggerId, status);
        } catch (IntegrationNotFoundException ex) {
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while updating trigger: {}", ex.getMessage());
            throw new IntegrationPersistenceException(
                    "Failed to update webhook trigger due to data integrity violation", ex);
        } catch (Exception ex) {
            log.error("Unexpected error while updating trigger {}: {}", triggerId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to update webhook trigger", ex);
        }
    }

    @Transactional
    public void update(final JiraWebhookEvent triggerHistory) {
        triggerHistoryRepository.save(triggerHistory);
    }

    public List<JiraWebhookEventResponse> getWebhookEventsByWebhookId(final String webhookId, final String tenantId) {
        return triggerHistoryRepository.findLatestEventsPerOriginalTriggerByWebhook(webhookId, tenantId)
                .stream().map(mapper::toResponse).toList();
    }

    public JiraWebhookEvent findByOriginalEventIdOrderByRetryAttempt(final String originalTriggerId) {
        return triggerHistoryRepository
                .findTopByOriginalEventIdOrderByRetryAttemptDesc(originalTriggerId)
                .orElseThrow(() -> new IntegrationPersistenceException(
                        "Webhook trigger history not found with original event ID: "
                                + originalTriggerId));
    }
}
