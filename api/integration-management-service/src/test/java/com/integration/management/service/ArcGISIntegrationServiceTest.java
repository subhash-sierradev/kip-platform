package com.integration.management.service;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.IntegrationScheduleResponse;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationFieldMapping;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.ies.client.IesArcGISApiClient;
import com.integration.management.mapper.ArcGISIntegrationMapper;
import com.integration.management.mapper.IntegrationFieldMappingMapper;
import com.integration.management.mapper.IntegrationJobExecutionMapper;
import com.integration.management.mapper.IntegrationSchedulerMapper;
import com.integration.management.model.dto.request.ArcGISIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.management.model.dto.response.ArcGISIntegrationResponse;
import com.integration.management.model.dto.response.ArcGISIntegrationSummaryResponse;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.projection.ArcGISIntegrationSummaryProjection;
import com.integration.management.repository.IntegrationFieldMappingRepository;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISIntegrationService")
class ArcGISIntegrationServiceTest {

    @Mock
    private ArcGISIntegrationRepository arcGISIntegrationRepository;
    @Mock
    private ArcGISIntegrationMapper arcGISIntegrationMapper;
    @Mock
    private CronScheduleService cronScheduleService;
    @Mock
    private IntegrationSchedulerMapper integrationSchedulerMapper;
    @Mock
    private IntegrationFieldMappingMapper integrationFieldMappingMapper;
    @Mock
    private IntegrationFieldMappingRepository integrationFieldMappingRepository;
    @Mock
    private ArcGISScheduleService arcGISScheduleService;
    @Mock
    private IntegrationJobExecutionRepository integrationJobExecutionRepository;
    @Mock
    private IntegrationJobExecutionMapper integrationJobExecutionMapper;
    @Mock
    private KwIntegrationService kwIntegrationService;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private IesArcGISApiClient iesArcGISApiClient;
    @Mock
    private IntegrationConnectionService integrationConnectionService;

    @InjectMocks
    private ArcGISIntegrationService service;

    private static ArcGISIntegrationCreateUpdateRequest sampleRequest(UUID connectionId) {
        IntegrationScheduleRequest schedule = IntegrationScheduleRequest.builder()
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .build();
        IntegrationFieldMappingDto mappingDto = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("a")
                .targetFieldPath("b")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();
        return ArcGISIntegrationCreateUpdateRequest.builder()
                .name("Integration A")
                .itemType("DOCUMENT")
                .itemSubtype("S")
                .connectionId(connectionId)
                .schedule(schedule)
                .fieldMappings(List.of(mappingDto))
                .build();
    }

