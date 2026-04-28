package com.integration.management.service;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.entity.Language;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.IntegrationPersistenceException;
import com.integration.management.exception.InvalidLanguageCodeException;
import com.integration.management.mapper.ConfluenceIntegrationMapper;
import com.integration.management.mapper.IntegrationJobExecutionMapper;
import com.integration.management.mapper.IntegrationSchedulerMapper;
import com.integration.management.model.dto.request.ConfluenceIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.response.ConfluenceIntegrationResponse;
import com.integration.management.model.dto.response.ConfluenceIntegrationSummaryResponse;
import com.integration.management.notification.aop.PublishNotification;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.ConfluenceIntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.integration.execution.contract.model.enums.TriggerType.API;
import static com.integration.execution.contract.model.enums.TriggerType.RETRY;
import static com.integration.management.constants.IntegrationManagementConstants.ROOT_FOLDER_KEY;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceIntegrationService {

    private final ConfluenceIntegrationRepository confluenceIntegrationRepository;
    private final MasterDataService masterDataService;
    private final CronScheduleService cronScheduleService;
    private final IntegrationSchedulerMapper integrationSchedulerMapper;
    private final ConfluenceScheduleService confluenceScheduleService;
    private final IntegrationJobExecutionMapper integrationJobExecutionMapper;
    private final IntegrationJobExecutionService integrationJobExecutionService;
    private final IntegrationConnectionService integrationConnectionService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final KwIntegrationService kwIntegrationService;
    private final ConfluenceLookupService confluenceLookupService;
    private final ConfluenceIntegrationMapper confluenceIntegrationMapper;
    private final MessagePublisher messagePublisher;

    @PublishNotification(
            eventKey = NotificationEventKey.CONFLUENCE_INTEGRATION_CREATED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadata = "{'integrationName': #result.name, 'integrationId': #result.id}")
    @Transactional
    public CreationResponse create(ConfluenceIntegrationCreateUpdateRequest request,
                                   String tenantId, String userId) {
        try {
            IntegrationSchedule schedule = integrationSchedulerMapper.toEntity(request.getSchedule());
            cronScheduleService.buildCron(schedule);
            ConfluenceIntegration integration = confluenceIntegrationMapper.toEntity(request);
            integration.setLanguages(resolveLanguages(request.getLanguageCodes()));
            integration.setSourceLanguage(resolveSourceLanguage(request.getSourceLanguage()));
            integration.setSchedule(schedule);
            integration.setTenantId(tenantId);
            integration.setCreatedBy(userId);
            integration.setLastModifiedBy(userId);
            ConfluenceIntegration saved = confluenceIntegrationRepository.save(integration);
            confluenceScheduleService.scheduleJob(saved);
            log.info("Scheduled job for created Confluence integration: {}", saved.getId());
            log.info("Successfully created Confluence integration with ID: {}", saved.getId());
            return confluenceIntegrationMapper.toCreationResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while creating Confluence integration: {}", ex.getMessage());
            if (isUniqueConstraintViolation(ex)) {
                throw new IntegrationNameAlreadyExistsException(
                        "Confluence integration name already exists. Please choose a different name.", ex);
            }
            throw new IntegrationPersistenceException(
                    "Failed to create Confluence integration due to data integrity violation", ex);
        } catch (IntegrationPersistenceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while creating Confluence integration: {}", ex.getMessage(), ex);
            throw new IntegrationPersistenceException("Failed to create Confluence integration", ex);
        }
    }

    @PublishNotification(
            eventKey = NotificationEventKey.CONFLUENCE_INTEGRATION_UPDATED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadata = "{'integrationName': #result.name, 'integrationId': #integrationId.toString()}")
    @Transactional
    public CreationResponse update(UUID integrationId, ConfluenceIntegrationCreateUpdateRequest request,
                                   String tenantId, String userId) {
        log.info("Updating Confluence integration: {} for tenant: {} by user: {}", integrationId, tenantId, userId);
        try {
            ConfluenceIntegration existing = findByIdAndTenant(integrationId, tenantId);
            String currentCronExpression = existing.getSchedule() != null
                    ? existing.getSchedule().getCronExpression() : null;

            confluenceIntegrationMapper.updateEntity(request, existing);
            existing.setLanguages(resolveLanguages(request.getLanguageCodes()));
            existing.setSourceLanguage(resolveSourceLanguage(request.getSourceLanguage()));
            existing.setLastModifiedBy(userId);
            integrationSchedulerMapper.updateEntity(request.getSchedule(), existing.getSchedule());
            cronScheduleService.buildCron(existing.getSchedule());
            confluenceIntegrationRepository.save(existing);
            if (!Objects.equals(currentCronExpression, existing.getSchedule().getCronExpression())) {
                confluenceScheduleService.updateSchedule(existing);
                log.info("Updated schedule for Confluence integration: {}", integrationId);
            }
            log.info("Successfully updated Confluence integration with ID: {}", integrationId);
            return confluenceIntegrationMapper.toCreationResponse(existing);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while updating Confluence integration: {}", ex.getMessage());
            if (isUniqueConstraintViolation(ex)) {
                throw new IntegrationNameAlreadyExistsException(
                        "Confluence integration name already exists. Please choose a different name.", ex);
            }
            throw new IntegrationPersistenceException(
                    "Failed to update Confluence integration due to data integrity violation", ex);
        } catch (IntegrationNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while updating Confluence integration: {}", ex.getMessage(), ex);
            throw new IntegrationPersistenceException("Failed to update Confluence integration", ex);
        }
    }

    @PublishNotification(
            eventKey = NotificationEventKey.CONFLUENCE_INTEGRATION_DELETED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadataProvider = "confluenceNotificationMetadataProvider",
            entityId = "#id")
    @Transactional
    public void delete(UUID id, String tenantId, String userId) {
        log.info("Deleting Confluence integration with ID: {} for tenant: {} by user: {}", id, tenantId, userId);
        try {
            ConfluenceIntegration existing = findByIdAndTenant(id, tenantId);
            if (existing.getSchedule() != null) {
                confluenceScheduleService.unscheduleJob(existing);
                log.info("Unscheduled job for deleted Confluence integration: {}", id);
            }
            existing.setIsEnabled(Boolean.FALSE);
            existing.setIsDeleted(Boolean.TRUE);
            existing.setLastModifiedBy(userId);
            confluenceIntegrationRepository.save(existing);
            log.info("Successfully deleted Confluence integration with ID: {}", id);
        } catch (Exception ex) {
            log.error("Unexpected error while deleting Confluence integration: {}", ex.getMessage(), ex);
            throw new IntegrationPersistenceException("Failed to delete Confluence integration", ex);
        }
    }

    @Transactional
    public boolean toggleActiveStatus(UUID id, String tenantId, String userId) {
        log.info("Toggling active status for Confluence integration: {} by user: {}", id, userId);
        try {
            ConfluenceIntegration integration = findByIdAndTenant(id, tenantId);
            boolean newStatus = !Boolean.TRUE.equals(integration.getIsEnabled());
            integration.setIsEnabled(newStatus);
            integration.setLastModifiedBy(userId);
            confluenceIntegrationRepository.save(integration);

            notificationEventPublisher.publishAfterCommit(NotificationEvent.builder()
                    .eventKey(newStatus
                            ? NotificationEventKey.CONFLUENCE_INTEGRATION_ENABLED.name()
                            : NotificationEventKey.CONFLUENCE_INTEGRATION_DISABLED.name())
                    .tenantId(tenantId)
                    .triggeredByUserId(userId)
                    .metadata(Map.of(
                            "integrationName", integration.getName() != null ? integration.getName() : "",
                            "integrationId", id.toString()))
                    .build());

            if (integration.getSchedule() != null) {
                if (newStatus) {
                    confluenceScheduleService.scheduleJob(integration);
                    log.info("Scheduled job for enabled Confluence integration: {}", id);
                } else {
                    confluenceScheduleService.unscheduleJob(integration);
                    log.info("Unscheduled job for disabled Confluence integration: {}", id);
                }
            }

            log.info("Successfully toggled Confluence integration {} status to: {}",
                    id, newStatus ? "enabled" : "disabled");
            return newStatus;
        } catch (DataAccessException e) {
            log.error("Failed to toggle Confluence integration status for integration: {}", id, e);
            throw new IntegrationPersistenceException("Failed to toggle Confluence integration status", e);
        }
    }

    @PublishNotification(
            eventKey         = NotificationEventKey.CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN,
            metadataProvider = "confluenceNotificationMetadataProvider",
            entityId         = "#integrationId")
    @Transactional
    public void triggerJobExecution(UUID integrationId, String tenantId, String userId) {
        log.info("Triggering manual job execution for Confluence integration: {} for tenant: {} by user: {}",
                integrationId, tenantId, userId);
        ConfluenceIntegration integration = findByIdAndTenant(integrationId, tenantId);
        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new IllegalStateException("Cannot trigger job for disabled integration: " + integrationId);
        }
        confluenceScheduleService.triggerJob(integration.getId(), tenantId, API, userId);
        log.info("Triggered Confluence job for integration: {} by user: {}", integrationId, userId);
    }

    public List<ConfluenceIntegrationSummaryResponse> getAllByTenant(String tenantId) {
        return confluenceIntegrationRepository
                .findAllSummariesByTenantId(tenantId)
                .stream()
                .map(confluenceIntegrationMapper::projectionToSummaryResponse)
                .peek(integration -> {
                    integration.setItemSubtypeLabel(
                            kwIntegrationService.getItemSubtypeDisplayValue(integration.getItemSubtype()));
                    integration.setDynamicDocumentTypeLabel(
                            kwIntegrationService.getDynamicDocumentTypeDisplayValue(
                                    integration.getItemType(),
                                    integration.getItemSubtype(),
                                    integration.getDynamicDocumentType()));
                    if (Boolean.TRUE.equals(integration.getIsEnabled())) {
                        integration.setNextRunAtUtc(cronScheduleService.getNextRun(
                                integration.getCronExpression(),
                                integration.getExecutionDate(),
                                integration.getExecutionTime()));
                    }
                }).collect(toList());
    }

    public ConfluenceIntegrationResponse getByIdAndTenantWithDetails(UUID id, String tenantId) {
        ConfluenceIntegration integration = findByIdAndTenant(id, tenantId);
        ConfluenceIntegrationResponse response = confluenceIntegrationMapper.toDetailsResponse(integration);
        response.setItemSubtypeLabel(
                kwIntegrationService.getItemSubtypeDisplayValue(response.getItemSubtype()));
        response.setDynamicDocumentTypeLabel(
                kwIntegrationService.getDynamicDocumentTypeDisplayValue(
                        integration.getDocumentItemType(),
                        response.getItemSubtype(),
                        response.getDynamicDocumentType()));
        response.setConfluenceSpaceLabel(getConfluenceSpaceLabel(
                integration.getConnectionId(),
                integration.getTenantId(),
                integration.getConfluenceSpaceKey()));
        response.setConfluenceSpaceFolderLabel(getConfluenceSpaceFolderLabel(
                integration.getConnectionId(),
                integration.getTenantId(),
                integration.getConfluenceSpaceKey(),
                integration.getConfluenceSpaceKeyFolderKey()));
        if (Boolean.TRUE.equals(integration.getIsEnabled())) {
            response.setNextRunAtUtc(cronScheduleService.getNextRun(
                    integration.getSchedule().getCronExpression(),
                    integration.getSchedule().getExecutionDate(),
                    integration.getSchedule().getExecutionTime()));
        }
        return response;
    }

    public List<IntegrationJobExecutionDto> getJobHistory(UUID integrationId, String tenantId) {
        return integrationJobExecutionService.getConfluenceJobHistory(integrationId, tenantId).stream()
                .map(integrationJobExecutionMapper::toDto)
                .collect(toList());
    }

    @Transactional
    public void retryJobExecution(UUID integrationId, UUID originalJobId, String tenantId, String userId) {
        ConfluenceIntegration integration = findByIdAndTenant(integrationId, tenantId);
        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new IllegalStateException("Cannot retry job for disabled integration: " + integrationId);
        }
        IntegrationJobExecution retryExecution = integrationJobExecutionService
                .createConfluenceRetryExecution(originalJobId, integrationId, tenantId, RETRY, userId);

        publishConfluenceExecutionCommand(
                integration,
                retryExecution,
                tenantId,
                userId,
                retryExecution.getWindowStart(),
                retryExecution.getWindowEnd());

        log.info("Published retry execution {} for source execution {} integration {}",
                retryExecution.getId(), originalJobId, integrationId);
    }

    public List<String> getAllConfluenceNormalizedNamesByTenantId(String tenantId) {
        return confluenceIntegrationRepository.findAllNormalizedNamesByTenantId(tenantId);
    }

    public String getConfluenceIntegrationNameById(String entityId) {
        return confluenceIntegrationRepository.findIntegrationNameById(UUID.fromString(entityId))
                .orElseThrow(() -> {
                    log.error("Confluence integration name not found for ID: {}", entityId);
                    return new IntegrationNotFoundException(
                            "Confluence integration name not found for ID: " + entityId);
                });
    }

    private ConfluenceIntegration findByIdAndTenant(UUID id, String tenantId) {
        return confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.error("Confluence integration not found with ID: {} for tenant: {}", id, tenantId);
                    return new IntegrationNotFoundException("Confluence integration not found with ID: " + id);
                });
    }

    public Optional<ConfluenceIntegration> getEnabledIntegrationForScheduledExecution(
            UUID integrationId, String tenantId) {
        return confluenceIntegrationRepository.findEnabledByIdAndTenantIdWithSchedule(integrationId, tenantId);
    }

    private List<Language> resolveLanguages(List<String> languageCodes) {
        List<Language> resolvedLanguages = masterDataService.getAllActiveLanguages().stream()
                .filter(lang -> languageCodes.contains(lang.getCode()))
                .collect(toList());
        if (languageCodes.size() != resolvedLanguages.size()) {
            throw new InvalidLanguageCodeException("One or more provided language codes are invalid: " + languageCodes);
        } else if (resolvedLanguages.isEmpty()) {
            throw new InvalidLanguageCodeException("At least one valid language code must be provided: "
                    + languageCodes);
        }
        return resolvedLanguages;
    }

    private String resolveSourceLanguage(String sourceLanguage) {
        if (sourceLanguage == null || sourceLanguage.isBlank()) {
            return "en";
        }
        return sourceLanguage.trim().toLowerCase();
    }

    public String getConfluenceSpaceLabel(UUID connectionId, String tenantId, String spaceKey) {
        List<ConfluenceSpaceDto> spaces = confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId);
        if (spaces == null) {
            return null;
        }
        return spaces.stream()
                .filter(space -> Objects.equals(space.getKey(), spaceKey))
                .findFirst()
                .map(space -> String.format("%s (%s)", space.getName(), space.getKey()))
                .orElse(null);
    }

    public String getConfluenceSpaceFolderLabel(UUID connectionId, String tenantId,
                                                String spaceKey, String folderKey) {
        if (folderKey.equals(ROOT_FOLDER_KEY)) {
            return ROOT_FOLDER_KEY;
        }
        List<ConfluencePageDto> pages = confluenceLookupService
                .getPagesByConnectionIdAndSpaceKey(connectionId, tenantId, spaceKey);
        if (pages == null) {
            return null;
        }

        return pages.stream()
                .filter(page -> Objects.equals(page.getId(), folderKey))
                .findFirst()
                .map(page -> page.getParentTitle() != null && !page.getParentTitle().isBlank()
                        ? page.getParentTitle() + " > " + page.getTitle()
                        : page.getTitle())
                .orElse(null);
    }

    private boolean isUniqueConstraintViolation(DataAccessException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("unique constraint")
                || message.contains("duplicate key")
                || message.contains("UNIQUE constraint"));
    }

    private void publishConfluenceExecutionCommand(
            ConfluenceIntegration integration,
            IntegrationJobExecution jobExecution,
            String tenantId,
            String triggeredByUser,
            Instant windowStart,
            Instant windowEnd) {

        String connectionSecretName = integrationConnectionService
                .getIntegrationConnectionNameById(integration.getConnectionId().toString(), tenantId);

        ConfluenceExecutionCommand command = confluenceIntegrationMapper.toExecutionCommand(
                integration,
                jobExecution,
                connectionSecretName,
                tenantId,
                TriggerType.RETRY,
                triggeredByUser,
                windowStart,
                windowEnd);

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                command);
    }
}
