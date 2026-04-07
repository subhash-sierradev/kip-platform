package com.integration.management.service;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.integration.management.constants.IntegrationManagementConstants.ROOT_FOLDER_KEY;
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
        integration.setIsEnabled(true);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        // Act
        service.triggerJobExecution(integrationId, tenantId, userId);

        // Assert schedule trigger
        verify(confluenceScheduleService).triggerJob(integrationId, tenantId, TriggerType.API, userId);

        // Assert adhoc-run notification is published
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.getEventKey())
                .isEqualTo(NotificationEventKey.CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN.name());
        assertThat(event.getTenantId()).isEqualTo(tenantId);
        assertThat(event.getTriggeredByUserId()).isEqualTo(userId);
        assertThat(event.getMetadata()).containsKey("integrationName");
        assertThat(event.getMetadata()).containsKey("integrationId");
        assertThat(event.getMetadata()).containsKey("triggeredBy");
    }

    @Test
    @DisplayName("triggerJobExecution - null integration name uses empty string in notification")
    void triggerJobExecution_nullName_usesEmptyStringInNotification() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant1";
        String userId = "user1";

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setName(null);
        integration.setIsEnabled(true);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        // Act
        service.triggerJobExecution(integrationId, tenantId, userId);

        // Assert notification metadata uses empty string for null name
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getMetadata().get("integrationName")).isEqualTo("");
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

    // -------------------------------------------------------------------------
    // triggerJobExecution - edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("triggerJobExecution - disabled integration throws IllegalStateException")
    void triggerJobExecution_disabled_throwsIllegalState() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setIsEnabled(false);

        when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.of(integration));

        // Act & Assert
        assertThatThrownBy(() -> service.triggerJobExecution(integrationId, "tenant1", "user1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot trigger job for disabled integration");

        verify(confluenceScheduleService, never()).triggerJob(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // getAllConfluenceNormalizedNamesByTenantId tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllConfluenceNormalizedNamesByTenantId - returns list of normalized names")
    void getAllConfluenceNormalizedNamesByTenantId_success_returnsList() {
        // Arrange
        String tenantId = "tenant1";
        List<String> expectedNames = List.of("integration_1", "integration_2");
        when(confluenceIntegrationRepository.findAllNormalizedNamesByTenantId(tenantId))
                .thenReturn(expectedNames);

        // Act
        List<String> result = service.getAllConfluenceNormalizedNamesByTenantId(tenantId);

        // Assert
        assertThat(result).isEqualTo(expectedNames);
        verify(confluenceIntegrationRepository).findAllNormalizedNamesByTenantId(tenantId);
    }

    // -------------------------------------------------------------------------
    // getConfluenceIntegrationNameById tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getConfluenceIntegrationNameById - returns integration name")
    void getConfluenceIntegrationNameById_found_returnsName() {
        // Arrange
        String entityId = UUID.randomUUID().toString();
        String expectedName = "Test Integration";
        when(confluenceIntegrationRepository.findIntegrationNameById(UUID.fromString(entityId)))
                .thenReturn(Optional.of(expectedName));

        // Act
        String result = service.getConfluenceIntegrationNameById(entityId);

        // Assert
        assertThat(result).isEqualTo(expectedName);
    }

    @Test
    @DisplayName("getConfluenceIntegrationNameById - not found throws IntegrationNotFoundException")
    void getConfluenceIntegrationNameById_notFound_throwsException() {
        // Arrange
        String entityId = UUID.randomUUID().toString();
        when(confluenceIntegrationRepository.findIntegrationNameById(any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getConfluenceIntegrationNameById(entityId))
                .isInstanceOf(IntegrationNotFoundException.class)
                .hasMessageContaining("name not found");
    }

    // -------------------------------------------------------------------------
    // getEnabledIntegrationForScheduledExecution tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getEnabledIntegrationForScheduledExecution - returns enabled integration")
    void getEnabledIntegrationForScheduledExecution_found_returnsIntegration() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant1";
        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);

        when(confluenceIntegrationRepository.findEnabledByIdAndTenantIdWithSchedule(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        // Act
        Optional<ConfluenceIntegration> result =
                service.getEnabledIntegrationForScheduledExecution(integrationId, tenantId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(integration);
    }

    @Test
    @DisplayName("getEnabledIntegrationForScheduledExecution - not found returns empty")
    void getEnabledIntegrationForScheduledExecution_notFound_returnsEmpty() {
        // Arrange
        when(confluenceIntegrationRepository.findEnabledByIdAndTenantIdWithSchedule(any(), any()))
                .thenReturn(Optional.empty());

        // Act
        Optional<ConfluenceIntegration> result =
                service.getEnabledIntegrationForScheduledExecution(UUID.randomUUID(), "tenant1");

        // Assert
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // resolveLanguages tests (via create/update)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create - invalid language code throws IntegrationPersistenceException")
    void create_invalidLanguageCode_throwsException() {
        // Arrange
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("invalid-code"));
        request.setSchedule(new IntegrationScheduleRequest());

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasCauseInstanceOf(InvalidLanguageCodeException.class);
    }

    @Test
    @DisplayName("create - partial invalid codes throws IntegrationPersistenceException")
    void create_partialInvalidLanguageCodes_throwsException() {
        // Arrange
        ConfluenceIntegrationCreateUpdateRequest request = new ConfluenceIntegrationCreateUpdateRequest();
        request.setLanguageCodes(List.of("en-US", "invalid-code"));
        request.setSchedule(new IntegrationScheduleRequest());

        Language validLang = new Language();
        validLang.setCode("en-US");

        when(integrationSchedulerMapper.toEntity(any())).thenReturn(new IntegrationSchedule());
        when(confluenceIntegrationMapper.toEntity(any())).thenReturn(new ConfluenceIntegration());
        when(masterDataService.getAllActiveLanguages()).thenReturn(List.of(validLang));

        // Act & Assert
        assertThatThrownBy(() -> service.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationPersistenceException.class)
                .hasCauseInstanceOf(InvalidLanguageCodeException.class);
    }

    // -------------------------------------------------------------------------
    // getConfluenceSpaceLabel tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getConfluenceSpaceLabel - found returns formatted label")
    void getConfluenceSpaceLabel_found_returnsLabel() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";
        String spaceKey = "SPACE1";

        ConfluenceSpaceDto space = new ConfluenceSpaceDto();
        space.setKey("SPACE1");
        space.setName("My Space");

        when(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId))
                .thenReturn(List.of(space));

        // Act
        String result = service.getConfluenceSpaceLabel(connectionId, tenantId, spaceKey);

        // Assert
        assertThat(result).isEqualTo("My Space (SPACE1)");
    }

    @Test
    @DisplayName("getConfluenceSpaceLabel - not found returns null")
    void getConfluenceSpaceLabel_notFound_returnsNull() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";

        ConfluenceSpaceDto space = new ConfluenceSpaceDto();
        space.setKey("SPACE2");
        space.setName("Other Space");

        when(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId))
                .thenReturn(List.of(space));

        // Act
        String result = service.getConfluenceSpaceLabel(connectionId, tenantId, "SPACE1");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getConfluenceSpaceLabel - null spaces list returns null")
    void getConfluenceSpaceLabel_nullList_returnsNull() {
        // Arrange
        when(confluenceLookupService.getSpacesByConnectionId(any(), any()))
                .thenReturn(null);

        // Act
        String result = service.getConfluenceSpaceLabel(UUID.randomUUID(), "tenant1", "SPACE1");

        // Assert
        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // getConfluenceSpaceFolderLabel tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel - root folder returns ROOT_FOLDER_KEY")
    void getConfluenceSpaceFolderLabel_rootFolder_returnsConstant() {
        // Act
        String result = service.getConfluenceSpaceFolderLabel(
                UUID.randomUUID(), "tenant1", "SPACE1", ROOT_FOLDER_KEY);

        // Assert
        assertThat(result).isEqualTo(ROOT_FOLDER_KEY);
        verify(confluenceLookupService, never()).getPagesByConnectionIdAndSpaceKey(any(), any(), any());
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel - page with parent returns full path")
    void getConfluenceSpaceFolderLabel_pageWithParent_returnsFullPath() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";
        String spaceKey = "SPACE1";
        String folderId = "page123";

        ConfluencePageDto page = new ConfluencePageDto();
        page.setId("page123");
        page.setTitle("Child Page");
        page.setParentTitle("Parent Page");

        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, tenantId, spaceKey))
                .thenReturn(List.of(page));

        // Act
        String result = service.getConfluenceSpaceFolderLabel(connectionId, tenantId, spaceKey, folderId);

        // Assert
        assertThat(result).isEqualTo("Parent Page > Child Page");
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel - page without parent returns title only")
    void getConfluenceSpaceFolderLabel_pageWithoutParent_returnsTitleOnly() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant1";
        String spaceKey = "SPACE1";
        String folderId = "page123";

        ConfluencePageDto page = new ConfluencePageDto();
        page.setId("page123");
        page.setTitle("Top Level Page");
        page.setParentTitle(null);

        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, tenantId, spaceKey))
                .thenReturn(List.of(page));

        // Act
        String result = service.getConfluenceSpaceFolderLabel(connectionId, tenantId, spaceKey, folderId);

        // Assert
        assertThat(result).isEqualTo("Top Level Page");
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel - null pages list returns null")
    void getConfluenceSpaceFolderLabel_nullList_returnsNull() {
        // Arrange
        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(any(), any(), any()))
                .thenReturn(null);

        // Act
        String result = service.getConfluenceSpaceFolderLabel(
                UUID.randomUUID(), "tenant1", "SPACE1", "page123");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getConfluenceSpaceFolderLabel - page not found returns null")
    void getConfluenceSpaceFolderLabel_pageNotFound_returnsNull() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        ConfluencePageDto page = new ConfluencePageDto();
        page.setId("page999");
        page.setTitle("Other Page");

        when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(any(), any(), any()))
                .thenReturn(List.of(page));

        // Act
        String result = service.getConfluenceSpaceFolderLabel(connectionId, "tenant1", "SPACE1", "page123");

        // Assert
        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // getJobHistory tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getJobHistory - returns mapped list of job executions")
    void getJobHistory_success_returnsMappedList() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant1";
        IntegrationJobExecution execution = new IntegrationJobExecution();
        execution.setId(UUID.randomUUID());

        IntegrationJobExecutionDto dto = new IntegrationJobExecutionDto();
        dto.setId(execution.getId());

        when(integrationJobExecutionService.getConfluenceJobHistory(integrationId, tenantId))
                .thenReturn(List.of(execution));
        when(integrationJobExecutionMapper.toDto(execution)).thenReturn(dto);

        // Act
        List<IntegrationJobExecutionDto> result = service.getJobHistory(integrationId, tenantId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(execution.getId());
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
}