    @Test
    @DisplayName("create should persist integration, save mappings, and schedule job")
    void create_success_persistsAndSchedules() {
        UUID connectionId = UUID.randomUUID();
        ArcGISIntegrationCreateUpdateRequest request = sampleRequest(connectionId);
        String tenantId = "tenant-1";
        String userId = "user-1";

        IntegrationSchedule scheduleEntity = IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .cronExpression("0 0 10 * * ?")
                .build();
        when(integrationSchedulerMapper.toEntity(request.getSchedule())).thenReturn(scheduleEntity);

        IntegrationFieldMapping mapping = IntegrationFieldMapping.builder()
                .id(UUID.randomUUID())
                .sourceFieldPath("a")
                .targetFieldPath("b")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .integrationId(UUID.randomUUID())
                .build();
        when(integrationFieldMappingMapper.toEntities(request.getFieldMappings()))
                .thenReturn(List.of(mapping));

        ArcGISIntegration integration = ArcGISIntegration.builder()
                .name(request.getName())
                .itemType(request.getItemType())
                .itemSubtype(request.getItemSubtype())
                .connectionId(connectionId)
                .build();
        when(arcGISIntegrationMapper.toEntity(request)).thenReturn(integration);

        UUID id = UUID.randomUUID();
        when(arcGISIntegrationRepository.save(any(ArcGISIntegration.class))).thenAnswer(inv -> {
            ArcGISIntegration arg = inv.getArgument(0);
            arg.setId(id);
            return arg;
        });
        when(integrationFieldMappingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(arcGISIntegrationMapper.toCreationResponse(any(ArcGISIntegration.class)))
                .thenReturn(CreationResponse.builder().id(id.toString()).name("Integration A").build());

        CreationResponse response = service.create(request, tenantId, userId);

        assertThat(response.getId()).isEqualTo(id.toString());
        verify(arcGISScheduleService).scheduleJob(any(ArcGISIntegration.class));

        ArgumentCaptor<List<IntegrationFieldMapping>> mappingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(integrationFieldMappingRepository).saveAll(mappingsCaptor.capture());
        assertThat(mappingsCaptor.getValue()).hasSize(1);
        assertThat(mappingsCaptor.getValue().getFirst().getIntegrationId()).isEqualTo(id);
        assertThat(mappingsCaptor.getValue().getFirst().getId()).isNull();
    }

    @Test
    @DisplayName("create should throw IntegrationNameAlreadyExistsException on unique constraint")
    void create_uniqueConstraint_throwsNameExists() {
        ArcGISIntegrationCreateUpdateRequest request = sampleRequest(UUID.randomUUID());
        when(integrationSchedulerMapper.toEntity(any())).thenReturn(IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .cronExpression("0 0 10 * * ?")
                .build());
        when(integrationFieldMappingMapper.toEntities(any())).thenReturn(List.of());
        when(arcGISIntegrationMapper.toEntity(any())).thenReturn(ArcGISIntegration.builder()
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .build());
        when(arcGISIntegrationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.create(request, "t", "u"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("update should clear dynamicDocumentType when non-dynamic subtype and update schedule when cron changes")
    void update_cronChange_updatesSchedule_andClearsDynamicType() {
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";

        IntegrationSchedule existingSchedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .cronExpression("old")
                .build();
        ArcGISIntegration existing = ArcGISIntegration.builder()
                .id(integrationId)
                .tenantId(tenantId)
                .createdBy("u")
                .lastModifiedBy("u")
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("DOCUMENT_FINAL_DYNAMIC")
                .dynamicDocumentType("DT")
                .connectionId(UUID.randomUUID())
                .schedule(existingSchedule)
                .isEnabled(true)
                .build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(integrationId, tenantId))
                .thenReturn(Optional.of(existing));

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("NON_DYNAMIC")
                .connectionId(existing.getConnectionId())
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .build())
                .fieldMappings(List.of())
                .build();

        when(arcGISIntegrationMapper.toCreationResponse(existing))
                .thenReturn(CreationResponse.builder().id(integrationId.toString()).name("n").build());

        org.mockito.Mockito.doAnswer(inv -> {
            ArcGISIntegrationCreateUpdateRequest req = inv.getArgument(0);
            ArcGISIntegration entity = inv.getArgument(1);
            entity.setItemSubtype(req.getItemSubtype());
            return null;
        }).when(arcGISIntegrationMapper).updateEntity(eq(request), eq(existing));

        org.mockito.Mockito.doAnswer(inv -> {
            IntegrationSchedule schedule = inv.getArgument(0);
            schedule.setCronExpression("new");
            return null;
        }).when(cronScheduleService).buildCron(existingSchedule);

        CreationResponse out = service.update(integrationId, request, tenantId, "user-1");

        assertThat(out.getId()).isEqualTo(integrationId.toString());
        assertThat(existing.getDynamicDocumentType()).isNull();
        verify(arcGISScheduleService).updateSchedule(existing);
        verify(integrationFieldMappingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getAllByTenant should enrich labels and set nextRunAtUtc only when enabled")
    void getAllByTenant_enrichesAndComputesNextRun() {
        ArcGISIntegrationSummaryProjection projection = org.mockito.Mockito
                .mock(ArcGISIntegrationSummaryProjection.class);
        when(arcGISIntegrationRepository.findAllSummariesByTenantId("t"))
                .thenReturn(List.of(projection));

        ArcGISIntegrationSummaryResponse response = ArcGISIntegrationSummaryResponse.builder()
                .id(UUID.randomUUID())
                .itemSubtype("S")
                .dynamicDocumentType("D")
                .cronExpression("0 0 10 * * ?")
                .executionTime(LocalTime.of(10, 0))
                .isEnabled(true)
                .build();
        when(arcGISIntegrationMapper.projectionToResponse(projection)).thenReturn(response);
        when(kwIntegrationService.getItemSubtypeDisplayValue("S")).thenReturn("Subtype");
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any())).thenReturn("Dyn");
        when(cronScheduleService.getNextRun(any(), any(), any()))
                .thenReturn(Instant.parse("2026-03-03T00:00:00Z"));

        List<ArcGISIntegrationSummaryResponse> out = service.getAllByTenant("t");

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getItemSubtypeLabel()).isEqualTo("Subtype");
        assertThat(out.getFirst().getDynamicDocumentTypeLabel()).isEqualTo("Dyn");
        assertThat(out.getFirst().getNextRunAtUtc()).isNotNull();
    }

    @Test
    @DisplayName("triggerJobExecution should throw for disabled integration")
    void triggerJobExecution_disabled_throws() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(id)
                .tenantId("t")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationSchedule.builder()
                        .id(UUID.randomUUID())
                        .executionTime(LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .cronExpression("0 0 10 * * ?")
                        .build())
                .isEnabled(false)
                .build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(integration));

