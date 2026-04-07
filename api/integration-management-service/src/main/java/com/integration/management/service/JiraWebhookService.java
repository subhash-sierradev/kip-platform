package com.integration.management.service;

import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.config.properties.JiraWebhookProperties;
import com.integration.management.entity.JiraFieldMapping;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.IntegrationPersistenceException;
import com.integration.management.mapper.JiraFieldMappingMapper;
import com.integration.management.mapper.JiraWebhookMapper;
import com.integration.management.model.dto.request.JiraWebhookCreateUpdateRequest;
import com.integration.management.model.dto.response.JiraWebhookDetailResponse;
import com.integration.management.model.dto.response.JiraWebhookEventSummaryResponse;
import com.integration.management.model.dto.response.JiraWebhookSummaryResponse;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import com.integration.management.notification.aop.PublishNotification;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraWebhookService {

    private final JiraWebhookProperties jiraWebhookProperties;
    private final JiraWebhookRepository jiraWebhookRepository;
    private final JiraWebhookMapper jiraWebhookMapper;
    private final JiraFieldMappingMapper jiraFieldMappingMapper;
    private final JiraWebhookEventRepository jiraWebhookEventRepository; // added
    private final NotificationEventPublisher notificationEventPublisher;

    @PublishNotification(
            eventKey = NotificationEventKey.JIRAWEBHOOK_INTEGRATION_CREATED,
            tenantId = "#tenantId",
            userId   = "#userId",
            metadata = "{'webhookName': #result.name, 'webhookId': #result.id}")
    @Transactional
    public JiraWebhook create(JiraWebhookCreateUpdateRequest request, String tenantId, String userId) {
        log.info("Creating Jira webhook: {} for tenant: {} by user: {}", request.getName(), tenantId, userId);
        try {
            JiraWebhook webhook = jiraWebhookMapper.toEntity(request);
            webhook.setTenantId(tenantId);
            webhook.setCreatedBy(userId);
            webhook.setLastModifiedBy(userId);

            // Process field mappings
            if (request.getFieldsMapping() != null) {
                // Convert DTOs to entities using dedicated mapper
                List<JiraFieldMapping> fieldMappings = jiraFieldMappingMapper.toEntity(request.getFieldsMapping());

                // Establish bidirectional relationship
                fieldMappings.forEach(mapping -> mapping.setJiraWebhook(webhook));
                webhook.setJiraFieldMappings(fieldMappings);
            }

            int attempts = 0;
            while (attempts++ < jiraWebhookProperties.getMaxRetries()) {
                String webhookId = IdGenerator.randomBase62(jiraWebhookProperties.getIdLength());
                webhook.setId(webhookId);
                webhook.setWebhookUrl(jiraWebhookProperties.getUrlTemplate().replace("{webhookId}", webhookId));
                try {
                    JiraWebhook saved = jiraWebhookRepository.save(webhook);
                    log.info("Successfully created Jira webhook with ID: {} and {} field mappings",
                            saved.getId(), saved.getJiraFieldMappings() != null
                                    ? saved.getJiraFieldMappings().size()
                                    : 0);
                    return saved;
                } catch (DataAccessException ex) {
                    log.error("Attempt {}: Failed to create Jira webhook due to data access exception: {}",
                            attempts, ex.getMessage());
                    if (isDuplicateKeyException(ex)) {
                        continue;
                    }
                    throw ex;
                }
            }
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while creating Jira webhook: {}", ex.getMessage());
            if (isUniqueConstraintViolation(ex)) {
                throw new IntegrationNameAlreadyExistsException(
                        "Jira webhook name already exists. Please choose a different name.", ex);
            }
            throw new IntegrationPersistenceException("Failed to create Jira webhook due to data integrity violation",
                    ex);
        } catch (Exception ex) {
            log.error("Unexpected error while creating Jira webhook: {}", ex.getMessage());
            throw new RuntimeException("Failed to create Jira webhook", ex);
        }
        return null;
    }

    private boolean isDuplicateKeyException(DataAccessException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate key")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("unique constraint")
                        || lowerMessage.contains("duplicate key")
                        || lowerMessage.contains("duplicate entry")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    @PublishNotification(
            eventKey = NotificationEventKey.JIRAWEBHOOK_INTEGRATION_UPDATED,
            tenantId = "#tenantId",
            userId   = "#userId",
            metadata = "{'webhookName': #request.name, 'webhookId': #id}")
    @Transactional
    public void update(String id, JiraWebhookCreateUpdateRequest request, String tenantId, String userId) {

        JiraWebhook existingWebhook = findByIdAndTenant(id, tenantId);

        // Update scalar fields (name, description, connectionId, etc.)
        jiraWebhookMapper.updateEntity(existingWebhook, request);

        // Update field mappings using orphan removal pattern
        if (request.getFieldsMapping() != null) {
            List<JiraFieldMapping> existingMappings = existingWebhook.getJiraFieldMappings();
            List<JiraFieldMapping> incomingMappings = jiraFieldMappingMapper
                    .toEntity(request.getFieldsMapping())
                    .stream()
                    .peek(m -> m.setJiraWebhook(existingWebhook))
                    .toList();

            boolean mappingsChanged = !snapshot(existingMappings).equals(snapshot(incomingMappings));
            log.info("Field mappings changed: {}", mappingsChanged);

            if (mappingsChanged) {
                // Clear existing mappings (orphanRemoval will delete them)
                existingMappings.clear();
                // Add new mappings
                existingMappings.addAll(incomingMappings);
                // Touch parent to increment version
                existingWebhook.touch();
                log.info("Updated {} field mappings for webhook: {}", incomingMappings.size(), id);
            }
        }

        existingWebhook.setWebhookUrl(jiraWebhookProperties.getUrlTemplate()
                .replace("{webhookId}", existingWebhook.getId()));
        existingWebhook.setLastModifiedBy(userId);
        jiraWebhookRepository.save(existingWebhook);
        log.info("Successfully updated Jira webhook: {}", id);
    }

    @PublishNotification(
            eventKey         = NotificationEventKey.JIRAWEBHOOK_INTEGRATION_DELETED,
            tenantId         = "#tenantId",
            userId           = "#userId",
            metadataProvider = "jiraWebhookNotificationMetadataProvider",
            entityId         = "#id")
    @Transactional
    public void delete(String id, String tenantId, String userId) {
        log.info("Deleting Jira webhook: {} for tenant: {} by user: {}", id, tenantId, userId);
        JiraWebhook webhook = findByIdAndTenant(id, tenantId);
        webhook.setIsDeleted(Boolean.TRUE);
        webhook.setIsEnabled(Boolean.FALSE);
        webhook.setLastModifiedBy(userId);
        jiraWebhookRepository.save(webhook);
        log.info("Successfully deleted Jira webhook with ID: {}", id);
    }

    public JiraWebhookDetailResponse getById(String id, String tenantId) {
        log.info("Fetching Jira webhook: {} for tenant: {}", id, tenantId);
        JiraWebhook webhook = findByIdAndTenant(id, tenantId);
        JiraWebhookDetailResponse response = jiraWebhookMapper.toResponse(webhook);
        if (webhook.getJiraFieldMappings() != null && !webhook.getJiraFieldMappings().isEmpty()) {
            response.setJiraFieldMappings(
                    webhook.getJiraFieldMappings().stream()
                            .map(jiraFieldMappingMapper::toDto)
                            .collect(java.util.stream.Collectors.toList()));
        } else {
            response.setJiraFieldMappings(java.util.Collections.emptyList());
        }
        return response;
    }

    public List<JiraWebhookSummaryResponse> getAllByTenantId(String tenantId) {
        log.info("Fetching all Jira webhooks summary for tenant: {}", tenantId);
        List<JiraWebhook> webhooks = jiraWebhookRepository.findAllSummaryByTenantId(tenantId);
        if (webhooks.isEmpty()) {
            return List.of();
        }
        // Batch load latest events per webhook in one query
        List<String> ids = webhooks.stream().map(JiraWebhook::getId).toList();
        var latestEvents = jiraWebhookEventRepository.findLatestEventsForWebhookIds(ids).stream()
                .collect(Collectors.toMap(JiraWebhookEvent::getWebhookId, ev -> ev));
        return webhooks.stream()
                .map(w -> mapToWebhookSummaryResponse(w, latestEvents.get(w.getId())))
                .collect(Collectors.toList());
    }

    // Overloaded mapper that accepts a pre-fetched latest event
    private JiraWebhookSummaryResponse mapToWebhookSummaryResponse(JiraWebhook webhook, JiraWebhookEvent latestEvent) {
        JiraWebhookSummaryResponse.JiraWebhookSummaryResponseBuilder builder = JiraWebhookSummaryResponse.builder()
                .id(webhook.getId())
                .name(webhook.getName())
                .webhookUrl(webhook.getWebhookUrl())
                .jiraFieldMappings(webhook.getJiraFieldMappings() != null
                        ? webhook.getJiraFieldMappings().stream()
                        .map(jiraFieldMappingMapper::toDto)
                        .collect(Collectors.toList())
                        : null)
                .isEnabled(webhook.getIsEnabled())
                .isDeleted(webhook.getIsDeleted())
                .createdBy(webhook.getCreatedBy())
                .createdDate(webhook.getCreatedDate());
        if (latestEvent != null) {
            builder.lastEventHistory(JiraWebhookEventSummaryResponse.builder()
                    .id(latestEvent.getId().toString())
                    .triggeredBy(latestEvent.getTriggeredBy())
                    .triggeredAt(latestEvent.getTriggeredAt())
                    .status(latestEvent.getStatus() != null ? latestEvent.getStatus().name() : null)
                    .jiraIssueUrl(latestEvent.getJiraIssueUrl())
                    .build());
        }
        return builder.build();
    }

    public List<JiraWebhookDetailResponse> getByConnectionId(UUID connectionId) {
        log.info("Fetching Jira webhooks for connection: {}", connectionId);
        List<JiraWebhook> webhooks = jiraWebhookRepository.findByConnectionIdAndIsDeletedFalse(connectionId);
        return webhooks.stream()
                .map(jiraWebhookMapper::toResponse)
                .collect(Collectors.toList());
    }

    private JiraWebhook findByIdAndTenant(String id, String tenantId) {
        Optional<JiraWebhook> webhookOpt = jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId);
        if (webhookOpt.isEmpty()) {
            throw new IntegrationNotFoundException("Jira webhook not found with ID: " + id + " for tenant: "
                    + tenantId);
        }
        log.info("Found Jira webhook with ID: {} for tenant: {}", id, webhookOpt.get().getJiraFieldMappings());
        return webhookOpt.get();
    }

    @Transactional
    public boolean toggleActiveStatus(String id, String tenantId, String userId) {
        log.info("Toggling active status for webhook: {} by user: {}", id, userId);
        try {
            Optional<JiraWebhook> optionalWebhook = jiraWebhookRepository
                    .findByIdAndTenantIdAndIsDeletedFalse(id, tenantId);
            if (optionalWebhook.isEmpty()) {
                throw new IntegrationNotFoundException("Webhook not found with ID: " + id);
            }

            JiraWebhook webhook = optionalWebhook.get();
            boolean newStatus = !Boolean.TRUE.equals(webhook.getIsEnabled());
            webhook.setIsEnabled(newStatus);
            webhook.setLastModifiedBy(userId);
            jiraWebhookRepository.save(webhook);
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(newStatus
                            ? NotificationEventKey.JIRAWEBHOOK_INTEGRATION_ENABLED.name()
                            : NotificationEventKey.JIRAWEBHOOK_INTEGRATION_DISABLED.name())
                    .tenantId(tenantId)
                    .triggeredByUserId(userId)
                    .metadata(java.util.Map.of(
                            "webhookName", webhook.getName() != null ? webhook.getName() : "",
                            "webhookId", webhook.getId() != null ? webhook.getId() : ""))
                    .build());
            log.info("Successfully toggled webhook {} status to: {}", id, newStatus ? "enabled" : "disabled");
            return newStatus; // Return the new status
        } catch (DataAccessException e) {
            log.error("Failed to toggle webhook status for webhook: {}", id, e);
            throw new IntegrationPersistenceException("Failed to toggle webhook status", e);
        }
    }

    public List<String> getAllNormalizedNamesByTenantId(String tenantId) {
        log.debug("Fetching Jira webhook normalized names from database for tenant: {}", tenantId);
        return jiraWebhookRepository.findAllNormalizedNamesByTenantId(tenantId);
    }

    @Cacheable(value = "jiraWebhookNamesCache", key = "#id")
    public String getJiraWebhookNameById(String id) {
        log.info("Fetching Jira webhook name for id: {}", id);
        try {
            return jiraWebhookRepository.findJiraWebhookNameById(id);
        } catch (Exception e) {
            log.warn("Unable to fetch Jira webhook name for id: {}, error: {}", id, e.getMessage());
            return null;
        }
    }

    private Set<FieldMappingSnapshot> snapshot(List<JiraFieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return Set.of();
        }

        return mappings.stream()
                .map(m -> new FieldMappingSnapshot(
                        normalize(m.getJiraFieldId()),
                        normalize(m.getJiraFieldName()),
                        normalize(m.getDisplayLabel()),
                        m.getDataType(),
                        normalize(m.getTemplate()),
                        Boolean.TRUE.equals(m.getRequired()),
                        normalize(m.getDefaultValue())
                ))
                .collect(Collectors.toSet()); // order-independent
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public record FieldMappingSnapshot(
            String jiraFieldId,
            String jiraFieldName,
            String displayLabel,
            JiraDataType dataType,
            String template,
            boolean required,
            String defaultValue
    ) {
    }
}
