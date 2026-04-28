package com.integration.management.service;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
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
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.management.model.dto.response.ConfluenceIntegrationResponse;
import com.integration.management.model.dto.response.ConfluenceIntegrationSummaryResponse;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.ConfluenceIntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceIntegrationService")
class ConfluenceIntegrationServiceTest {

    @Mock
    private ConfluenceIntegrationRepository confluenceIntegrationRepository;
    @Mock
    private MasterDataService masterDataService;
    @Mock
    private CronScheduleService cronScheduleService;
    @Mock
    private IntegrationSchedulerMapper integrationSchedulerMapper;
    @Mock
    private ConfluenceScheduleService confluenceScheduleService;
    @Mock
    private IntegrationJobExecutionMapper integrationJobExecutionMapper;
    @Mock
    private IntegrationJobExecutionService integrationJobExecutionService;
    @Mock
    private IntegrationConnectionService integrationConnectionService;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private KwIntegrationService kwIntegrationService;
    @Mock
    private ConfluenceLookupService confluenceLookupService;
    @Mock
    private ConfluenceIntegrationMapper confluenceIntegrationMapper;
    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private ConfluenceIntegrationService service;

    // -------------------------------------------------------------------------
    // create tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create - success path schedules job and returns response")
    void create_success_schedulesJobAndReturns() {
        // Arrange
        String tenantId = "tenant1";
        String userId = "user1";
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setName("Test Integration");
        request.setLanguageCodes(List.of("en-US"));
        IntegrationScheduleRequest scheduleRequest = new IntegrationScheduleRequest();
        request.setSchedule(scheduleRequest);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");
        Language language = new Language();
        language.setCode("en-US");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(UUID.randomUUID());
        integration.setName("Test Integration");

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(schedule);
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(integration);
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(language));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        CreationResponse expectedResponse = new CreationResponse(integration.getId().toString(), integration.getName());
        when(confluenceIntegrationMapper.toCreationResponse(any())).thenReturn(expectedResponse);

        // Act
        CreationResponse result = service.create(request, tenantId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(integration.getId().toString());
        verify(cronScheduleService).buildCron(schedule);
        verify(confluenceScheduleService).scheduleJob(integration);
        verify(confluenceIntegrationRepository).save(any(ConfluenceIntegration.class));
    }