        assertThatThrownBy(() -> service.triggerJobExecution(id, "t", "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("toggleActiveStatus should schedule when enabling and unschedule when disabling")
    void toggleActiveStatus_schedulesOrUnschedules() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(id)
                .tenantId("t")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationSchedule.builder()
                        .id(UUID.randomUUID())
                        .executionTime(LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .cronExpression("0 0 10 * * ?")
                        .build())
                .isEnabled(false)
                .build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(integration));

        boolean newStatus = service.toggleActiveStatus(id, "t", "u2");
        assertThat(newStatus).isTrue();
        verify(arcGISScheduleService).scheduleJob(integration);

        // toggle again -> disable
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(integration));
        boolean status2 = service.toggleActiveStatus(id, "t", "u2");
        assertThat(status2).isFalse();
        verify(arcGISScheduleService).unscheduleJob(integration);
    }

    @Test
    @DisplayName("triggerJobExecution should call schedule service with API trigger")
    void triggerJobExecution_enabled_triggers() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(id)
                .tenantId("t")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationSchedule.builder()
                        .id(UUID.randomUUID())
                        .executionTime(LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .cronExpression("0 0 10 * * ?")
                        .build())
                .isEnabled(true)
                .build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(integration));

        service.triggerJobExecution(id, "t", "u");

        verify(arcGISScheduleService).triggerJob(id, "t", TriggerType.API, "u");
    }

    @Test
    @DisplayName("getArcGISIntegrationNameById should return value from repository")
    void getArcGISIntegrationNameById_returns() {
        UUID id = UUID.randomUUID();
        when(arcGISIntegrationRepository.findIntegrationNameById(id)).thenReturn(Optional.of("n"));

        assertThat(service.getArcGISIntegrationNameById(id.toString())).isEqualTo("n");
    }

    // ── Additional branch coverage tests ────────────────────────────────────────

    @Test
    @DisplayName("create should throw IntegrationPersistenceException for non-unique DataIntegrityViolationException")
    void create_nonUniqueDataIntegrity_throwsPersistence() {
        ArcGISIntegrationCreateUpdateRequest request = buildCreateRequest("My Integration");
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID()).executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY).build();
        when(integrationSchedulerMapper.toEntity(any())).thenReturn(schedule);
        when(integrationFieldMappingMapper.toEntities(any())).thenReturn(List.of());
        when(arcGISIntegrationMapper.toEntity(request)).thenReturn(ArcGISIntegration.builder()
                .id(UUID.randomUUID()).name("My Integration").build());
        when(arcGISIntegrationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("other violation"));

        assertThatThrownBy(() -> service.create(request, "t1", "u1"))
                .isInstanceOf(com.integration.management.exception.IntegrationPersistenceException.class)
                .hasMessageContaining("data integrity violation");
    }

    @Test
    @DisplayName("create should throw IntegrationPersistenceException for unexpected exception")
    void create_unexpectedException_throwsPersistence() {
        ArcGISIntegrationCreateUpdateRequest request = buildCreateRequest("My Integration");
        when(integrationSchedulerMapper.toEntity(any())).thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> service.create(request, "t1", "u1"))
                .isInstanceOf(com.integration.management.exception.IntegrationPersistenceException.class)
                .hasMessageContaining("Failed to create ArcGIS integration");
    }

    @Test
    @DisplayName("update should throw IntegrationNameAlreadyExistsException for unique DataIntegrityViolation")
    void update_uniqueConstraint_throwsNameAlreadyExists() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration existing = buildExistingIntegration(id);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(existing));
        when(arcGISIntegrationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        ArcGISIntegrationCreateUpdateRequest request = buildCreateRequest("Dup");
        org.mockito.Mockito.doNothing().when(arcGISIntegrationMapper).updateEntity(any(), any());
        org.mockito.Mockito.doNothing().when(integrationSchedulerMapper).updateEntity(any(), any());

        assertThatThrownBy(() -> service.update(id, request, "t1", "u1"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("update should throw IntegrationNotFoundException when integration not found")
    void update_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, buildCreateRequest("n"), "t1", "u1"))
                .isInstanceOf(com.integration.management.exception.IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("getAllByTenant should set nextRunAtUtc to null for disabled integration")
    void getAllByTenant_disabled_noNextRun() {
        ArcGISIntegrationSummaryProjection projection = org.mockito.Mockito
                .mock(ArcGISIntegrationSummaryProjection.class);
        when(arcGISIntegrationRepository.findAllSummariesByTenantId("t"))
                .thenReturn(List.of(projection));

        ArcGISIntegrationSummaryResponse response = ArcGISIntegrationSummaryResponse.builder()
                .id(UUID.randomUUID()).itemSubtype("S").dynamicDocumentType("D")
                .cronExpression("0 0 10 * * ?").executionTime(LocalTime.of(10, 0))
                .isEnabled(false).build();
        when(arcGISIntegrationMapper.projectionToResponse(projection)).thenReturn(response);
        when(kwIntegrationService.getItemSubtypeDisplayValue("S")).thenReturn(null);
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any())).thenReturn(null);

        List<ArcGISIntegrationSummaryResponse> out = service.getAllByTenant("t");

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getNextRunAtUtc()).isNull();
    }

    @Test
    @DisplayName("getAllByTenant should propagate KW label lookup exceptions")
    void getAllByTenant_kwLabelException_propagates() {
        ArcGISIntegrationSummaryProjection projection = org.mockito.Mockito
                .mock(ArcGISIntegrationSummaryProjection.class);
        when(arcGISIntegrationRepository.findAllSummariesByTenantId("t"))
                .thenReturn(List.of(projection));

        ArcGISIntegrationSummaryResponse response = ArcGISIntegrationSummaryResponse.builder()
                .id(UUID.randomUUID()).itemSubtype("S").cronExpression("0 0 10 * * ?")
                .executionTime(LocalTime.of(10, 0)).isEnabled(true).build();
        when(arcGISIntegrationMapper.projectionToResponse(projection)).thenReturn(response);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any()))
                .thenThrow(new RuntimeException("KW down"));

        assertThatThrownBy(() -> service.getAllByTenant("t"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KW down");
    }

    @Test
    @DisplayName("getByIdAndTenantWithDetails should set nextRunAtUtc to null when disabled")
    void getByIdAndTenantWithDetails_disabled_noNextRun() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = buildExistingIntegration(id);
        integration.setIsEnabled(false);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));
        ArcGISIntegrationResponse response = ArcGISIntegrationResponse
                .builder()
                .id(id).itemSubtype("S")
                .schedule(IntegrationScheduleResponse
                        .builder()
                        .executionTime(LocalTime.of(10, 0)).build())
                .build();
        when(arcGISIntegrationMapper.toDetailsResponse(integration)).thenReturn(response);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any())).thenReturn(null);
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any())).thenReturn(null);

        var result = service.getByIdAndTenantWithDetails(id, "t1");

        assertThat(result.getNextRunAtUtc()).isNull();
    }

    @Test
    @DisplayName("delete should not update schedule when schedule is null")
    void delete_noSchedule_doesNotUpdateSchedule() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("n").itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID()).schedule(null).isEnabled(false).build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));

        service.delete(id, "t1", "u");

        verify(arcGISScheduleService, never()).updateSchedule(any());
        verify(arcGISIntegrationRepository).save(integration);
        assertThat(integration.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("delete should not unschedule job when schedule is null")
    void delete_noSchedule_doesNotUnscheduleJob() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("n").itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID()).schedule(null).isEnabled(false).build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));

        service.delete(id, "t1", "u");

        verify(arcGISScheduleService, never()).unscheduleJob(any());
        verify(arcGISScheduleService, never()).updateSchedule(any());
        verify(arcGISIntegrationRepository).save(integration);
        assertThat(integration.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("toggleActiveStatus should not schedule/unschedule when schedule is null")
    void toggleActiveStatus_nullSchedule_noScheduling() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("n").itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID()).schedule(null).isEnabled(false).build();
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));

        boolean result = service.toggleActiveStatus(id, "t1", "u");

        assertThat(result).isTrue();
        verify(arcGISScheduleService, never()).scheduleJob(any());
        verify(arcGISScheduleService, never()).unscheduleJob(any());
    }

    @Test
    @DisplayName("getAllArcGISNormalizedNamesByTenantId delegates to repository")
    void getAllArcGISNormalizedNamesByTenantId_delegates() {
        when(arcGISIntegrationRepository.findAllNormalizedNamesByTenantId("t1"))
                .thenReturn(List.of("int-a", "int-b"));

        List<String> result = service.getAllArcGISNormalizedNamesByTenantId("t1");

        assertThat(result).containsExactly("int-a", "int-b");
    }

    @Test
    @DisplayName("getFieldMappings delegates to repository and maps")
    void getFieldMappings_delegates() {
        UUID integrationId = UUID.randomUUID();
        IntegrationFieldMapping mapping = IntegrationFieldMapping.builder().id(UUID.randomUUID()).build();
        IntegrationFieldMappingDto dto = IntegrationFieldMappingDto.builder().build();
        when(integrationFieldMappingRepository.findByIntegrationIdAndTenantId(integrationId, "t1"))
                .thenReturn(List.of(mapping));
        when(integrationFieldMappingMapper.toDto(mapping)).thenReturn(dto);

        List<IntegrationFieldMappingDto> result = service.getFieldMappings(integrationId, "t1");

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    @DisplayName("getJobHistory delegates to repository and maps results")
    void getJobHistory_delegates() {
        UUID integrationId = UUID.randomUUID();
        com.integration.management.entity.IntegrationJobExecution exec = com.integration.management.entity.IntegrationJobExecution
                .builder()
                .id(UUID.randomUUID())
                .scheduleId(UUID.randomUUID())
                .build();
        com.integration.execution.contract.model.IntegrationJobExecutionDto dto = com.integration.execution.contract.model.IntegrationJobExecutionDto
                .builder().build();
        when(integrationJobExecutionRepository.findByIntegrationAndTenant(
                eq(integrationId), eq("t1"), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(exec));
        when(integrationJobExecutionMapper.toDto(exec)).thenReturn(dto);

        List<com.integration.execution.contract.model.IntegrationJobExecutionDto> result = service
                .getJobHistory(integrationId, "t1");

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    @DisplayName("update should keep dynamicDocumentType when subtype is DYNAMIC")
    void update_dynamicSubtype_keepsDynamicDocumentType() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration existing = buildExistingIntegration(id);
        existing.setItemSubtype("DOCUMENT_FINAL_DYNAMIC");
        existing.setDynamicDocumentType("DT");
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(existing));
        when(arcGISIntegrationRepository.save(any())).thenReturn(existing);

        ArcGISIntegrationCreateUpdateRequest request = buildCreateRequest("n");
        // keep subtype as DYNAMIC
        org.mockito.Mockito.doAnswer(inv -> {
            ArcGISIntegration entity = inv.getArgument(1);
            entity.setItemSubtype("DOCUMENT_FINAL_DYNAMIC");
            return null;
        }).when(arcGISIntegrationMapper).updateEntity(any(), any());
        org.mockito.Mockito.doNothing().when(integrationSchedulerMapper).updateEntity(any(), any());
        // cron stays same → no reschedule
        org.mockito.Mockito.doNothing().when(cronScheduleService).buildCron(any());

        when(arcGISIntegrationMapper.toCreationResponse(any()))
                .thenReturn(CreationResponse.builder().id(id.toString()).name("n").build());

        var result = service.update(id, request, "t1", "u");

        assertThat(result.getId()).isEqualTo(id.toString());
        // dynamicDocumentType must be preserved because subtype IS DYNAMIC
        assertThat(existing.getDynamicDocumentType()).isEqualTo("DT");
        // no schedule update because cron unchanged
        verify(arcGISScheduleService, never()).updateSchedule(any());
    }

    @Test
    @DisplayName("getArcGISIntegrationNameById throws IntegrationNotFoundException when not found")
    void getArcGISIntegrationNameById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(arcGISIntegrationRepository.findIntegrationNameById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getArcGISIntegrationNameById(id.toString()))
                .isInstanceOf(com.integration.management.exception.IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("update should throw IntegrationPersistenceException for unexpected exception")
    void update_unexpectedException_throwsPersistence() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration existing = buildExistingIntegration(id);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(existing));
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(arcGISIntegrationMapper).updateEntity(any(), any());

        assertThatThrownBy(() -> service.update(id, buildCreateRequest("n"), "t1", "u"))
                .isInstanceOf(com.integration.management.exception.IntegrationPersistenceException.class)
                .hasMessageContaining("Failed to update ArcGIS integration");
    }

    @Test
    @DisplayName("getByIdAndTenantWithDetails enriches labels and sets nextRunAtUtc for enabled integration")
    void getByIdAndTenantWithDetails_enabled_setsNextRun() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = buildExistingIntegration(id);
        integration.setIsEnabled(true);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));

        com.integration.management.model.dto.response.ArcGISIntegrationResponse response = ArcGISIntegrationResponse
                .builder()
                .id(id).itemSubtype("S")
                .schedule(IntegrationScheduleResponse
                        .builder()
                        .executionTime(LocalTime.of(10, 0)).build())
                .build();
        when(arcGISIntegrationMapper.toDetailsResponse(integration)).thenReturn(response);
        when(kwIntegrationService.getItemSubtypeDisplayValue(any())).thenReturn("Label");
        when(kwIntegrationService.getDynamicDocumentTypeDisplayValue(any(), any(), any())).thenReturn("Dyn");
        when(cronScheduleService.getNextRun(any(), any(), any()))
                .thenReturn(java.time.Instant.parse("2026-03-10T10:00:00Z"));

        var result = service.getByIdAndTenantWithDetails(id, "t1");

        assertThat(result.getItemSubtypeLabel()).isEqualTo("Label");
        assertThat(result.getDynamicDocumentTypeLabel()).isEqualTo("Dyn");
        assertThat(result.getNextRunAtUtc()).isNotNull();
    }

    @Test
    @DisplayName("delete should unschedule job when schedule exists")
    void delete_withSchedule_callsUnscheduleJob() {
        UUID id = UUID.randomUUID();
        ArcGISIntegration integration = buildExistingIntegration(id);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(integration));

        service.delete(id, "t1", "u");

        verify(arcGISScheduleService).unscheduleJob(integration);
        verify(arcGISScheduleService, never()).updateSchedule(any());
        verify(arcGISIntegrationRepository).save(integration);
        assertThat(integration.getIsDeleted()).isTrue();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ArcGISIntegrationCreateUpdateRequest buildCreateRequest(String name) {
        return ArcGISIntegrationCreateUpdateRequest.builder()
                .name(name).itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY).build())
                .fieldMappings(List.of()).build();
    }

    private ArcGISIntegration buildExistingIntegration(UUID id) {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID()).executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY).cronExpression("0 0 10 * * ?").build();
        return ArcGISIntegration.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("n").itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID()).schedule(schedule).isEnabled(true).build();
    }

    @Test
    @DisplayName("fetchArcGISFields should resolve secretName via IntegrationConnectionService")
    void fetchArcGISFields_resolvesSecretNameAndDelegatesToClient() {
        UUID connectionId = UUID.randomUUID();
        String tenantId = "tenant-1";
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), tenantId))
                .thenReturn("secret-123");

        List<ArcGISFieldDto> expected = List.of(new ArcGISFieldDto());
        when(iesArcGISApiClient.fetchArcGISFields("secret-123")).thenReturn(expected);

        List<ArcGISFieldDto> actual = service.fetchArcGISFields(connectionId, tenantId);

        assertThat(actual).isSameAs(expected);
        verify(iesArcGISApiClient).fetchArcGISFields("secret-123");
    }

    @Test
    @DisplayName("update with null fieldMappings skips syncIntegrationFieldMappings")
    void update_nullFieldMappings_skipsSync() {
        // Covers syncIntegrationFieldMappings: requestMappings == null → return early
        UUID id = UUID.randomUUID();
        ArcGISIntegration existing = buildExistingIntegration(id);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(existing));
        when(arcGISIntegrationRepository.save(any())).thenReturn(existing);

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("n").itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(java.time.LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY).build())
                .fieldMappings(null)  // null → syncIntegrationFieldMappings returns early
                .build();
        org.mockito.Mockito.doNothing().when(arcGISIntegrationMapper).updateEntity(any(), any());
        org.mockito.Mockito.doNothing().when(integrationSchedulerMapper).updateEntity(any(), any());

        service.update(id, request, "t1", "u1");

        verify(integrationFieldMappingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("update with empty fieldMappings skips syncIntegrationFieldMappings")
    void update_emptyFieldMappings_skipsSync() {
        // Covers syncIntegrationFieldMappings: requestMappings.isEmpty() → return early
        UUID id = UUID.randomUUID();
        ArcGISIntegration existing = buildExistingIntegration(id);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(existing));
        when(arcGISIntegrationRepository.save(any())).thenReturn(existing);

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("n").itemType("DOCUMENT").itemSubtype("S")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(java.time.LocalTime.of(10, 0))
                        .frequencyPattern(FrequencyPattern.DAILY).build())
                .fieldMappings(java.util.List.of())  // empty → syncIntegrationFieldMappings returns early
                .build();
        org.mockito.Mockito.doNothing().when(arcGISIntegrationMapper).updateEntity(any(), any());
        org.mockito.Mockito.doNothing().when(integrationSchedulerMapper).updateEntity(any(), any());

        service.update(id, request, "t1", "u1");

        verify(integrationFieldMappingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("isUniqueConstraintViolation: non-matching cause message returns false (throws IntegrationPersistenceException)")
    void isUniqueConstraintViolation_nonMatchingMessage_throwsPersistence() {
        // Covers isUniqueConstraintViolation: cause message exists but no keyword → returns false
        UUID id = UUID.randomUUID();
        ArcGISIntegration existing = buildExistingIntegration(id);
        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(existing));
        org.mockito.Mockito.doNothing().when(arcGISIntegrationMapper).updateEntity(any(), any());
        org.mockito.Mockito.doNothing().when(integrationSchedulerMapper).updateEntity(any(), any());
        when(arcGISIntegrationRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("violation",
                        new RuntimeException("foreign key constraint failed")));

        ArcGISIntegrationCreateUpdateRequest request = buildCreateRequest("test");

        assertThatThrownBy(() -> service.update(id, request, "t1", "u1"))
                .isInstanceOf(com.integration.management.exception.IntegrationPersistenceException.class)
                .hasMessageContaining("data integrity violation");
    }
}

