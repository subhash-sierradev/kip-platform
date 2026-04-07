package com.integration.management.mapper;

import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.IntegrationJobExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationJobExecutionMapper")
class IntegrationJobExecutionMapperTest {

    @Test
    @DisplayName("toDto should map basic execution fields")
    void toDto_mapsFields() {
        IntegrationJobExecutionMapper mapper = new IntegrationJobExecutionMapperImpl();

        UUID id = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        IntegrationJobExecution entity = IntegrationJobExecution.builder()
                .id(id)
                .scheduleId(scheduleId)
                .triggeredBy(TriggerType.API)
                .status(JobExecutionStatus.SUCCESS)
                .startedAt(Instant.parse("2026-03-03T00:00:00Z"))
                .completedAt(Instant.parse("2026-03-03T00:10:00Z"))
                .addedRecords(1)
                .updatedRecords(2)
                .failedRecords(3)
                .totalRecords(6)
                .errorMessage("err")
                .triggeredByUser("u1")
                .build();

        IntegrationJobExecutionDto dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getScheduleId()).isEqualTo(scheduleId);
        assertThat(dto.getTriggeredBy()).isEqualTo(TriggerType.API);
        assertThat(dto.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(dto.getTotalRecords()).isEqualTo(6);
        assertThat(dto.getErrorMessage()).isEqualTo("err");
        assertThat(dto.getTriggeredByUser()).isEqualTo("u1");
    }

    @Test
    @DisplayName("toDto returns null for null input")
    void toDto_null_returnsNull() {
        IntegrationJobExecutionMapper mapper = new IntegrationJobExecutionMapperImpl();
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("toDto maps metadata list branches")
    void toDto_mapsMetadataListBranches() {
        IntegrationJobExecutionMapper mapper = new IntegrationJobExecutionMapperImpl();

        List<RecordMetadata> addedMetadata = new ArrayList<>();
        addedMetadata.add(new RecordMetadata("d1", "Doc", "l1", 1L, 2L, 3L, 4L));
        addedMetadata.add(null);

        IntegrationJobExecution entity = IntegrationJobExecution.builder()
                .id(UUID.randomUUID())
                .scheduleId(UUID.randomUUID())
                .triggeredBy(TriggerType.API)
                .status(JobExecutionStatus.SUCCESS)
                .startedAt(Instant.parse("2026-03-03T00:00:00Z"))
                .addedRecordsMetadata(addedMetadata)
                .updatedRecordsMetadata(null)
                .failedRecordsMetadata(List.of(
                        new FailedRecordMetadata("d2", "Doc2", "l2", null, null, null, null, "boom")
                ))
                .totalRecordsMetadata(List.of(
                        new RecordMetadata("d3", "Doc3", "l3", null, null, null, null)
                ))
                .build();

        IntegrationJobExecutionDto dto = mapper.toDto(entity);
        assertThat(dto.getAddedRecordsMetadata()).hasSize(2);
        assertThat(dto.getAddedRecordsMetadata().get(0).documentId()).isEqualTo("d1");
        assertThat(dto.getAddedRecordsMetadata().get(1)).isNull();
        assertThat(dto.getUpdatedRecordsMetadata()).isNull();
        assertThat(dto.getFailedRecordsMetadata()).hasSize(1);
        assertThat(dto.getFailedRecordsMetadata().getFirst().errorMessage()).isEqualTo("boom");
        assertThat(dto.getTotalRecordsMetadata()).hasSize(1);
    }
}