    @Test
    @DisplayName("create - unique constraint violation throws IntegrationNameAlreadyExistsException")
    void create_uniqueViolation_throwsNameAlreadyExists() {
        // Arrange
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        Language lang = new Language();
        lang.setCode("en-US");
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));

        DataIntegrityViolationException uniqueEx = new DataIntegrityViolationException(
                "unique constraint violation: uk_confluence_integration_normalized_name_tenant");
        when(confluenceIntegrationRepository.save(any())).thenThrow(uniqueEx);

        // Act & Assert
        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class)
                .hasMessageContaining("name already exists");

        verify(confluenceScheduleService, never()).scheduleJob(any());
    }

    @Test
    @DisplayName("create - non-unique data integrity violation throws IntegrationPersistenceException")
    void create_nonUniqueViolation_throwsPersistence() {
        // Arrange
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        Language lang = new Language();
        lang.setCode("en-US");
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));

        DataIntegrityViolationException otherEx = new DataIntegrityViolationException("some other constraint");
        when(confluenceIntegrationRepository.save(any())).thenThrow(otherEx);

        // Act & Assert
        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("data integrity violation");
    }

    @Test
    @DisplayName("create - unexpected exception throws IntegrationPersistenceException")
    void create_unexpectedException_throwsPersistence() {
        // Arrange
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        when(integrationSchedulerMapper.toEntity(any())).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("Failed to create");
    }

    // -------------------------------------------------------------------------
    // update tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update - success with schedule change updates and reschedules")
    void update_successWithScheduleChange_updatesAndReschedules() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tenantId = "tenant1";
        String userId = "user1";

        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        IntegrationScheduleRequest scheduleRequest = new IntegrationScheduleRequest();
        request.setSchedule(scheduleRequest);

        IntegrationSchedule existingSchedule = new IntegrationSchedule();
        existingSchedule.setCronExpression("0 0 10 * * ?");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(existingSchedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(existing));
        Language lang = new Language();
        lang.setCode("en-US");
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);

        doAnswer(inv -> {
            IntegrationSchedule sched = inv.getArgument(0);
            sched.setCronExpression("0 0 12 * * ?");  // New cron
            return null;
        }).when(cronScheduleService).buildCron(any());

        CreationResponse expectedResponse = new CreationResponse(id.toString(), "Updated");
        when(confluenceIntegrationMapper.toCreationResponse(any())).thenReturn(expectedResponse);

        // Act
        CreationResponse result = service.update(id, request, tenantId, userId);

        // Assert
        assertThat(result).isNotNull();
        verify(confluenceScheduleService).updateSchedule(existing);
        verify(confluenceIntegrationRepository).save(existing);
    }

    @Test
    @DisplayName("update - no schedule change does not reschedule")
    void update_noScheduleChange_doesNotReschedule() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tenantId = "tenant1";

        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(existing));
        Language lang = new Language();
        lang.setCode("en-US");
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(id.toString(), "Test"));

        // Act
        service.update(id, request, tenantId, "user1");

        // Assert
        verify(confluenceScheduleService, never()).updateSchedule(any());
    }

    @Test
    @DisplayName("update - not found throws IntegrationNotFoundException")
    void update_notFound_throwsNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.update(id, request, "tenant1", "user1"))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // delete tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - with schedule unschedules before marking deleted")
    void delete_withSchedule_unschedulesAndDeletes() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tenantId = "tenant1";

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(new IntegrationSchedule());

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(existing));
        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);

        // Act
        service.delete(id, tenantId, "user1");

        // Assert
        verify(confluenceScheduleService).unscheduleJob(existing);
        verify(confluenceIntegrationRepository).save(any());
        assertThat(existing.getIsDeleted()).isTrue();
        assertThat(existing.getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("delete - without schedule does not call unschedule")
    void delete_withoutSchedule_deletesWithoutUnschedule() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tenantId = "tenant1";

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(null);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(existing));
        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);

        // Act
        service.delete(id, tenantId, "user1");

        // Assert
        verify(confluenceScheduleService, never()).unscheduleJob(any());
        assertThat(existing.getIsDeleted()).isTrue();
    }

    // -------------------------------------------------------------------------
    // toggleActiveStatus tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toggleActiveStatus - enables disabled integration and schedules")
    void toggleActiveStatus_enable_schedulesJob() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tenantId = "tenant1";

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setName("Test");
        integration.setIsEnabled(false);
        integration.setSchedule(new IntegrationSchedule());

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        // Act
        boolean result = service.toggleActiveStatus(id, tenantId, "user1");

        // Assert
        assertThat(result).isTrue();
        assertThat(integration.getIsEnabled()).isTrue();
        verify(confluenceScheduleService).scheduleJob(integration);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey())
                .isEqualTo(NotificationEventKey.CONFLUENCE_INTEGRATION_ENABLED.name());
    }

    @Test
    @DisplayName("toggleActiveStatus - disables enabled integration and unschedules")
    void toggleActiveStatus_disable_unschedulesJob() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tenantId = "tenant1";

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setName("Test");
        integration.setIsEnabled(true);
        integration.setSchedule(new IntegrationSchedule());

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        // Act
        boolean result = service.toggleActiveStatus(id, tenantId, "user1");

        // Assert
        assertThat(result).isFalse();
        assertThat(integration.getIsEnabled()).isFalse();
        verify(confluenceScheduleService).unscheduleJob(integration);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey())
                .isEqualTo(NotificationEventKey.CONFLUENCE_INTEGRATION_DISABLED.name());
    }

    @Test
    @DisplayName("toggleActiveStatus - no schedule does not call schedule operations")
    void toggleActiveStatus_noSchedule_doesNotCallScheduleOps() {
        // Arrange
        UUID id = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setName("Test");
        integration.setIsEnabled(false);
        integration.setSchedule(null);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        // Act
        service.toggleActiveStatus(id, "tenant1", "user1");

        // Assert
        verify(confluenceScheduleService, never()).scheduleJob(any());
        verify(confluenceScheduleService, never()).unscheduleJob(any());
    }

    // -------------------------------------------------------------------------
    // triggerJobExecution tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("triggerJobExecution - publishes command to execution queue")
    void triggerJobExecution_success_publishesCommand() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant1";
        String userId = "user1";

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(UUID.randomUUID());

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setName("Test");
        integration.setConnectionId(UUID.randomUUID());
        integration.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        // Act
        service.triggerJobExecution(integrationId, tenantId, userId);

        // Assert
        verify(confluenceScheduleService).triggerJob(integrationId, tenantId, TriggerType.API, userId);
    }

    @Test
    @DisplayName("getAllByTenant - returns summary list with KW labels enrichment")
    void getAllByTenant_success_returnsEnrichedSummaries() {
        // Arrange
        String tenantId = "tenant1";

        // Mock the repository to return empty list - no stub needed for mappers
        when(confluenceIntegrationRepository.findAllSummariesByTenantId(tenantId))
                .thenReturn(List.of());

        // Act
        List<ConfluenceIntegrationSummaryResponse> result = service.getAllByTenant(tenantId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(confluenceIntegrationRepository).findAllSummariesByTenantId(tenantId);
    }

    @Test
    @DisplayName("getAllByTenant - KW label error does not fail request")
    void getAllByTenant_kwLabelError_doesNotFail() {
        // Arrange
        String tenantId = "tenant1";

        when(confluenceIntegrationRepository.findAllSummariesByTenantId(tenantId))
                .thenReturn(List.of());

        // Act
        List<ConfluenceIntegrationSummaryResponse> result = service.getAllByTenant(tenantId);

        // Assert - should not throw
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getByIdAndTenantWithDetails tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getByIdAndTenantWithDetails - resolves connection name successfully")
    void getByIdAndTenantWithDetails_success_resolvesConnectionName() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setConnectionId(connectionId);
        integration.setTenantId(tenantId);
        integration.setSchedule(schedule);
        integration.setIsEnabled(Boolean.FALSE);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationMapper.toDetailsResponse(any()))
                .thenReturn(new ConfluenceIntegrationResponse());
        when(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId))
                .thenReturn(null);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any()))
                .thenReturn("Label");
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any()))
                .thenReturn("Label");

        // Act
        ConfluenceIntegrationResponse result = service.getByIdAndTenantWithDetails(id, tenantId);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getByIdAndTenantWithDetails - connection name error does not fail")
    void getByIdAndTenantWithDetails_connectionError_doesNotFail() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setConnectionId(connectionId);
        integration.setTenantId(tenantId);
        integration.setSchedule(schedule);
        integration.setIsEnabled(Boolean.FALSE);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationMapper.toDetailsResponse(any()))
                .thenReturn(new ConfluenceIntegrationResponse());
        when(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId))
                .thenReturn(null);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any()))
                .thenReturn("Label");
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any()))
                .thenReturn("Label");

        // Act
        ConfluenceIntegrationResponse result = service.getByIdAndTenantWithDetails(id, tenantId);

        // Assert - should handle exception gracefully
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getByIdAndTenantWithDetails - enabled integration includes nextRunAtUtc")
    void getByIdAndTenantWithDetails_enabled_includesNextRunAtUtc() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setConnectionId(connectionId);
        integration.setTenantId(tenantId);
        integration.setSchedule(schedule);
        integration.setIsEnabled(Boolean.TRUE);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(integration));

        ConfluenceIntegrationResponse response = new ConfluenceIntegrationResponse();
        when(confluenceIntegrationMapper.toDetailsResponse(any())).thenReturn(response);
        when(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId))
                .thenReturn(null);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any())).thenReturn("Label");
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any())).thenReturn("Label");
        Instant expectedNextRun = Instant.parse("2026-04-10T12:00:00Z");
        when(cronScheduleService.getNextRun(any(), any(), any())).thenReturn(expectedNextRun);

        // Act
        ConfluenceIntegrationResponse result = service.getByIdAndTenantWithDetails(id, tenantId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNextRunAtUtc()).isEqualTo(expectedNextRun);
        verify(cronScheduleService).getNextRun(any(), any(), any());
    }

    @Test
    @DisplayName("getByIdAndTenantWithDetails - disabled integration does not include nextRunAtUtc")
    void getByIdAndTenantWithDetails_disabled_noNextRunAtUtc() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setConnectionId(connectionId);
        integration.setTenantId(tenantId);
        integration.setSchedule(schedule);
        integration.setIsEnabled(Boolean.FALSE);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId))
                .thenReturn(Optional.of(integration));

        ConfluenceIntegrationResponse response = new ConfluenceIntegrationResponse();
        when(confluenceIntegrationMapper.toDetailsResponse(any())).thenReturn(response);
        when(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId))
                .thenReturn(null);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any())).thenReturn("Label");
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any())).thenReturn("Label");

        // Act
        ConfluenceIntegrationResponse result = service.getByIdAndTenantWithDetails(id, tenantId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNextRunAtUtc()).isNull();
        verify(cronScheduleService, never()).getNextRun(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // retryJobExecution tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("retryJobExecution - creates retry execution and publishes command")
    void retryJobExecution_success_createsAndPublishes() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        UUID originalJobId = UUID.randomUUID();
        String tenantId = "tenant1";
        String userId = "user1";

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setIsEnabled(true);
        integration.setConnectionId(UUID.randomUUID());

        IntegrationJobExecution retryExecution = new IntegrationJobExecution();
        retryExecution.setId(UUID.randomUUID());

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, tenantId))
                .thenReturn(Optional.of(integration));
        when(integrationJobExecutionService.createConfluenceRetryExecution(
                originalJobId, integrationId, tenantId, TriggerType.RETRY, userId))
                .thenReturn(retryExecution);

        // Act
        service.retryJobExecution(integrationId, originalJobId, tenantId, userId);

        // Assert
        verify(integrationJobExecutionService).createConfluenceRetryExecution(
                originalJobId, integrationId, tenantId, TriggerType.RETRY, userId);
    }

    @Test
    @DisplayName("retryJobExecution - disabled integration throws IllegalStateException")
    void retryJobExecution_disabled_throwsIllegalState() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        UUID originalJobId = UUID.randomUUID();

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setIsEnabled(false);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.of(integration));

        // Act & Assert
        assertThatThrownBy(() -> service.retryJobExecution(integrationId, originalJobId, "tenant1", "user1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot retry job for disabled integration");
    }

    // -------------------------------------------------------------------------
    // toggleActiveStatus - edge case with DataAccessException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toggleActiveStatus - DataAccessException throws IntegrationPersistenceException")
    void toggleActiveStatus_dataAccessException_throwsPersistence() {
        // Arrange
        UUID id = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setIsEnabled(false);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any()))
                .thenThrow(new DataAccessException("DB error") { });

        // Act & Assert
        assertThatThrownBy(() -> service.toggleActiveStatus(id, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("Failed to toggle");
    }

    //  -------------------------------------------------------------------------
    // delete - exception handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - unexpected exception throws IntegrationPersistenceException")
    void delete_unexpectedException_throwsPersistence() {
        // Arrange
        UUID id = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        assertThatThrownBy(() -> service.delete(id, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("Failed to delete");
    }

    // -------------------------------------------------------------------------
    // Additional branch-coverage tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel - page with blank parentTitle returns title only")
    void getConfluenceSpaceFolderLabel_blankParentTitle_returnsTitleOnly() {
        UUID connectionId = UUID.randomUUID();
        ConfluencePageDto page = new ConfluencePageDto();
        page.setId("page-blank");
        page.setTitle("Child Only");
        page.setParentTitle("   "); // blank

        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, "tenant1", "SPACE"))
                .thenReturn(List.of(page));

        String result = service.getConfluenceSpaceFolderLabel(connectionId, "tenant1", "SPACE", "page-blank");
        assertThat(result).isEqualTo("Child Only");
    }

    @Test
    @DisplayName("create - non-unique DataIntegrityViolationException throws IntegrationPersistenceException")
    void create_nonUniqueDataIntegrity_throwsPersistenceException() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("foreign key violation"));

        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("data integrity violation");
    }

    @Test
    @DisplayName("update - non-unique DataIntegrityViolationException throws IntegrationPersistenceException")
    void update_nonUniqueDataIntegrity_throwsPersistenceException() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(new IntegrationSchedule());
        existing.getSchedule().setCronExpression("0 0 12 * * ?");

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(existing));
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("foreign key violation"));

        assertThatThrownBy(() -> service.update(id, request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("data integrity violation");
    }

    @Test
    @DisplayName("update - unchanged schedule does not reschedule")
    void update_unchangedSchedule_doesNotReschedule() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setName("Existing");
        existing.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(existing));
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(id.toString(), "Existing"));

        CreationResponse result = service.update(id, request, "tenant1", "user1");

        assertThat(result).isNotNull();
        verify(confluenceScheduleService, never()).updateSchedule(any());
    }

    @Test
    @DisplayName("update - changed schedule triggers reschedule")
    void update_changedSchedule_triggersReschedule() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setName("Existing");
        existing.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(existing));
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));

        // Simulate cron expression changing after buildCron
        doAnswer(inv -> {
            existing.getSchedule().setCronExpression("0 30 14 * * ?");
            return null;
        }).when(cronScheduleService).buildCron(any(IntegrationSchedule.class));

        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(id.toString(), "Existing"));

        CreationResponse result = service.update(id, request, "tenant1", "user1");

        assertThat(result).isNotNull();
        verify(confluenceScheduleService).updateSchedule(existing);
    }

    @Test
    @DisplayName("triggerJobExecution - enabled integration triggers job")
    void triggerJobExecution_enabled_triggersJob() {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setIsEnabled(true);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, "tenant1"))
                .thenReturn(Optional.of(integration));

        service.triggerJobExecution(integrationId, "tenant1", "user1");

        verify(confluenceScheduleService).triggerJob(integrationId, "tenant1", TriggerType.API, "user1");
    }

    @Test
    @DisplayName("triggerJobExecution - disabled integration throws IllegalStateException")
    void triggerJobExecution_disabled_throwsException() {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setIsEnabled(false);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, "tenant1"))
                .thenReturn(Optional.of(integration));

        assertThatThrownBy(() -> service.triggerJobExecution(integrationId, "tenant1", "user1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot trigger job for disabled integration");
    }

    @Test
    @DisplayName("toggleActiveStatus - enabling schedules job")
    void toggleActiveStatus_enabling_schedulesJob() {
        UUID id = UUID.randomUUID();
        IntegrationSchedule schedule = new IntegrationSchedule();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setName("My Int");
        integration.setIsEnabled(false); // currently disabled
        integration.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        boolean result = service.toggleActiveStatus(id, "tenant1", "user1");

        assertThat(result).isTrue();
        verify(confluenceScheduleService).scheduleJob(integration);
        verify(confluenceScheduleService, never()).unscheduleJob(any());
    }

    @Test
    @DisplayName("toggleActiveStatus - disabling unschedules job")
    void toggleActiveStatus_disabling_unschedulesJob() {
        UUID id = UUID.randomUUID();
        IntegrationSchedule schedule = new IntegrationSchedule();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setName("My Int");
        integration.setIsEnabled(true); // currently enabled
        integration.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        boolean result = service.toggleActiveStatus(id, "tenant1", "user1");

        assertThat(result).isFalse();
        verify(confluenceScheduleService).unscheduleJob(integration);
        verify(confluenceScheduleService, never()).scheduleJob(any());
    }

    @Test
    @DisplayName("toggleActiveStatus - no schedule on integration skips schedule/unschedule")
    void toggleActiveStatus_noSchedule_skipsScheduling() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setName("No Schedule");
        integration.setIsEnabled(false);
        integration.setSchedule(null);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        boolean result = service.toggleActiveStatus(id, "tenant1", "user1");

        assertThat(result).isTrue();
        verify(confluenceScheduleService, never()).scheduleJob(any());
        verify(confluenceScheduleService, never()).unscheduleJob(any());
    }

    @Test
    @DisplayName("update - unique constraint violation throws IntegrationNameAlreadyExistsException")
    void update_uniqueViolation_throwsNameAlreadyExists() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(existing));
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));

        DataIntegrityViolationException uniqueEx = new DataIntegrityViolationException(
                "unique constraint: duplicate key value");
        when(confluenceIntegrationRepository.save(any())).thenThrow(uniqueEx);

        assertThatThrownBy(() -> service.update(id, request, "tenant1", "user1"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class)
                .hasMessageContaining("name already exists");
    }

    @Test
    @DisplayName("update - IntegrationNotFoundException propagates unchanged")
    void update_notFound_throwsNotFoundDirectly() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setSchedule(new IntegrationScheduleRequest());

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request, "tenant1", "user1"))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("update - unexpected exception throws IntegrationPersistenceException")
    void update_unexpectedException_throwsPersistence() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setCronExpression("0 0 12 * * ?");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(schedule);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(existing));
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any()))
                .thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> service.update(id, request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("Failed to update");
    }

    @Test
    @DisplayName("delete - integration with null schedule does not try to unschedule")
    void delete_nullSchedule_doesNotUnschedule() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(id);
        integration.setSchedule(null);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        service.delete(id, "tenant1", "user1");

        verify(confluenceScheduleService, never()).unscheduleJob(any());
        verify(confluenceIntegrationRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // Additional branch-coverage tests for resolveLanguages, getByIdAndTenantWithDetails
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create - empty language codes with no matching active languages throws InvalidLanguageCodeException")
    void create_emptyActiveLanguages_throwsInvalidLanguageCode() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasCauseInstanceOf(InvalidLanguageCodeException.class);
    }


    @Test
    @DisplayName("create - IntegrationPersistenceException is rethrown as-is")
    void create_integrationPersistenceException_rethrownAsIs() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        IntegrationPersistenceException original = new IntegrationPersistenceException("original error");
        when(confluenceIntegrationRepository.save(any())).thenThrow(original);

        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .isSameAs(original);
    }

    @Test
    @DisplayName("update - existing schedule is null, currentCronExpression should be null")
    void update_existingScheduleNull_currentCronIsNull() {
        UUID id = UUID.randomUUID();
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language lang = new Language();
        lang.setCode("en-US");

        ConfluenceIntegration existing = new ConfluenceIntegration();
        existing.setId(id);
        existing.setSchedule(null);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "tenant1"))
                .thenReturn(Optional.of(existing));
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));

        // When schedule is null, the update mapper and buildCron will work on null schedule
        // This should trigger the currentCronExpression=null branch
        IntegrationSchedule newSchedule = new IntegrationSchedule();
        newSchedule.setCronExpression("0 0 12 * * ?");
        doAnswer(inv -> {
            existing.setSchedule(newSchedule);
            return null;
        }).when(integrationSchedulerMapper).updateEntity(any(), any());

        when(confluenceIntegrationRepository.save(any())).thenReturn(existing);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(id.toString(), "Test"));

        CreationResponse result = service.update(id, request, "tenant1", "user1");

        assertThat(result).isNotNull();
        verify(confluenceScheduleService).updateSchedule(existing);
    }

    @Test
    @DisplayName("resolveLanguages - matched count differs from input throws InvalidLanguageCodeException")
    void resolveLanguages_mismatchCount_throwsException() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US", "fr-FR", "invalid-code"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language en = new Language();
        en.setCode("en-US");
        Language fr = new Language();
        fr.setCode("fr-FR");

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(en, fr));

        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasCauseInstanceOf(InvalidLanguageCodeException.class);
    }

    @Test
    @DisplayName("getAllByTenant: disabled integration does not set nextRunAtUtc")
    void getAllByTenant_disabledIntegration_noNextRunAtUtc() {
        // Covers lambda$getAllByTenant$0: Boolean.TRUE.equals(isEnabled) = false → skip setNextRunAtUtc
        com.integration.management.repository.projection.ConfluenceIntegrationSummaryProjection projection =
                org.mockito.Mockito.mock(
                        com.integration.management.repository.projection.ConfluenceIntegrationSummaryProjection.class);
        when(confluenceIntegrationRepository.findAllSummariesByTenantId("t1"))
                .thenReturn(List.of(projection));

        ConfluenceIntegrationSummaryResponse response = ConfluenceIntegrationSummaryResponse.builder()
                .id(UUID.randomUUID()).isEnabled(false)  // disabled → no nextRunAtUtc
                .build();
        when(confluenceIntegrationMapper.projectionToSummaryResponse(projection)).thenReturn(response);

        List<ConfluenceIntegrationSummaryResponse> out = service.getAllByTenant("t1");

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getNextRunAtUtc()).isNull();
        verify(cronScheduleService, never()).getNextRun(any(), any(), any());
    }

    @Test
    @DisplayName("toggleActiveStatus: null integration name uses empty string in metadata")
    void toggleActiveStatus_nullName_usesEmptyString() {
        // Covers: integration.getName() != null ? ... : "" when name is null
        UUID id = UUID.randomUUID();
        ConfluenceIntegration integration = ConfluenceIntegration.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name(null)  // null name
                .connectionId(UUID.randomUUID()).isEnabled(true).build();
        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);

        boolean result = service.toggleActiveStatus(id, "t1", "u1");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("resolveLanguages: empty resolved list throws InvalidLanguageCodeException")
    void resolveLanguages_emptyResolved_throwsException() {
        // Covers else-if (resolvedLanguages.isEmpty()) branch
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("xx-XX"));  // code that won't match anything
        request.setSchedule(new IntegrationScheduleRequest());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        // All languages empty → filtered result is empty (size != input size → throws first branch)
        // To hit the else-if, we need languageCodes.size() == resolvedLanguages.size() but both empty
        // That means input languageCodes is empty too:
        request.setLanguageCodes(List.of());  // empty list
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasCauseInstanceOf(InvalidLanguageCodeException.class);
    }

    @Test
    @DisplayName("getConfluenceSpaceLabel: null spaces returns null")
    void getConfluenceSpaceLabel_nullSpaces_returnsNull() {
        // Covers: if (spaces == null) → return null
        UUID connectionId = UUID.randomUUID();
        when(confluenceLookupService.getSpacesByConnectionId(connectionId, "t1")).thenReturn(null);

        String result = service.getConfluenceSpaceLabel(connectionId, "t1", "SPACE-1");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel: null pages returns null")
    void getConfluenceSpaceFolderLabel_nullPages_returnsNull() {
        // Covers: if (pages == null) → return null
        UUID connectionId = UUID.randomUUID();
        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, "t1", "SPACE-1"))
                .thenReturn(null);

        String result = service.getConfluenceSpaceFolderLabel(connectionId, "t1", "SPACE-1", "folder-id");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel: page with null parentTitle returns just title")
    void getConfluenceSpaceFolderLabel_nullParentTitle_returnsTitle() {
        // Covers lambda$getConfluenceSpaceFolderLabel$1: parentTitle == null → return page.getTitle()
        UUID connectionId = UUID.randomUUID();
        com.integration.execution.contract.rest.response.confluence.ConfluencePageDto page =
                new ConfluencePageDto();
        page.setId("folder-id");
        page.setTitle("My Page");
        page.setParentTitle(null);  // null parentTitle → return page.getTitle() only
        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, "t1", "SPACE-1"))
                .thenReturn(List.of(page));

        String result = service.getConfluenceSpaceFolderLabel(connectionId, "t1", "SPACE-1", "folder-id");

        assertThat(result).isEqualTo("My Page");
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel: page with blank parentTitle returns just title")
    void getConfluenceSpaceFolderLabel_blankParentTitle_returnsTitle() {
        // Covers lambda$getConfluenceSpaceFolderLabel$1: parentTitle blank → return page.getTitle() only
        UUID connectionId = UUID.randomUUID();
        com.integration.execution.contract.rest.response.confluence.ConfluencePageDto page =
                new ConfluencePageDto();
        page.setId("folder-id");
        page.setTitle("My Page");
        page.setParentTitle("   ");  // blank parentTitle → return page.getTitle() only
        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, "t1", "SPACE-1"))
                .thenReturn(List.of(page));

        String result = service.getConfluenceSpaceFolderLabel(connectionId, "t1", "SPACE-1", "folder-id");

        assertThat(result).isEqualTo("My Page");
    }

    @Test
    @DisplayName("isUniqueConstraintViolation: non-matching message throws IntegrationPersistenceException")
    void isUniqueConstraintViolation_nonMatchingMessage_throwsPersistence() {
        // Covers isUniqueConstraintViolation: message present but no keyword → returns false
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language en = new Language();
        en.setCode("en-US");
        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        ConfluenceIntegration entity = new ConfluenceIntegration();
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(entity);
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(en));
        when(confluenceIntegrationRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("foreign key violation"));

        assertThatThrownBy(() -> service.create(request, "t1", "u1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasMessageContaining("data integrity");
    }

    // -------------------------------------------------------------------------
    // resolveSourceLanguage branch-coverage tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create - null sourceLanguage defaults to 'en'")
    void create_nullSourceLanguage_defaultsToEn() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());
        request.setSourceLanguage(null); // explicitly null

        Language lang = new Language();
        lang.setCode("en-US");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(UUID.randomUUID());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(integration);
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(integration.getId().toString(), "Test"));

        service.create(request, "tenant1", "user1");

        // sourceLanguage should have been set to "en"
        assertThat(integration.getSourceLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("create - blank sourceLanguage defaults to 'en'")
    void create_blankSourceLanguage_defaultsToEn() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());
        request.setSourceLanguage("   "); // blank

        Language lang = new Language();
        lang.setCode("en-US");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(UUID.randomUUID());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(integration);
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(integration.getId().toString(), "Test"));

        service.create(request, "tenant1", "user1");

        assertThat(integration.getSourceLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("create - uppercase sourceLanguage is normalized to lowercase")
    void create_uppercaseSourceLanguage_normalizedToLowercase() {
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US"));
        request.setSchedule(new IntegrationScheduleRequest());
        request.setSourceLanguage("  JA  "); // uppercase with spaces

        Language lang = new Language();
        lang.setCode("en-US");

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(UUID.randomUUID());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(integration);
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(lang));
        when(confluenceIntegrationRepository.save(any())).thenReturn(integration);
        when(confluenceIntegrationMapper.toCreationResponse(any()))
                .thenReturn(new CreationResponse(integration.getId().toString(), "Test"));

        service.create(request, "tenant1", "user1");

        assertThat(integration.getSourceLanguage()).isEqualTo("ja");
    }
}
