package com.integration.management.service;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import com.integration.management.constants.KwConstants;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationFieldMapping;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.IntegrationPersistenceException;
import com.integration.management.ies.client.IesArcGISApiClient;
import com.integration.management.mapper.ArcGISIntegrationMapper;
import com.integration.management.mapper.IntegrationFieldMappingMapper;
import com.integration.management.mapper.IntegrationJobExecutionMapper;
import com.integration.management.mapper.IntegrationSchedulerMapper;
import com.integration.management.model.dto.request.ArcGISIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.management.model.dto.response.ArcGISIntegrationResponse;
import com.integration.management.model.dto.response.ArcGISIntegrationSummaryResponse;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.notification.aop.PublishNotification;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.IntegrationFieldMappingRepository;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.integration.execution.contract.model.enums.TriggerType.API;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArcGISIntegrationService {

    private final ArcGISIntegrationRepository arcGISIntegrationRepository;
    private final ArcGISIntegrationMapper arcGISIntegrationMapper;
    private final CronScheduleService cronScheduleService;
    private final IntegrationSchedulerMapper integrationSchedulerMapper;
    private final IntegrationFieldMappingMapper integrationFieldMappingMapper;
    private final IntegrationFieldMappingRepository integrationFieldMappingRepository;
    private final ArcGISScheduleService arcGISScheduleService;
    private final IntegrationJobExecutionRepository integrationJobExecutionRepository;
    private final IntegrationJobExecutionMapper integrationJobExecutionMapper;
    private final KwIntegrationService kwIntegrationService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final IesArcGISApiClient iesArcGISApiClient;
    private final IntegrationConnectionService integrationConnectionService;

    @PublishNotification(
            eventKey = NotificationEventKey.ARCGIS_INTEGRATION_CREATED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadata = "{'integrationName': #result.name, 'integrationId': #result.id}")
    @Transactional
    public CreationResponse create(ArcGISIntegrationCreateUpdateRequest request, String tenantId, String userId) {
        try {
            IntegrationScheduleRequest scheduleRequest = request.getSchedule();
            IntegrationSchedule schedule = integrationSchedulerMapper.toEntity(scheduleRequest);
            cronScheduleService.buildCron(schedule);

            List<IntegrationFieldMapping> mappings = integrationFieldMappingMapper
                    .toEntities(request.getFieldMappings());

            ArcGISIntegration integration = arcGISIntegrationMapper.toEntity(request);

            integration.setSchedule(schedule);
            integration.setTenantId(tenantId);
            integration.setCreatedBy(userId);
            integration.setLastModifiedBy(userId);

            // First save the integration to get its ID
            ArcGISIntegration saved = arcGISIntegrationRepository.save(integration);

            if (!mappings.isEmpty()) {
                mappings.forEach(mapping -> {
                    mapping.setId(null); // remove existing id for clone case
                    mapping.setIntegrationId(saved.getId()); // set integrationId
                });
                integrationFieldMappingRepository.saveAll(mappings);
            }

            arcGISScheduleService.scheduleJob(saved);
            log.info("Scheduled job for created ArcGIS integration: {}", saved.getId());

            log.info("Successfully created ArcGIS integration with ID: {}", saved.getId());
            return arcGISIntegrationMapper.toCreationResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while creating ArcGIS integration: {}",
                    ex.getMessage());
            if (isUniqueConstraintViolation(ex)) {
                throw new IntegrationNameAlreadyExistsException(
                        "ArcGIS integration name already exists. Please choose a different name.", ex);
            }
            throw new IntegrationPersistenceException(
                    "Failed to create ArcGIS integration due to data integrity violation", ex);
        } catch (Exception ex) {
            log.error("Unexpected error while creating ArcGIS integration: {}",
                    ex.getMessage(), ex);
            throw new IntegrationPersistenceException(
                    "Failed to create ArcGIS integration", ex);
        }
    }

    @PublishNotification(
            eventKey = NotificationEventKey.ARCGIS_INTEGRATION_UPDATED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadata = "{'integrationName': #result.name, 'integrationId': #integrationId.toString()}")
    @Transactional
    public CreationResponse update(UUID integrationId, ArcGISIntegrationCreateUpdateRequest request,
            String tenantId, String userId) {
        log.info("Updating ArcGIS integration: {} (ID: {}) for tenant: {} by user: {}",
                request.getName(), integrationId, tenantId, userId);
        try {
            ArcGISIntegration existing = findByIdAndTenant(integrationId, tenantId);
            String currentCronExpression = existing.getSchedule().getCronExpression();
            arcGISIntegrationMapper.updateEntity(request, existing);
            integrationSchedulerMapper.updateEntity(request.getSchedule(), existing.getSchedule());
            cronScheduleService.buildCron(existing.getSchedule());

            // Clear dynamicDocumentType if itemSubtype changed or is non-dynamic
            if (!KwConstants.DOCUMENT_FINAL_DYNAMIC.equals(existing.getItemSubtype())) {
                existing.setDynamicDocumentType(null);
            }

            syncIntegrationFieldMappings(existing, request);

            existing.setLastModifiedBy(userId);
            arcGISIntegrationRepository.save(existing);

            // Update schedule if cron expression has changed
            if (!currentCronExpression.equals(existing.getSchedule().getCronExpression())) {
                arcGISScheduleService.updateSchedule(existing);
                log.info("Updated schedule for ArcGIS integration: {}", integrationId);
            }

            log.info("Successfully updated ArcGIS integration with ID: {}", integrationId);
            return arcGISIntegrationMapper.toCreationResponse(existing);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while updating ArcGIS integration: {}",
                    ex.getMessage());
            if (isUniqueConstraintViolation(ex)) {
                throw new IntegrationNameAlreadyExistsException(
                        "ArcGIS integration name already exists. Please choose a different name.", ex);
            }
            throw new IntegrationPersistenceException(
                    "Failed to update ArcGIS integration due to data integrity violation", ex);
        } catch (IntegrationNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while updating ArcGIS integration: {}",
                    ex.getMessage(), ex);
            throw new IntegrationPersistenceException(
                    "Failed to update ArcGIS integration", ex);
        }
    }

    public List<ArcGISIntegrationSummaryResponse> getAllByTenant(String tenantId) {
        return arcGISIntegrationRepository.findAllSummariesByTenantId(tenantId)
                .stream()
                .map(arcGISIntegrationMapper::projectionToResponse)
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
                })
                .toList();
    }

    public ArcGISIntegrationResponse getByIdAndTenantWithDetails(UUID id, String tenantId) {
        log.info("Fetching ArcGIS integration with ID: {} for tenant: {}", id, tenantId);
        ArcGISIntegration integration = findByIdAndTenant(id, tenantId);
        ArcGISIntegrationResponse arcGISIntegrationResponse = arcGISIntegrationMapper
                .toDetailsResponse(integration);
        arcGISIntegrationResponse.setItemSubtypeLabel(
                kwIntegrationService.getItemSubtypeDisplayValue(
                        arcGISIntegrationResponse.getItemSubtype()));
        arcGISIntegrationResponse.setDynamicDocumentTypeLabel(
                kwIntegrationService.getDynamicDocumentTypeDisplayValue(
                        integration.getItemType(),
                        arcGISIntegrationResponse.getItemSubtype(),
                        arcGISIntegrationResponse.getDynamicDocumentType()));
        // Compute next run time for enabled integrations only
        if (Boolean.TRUE.equals(integration.getIsEnabled())) {
            arcGISIntegrationResponse.setNextRunAtUtc(cronScheduleService.getNextRun(
                    integration.getSchedule().getCronExpression(),
                    arcGISIntegrationResponse.getSchedule().getExecutionDate(),
                    arcGISIntegrationResponse.getSchedule().getExecutionTime()));
        }
        return arcGISIntegrationResponse;
    }

    private ArcGISIntegration findByIdAndTenant(UUID id, String tenantId) {
        return arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.error("ArcGIS integration not found with ID: {} for tenant: {}", id, tenantId);
                    return new IntegrationNotFoundException("ArcGIS integration not found with ID: " + id);
                });
    }

    @PublishNotification(
            eventKey = NotificationEventKey.ARCGIS_INTEGRATION_DELETED,
            tenantId = "#tenantId",
            userId = "#userId",
            metadataProvider = "arcGISNotificationMetadataProvider",
            entityId = "#id")
    @Transactional
    public void delete(UUID id, String tenantId, String userId) {
        log.info("Deleting ArcGIS integration with ID: {} for tenant: {} by user: {}",
                id, tenantId, userId);
        try {
            ArcGISIntegration existing = findByIdAndTenant(id, tenantId);
            if (existing.getSchedule() != null) {
                arcGISScheduleService.unscheduleJob(existing);
            }
            existing.setIsDeleted(Boolean.TRUE);
            existing.setLastModifiedBy(userId);
            arcGISIntegrationRepository.save(existing);
            log.info("Successfully deleted ArcGIS integration with ID: {}", id);
        } catch (Exception ex) {
            log.error("Unexpected error while deleting ArcGIS integration: {}", ex.getMessage(), ex);
            throw new IntegrationPersistenceException("Failed to deleteSecret ArcGIS integration", ex);
        }
    }

    @Transactional
    public boolean toggleActiveStatus(UUID id, String tenantId, String userId) {
        log.info("Toggling active status for ArcGIS integration: {} by user: {}", id, userId);
        try {
            ArcGISIntegration integration = findByIdAndTenant(id, tenantId);
            boolean newStatus = !Boolean.TRUE.equals(integration.getIsEnabled());
            integration.setIsEnabled(newStatus);
            integration.setLastModifiedBy(userId);
            arcGISIntegrationRepository.save(integration);

            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(newStatus
                            ? NotificationEventKey.ARCGIS_INTEGRATION_ENABLED.name()
                            : NotificationEventKey.ARCGIS_INTEGRATION_DISABLED.name())
                    .tenantId(tenantId)
                    .triggeredByUserId(userId)
                    .metadata(java.util.Map.of(
                            "integrationName", integration.getName() != null ? integration.getName() : "",
                            "integrationId", id.toString()))
                    .build());

            if (integration.getSchedule() != null) {
                if (newStatus) {
                    arcGISScheduleService.scheduleJob(integration);
                    log.info("Scheduled job for enabled ArcGIS integration: {}", id);
                } else {
                    arcGISScheduleService.unscheduleJob(integration);
                    log.info("Unscheduled job for disabled ArcGIS integration: {}", id);
                }
            }

            log.info("Successfully toggled ArcGIS integration {} status to: {}",
                    id, newStatus ? "enabled" : "disabled");
            return newStatus; // Return the new status
        } catch (DataAccessException e) {
            log.error("Failed to toggle ArcGIS integration status for integration: {}", id, e);
            throw new IntegrationPersistenceException("Failed to toggle ArcGIS integration status", e);
        }
    }

    public List<IntegrationJobExecutionDto> getJobHistory(UUID integrationId, String tenantId) {
        return integrationJobExecutionRepository
                .findByIntegrationAndTenant(
                        integrationId,
                        tenantId,
                        Sort.by(Sort.Direction.DESC, "startedAt"))
                .stream()
                .map(integrationJobExecutionMapper::toDto)
                .toList();
    }

    public List<IntegrationFieldMappingDto> getFieldMappings(UUID integrationId, String tenantId) {
        return integrationFieldMappingRepository
                .findByIntegrationIdAndTenantId(integrationId, tenantId).stream()
                .map(integrationFieldMappingMapper::toDto).toList();
    }

    private boolean isUniqueConstraintViolation(DataAccessException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("unique constraint")
                || message.contains("duplicate key")
                || message.contains("UNIQUE constraint"));
    }

    @PublishNotification(
            eventKey         = NotificationEventKey.ARCGIS_INTEGRATION_JOB_ADHOC_RUN,
            metadataProvider = "arcGISNotificationMetadataProvider",
            entityId         = "#integrationId")
    @Transactional
    public void triggerJobExecution(UUID integrationId, String tenantId, String userId) {
        log.info("Triggering manual job execution for ArcGIS integration: {} for tenant: {} by user: {}",
                integrationId, tenantId, userId);

        ArcGISIntegration integration = findByIdAndTenant(integrationId, tenantId);

        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new IllegalStateException("Cannot trigger job for disabled integration: " + integrationId);
        }

        arcGISScheduleService.triggerJob(integration.getId(), tenantId, API, userId);
        log.info("Triggered ArcGIS job for integration: {} by user: {}", integrationId, userId);
    }

    private void syncIntegrationFieldMappings(ArcGISIntegration existing,
            ArcGISIntegrationCreateUpdateRequest request) {
        List<IntegrationFieldMappingDto> requestMappings = request.getFieldMappings();
        if (requestMappings == null || requestMappings.isEmpty()) {
            return;
        }

        List<IntegrationFieldMapping> mappings = integrationFieldMappingMapper.toEntities(requestMappings);

        mappings.forEach(m -> m.setIntegrationId(existing.getId()));

        Set<UUID> incomingIds = integrationFieldMappingRepository
                .saveAll(mappings)
                .stream()
                .map(IntegrationFieldMapping::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        integrationFieldMappingRepository
                .deleteByIntegrationIdAndIdNotIn(existing.getId(), incomingIds);
    }

    public String getArcGISIntegrationNameById(String entityId) {
        return arcGISIntegrationRepository.findIntegrationNameById(UUID.fromString(entityId))
                .orElseThrow(() -> {
                    log.error("ArcGIS integration name not found for ID: {}", entityId);
                    return new IntegrationNotFoundException(
                            "ArcGIS integration name not found for ID: " + entityId);
                });
    }

    public List<String> getAllArcGISNormalizedNamesByTenantId(String tenantId) {
        log.debug("Fetching ArcGIS normalized names from database for tenant: {}", tenantId);
        return arcGISIntegrationRepository.findAllNormalizedNamesByTenantId(tenantId);
    }

    @Cacheable(value = "arcgisFeaturesCache", key = "#tenantId")
    public List<ArcGISFieldDto> fetchArcGISFields(final UUID connectionId, final String tenantId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesArcGISApiClient.fetchArcGISFields(secretName);
    }
}
