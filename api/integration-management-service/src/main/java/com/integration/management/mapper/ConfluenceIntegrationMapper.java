package com.integration.management.mapper;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.Language;
import com.integration.management.model.dto.request.ConfluenceIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.response.ConfluenceIntegrationResponse;
import com.integration.management.model.dto.response.ConfluenceIntegrationSummaryResponse;
import com.integration.management.repository.projection.ConfluenceIntegrationSummaryProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring", uses = IntegrationSchedulerMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConfluenceIntegrationMapper extends NormalizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "normalizedName", expression = "java(normalize(request.getName()))")
    @Mapping(target = "includeTableOfContents",
            expression = "java(Boolean.TRUE.equals(request.getIncludeTableOfContents()))")
    @Mapping(target = "languages", ignore = true)
    @Mapping(target = "schedule", ignore = true)
    @Mapping(target = "confluenceSpaceKeyFolderKey",
            expression = "java(request.getConfluenceSpaceKeyFolderKey() != null"
                    + " && !request.getConfluenceSpaceKeyFolderKey().isBlank()"
                    + " ? request.getConfluenceSpaceKeyFolderKey()"
                    + " : com.integration.management.constants.IntegrationManagementConstants.ROOT_FOLDER_KEY)")
    ConfluenceIntegration toEntity(ConfluenceIntegrationCreateUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "normalizedName", expression = "java(normalize(request.getName()))")
    @Mapping(target = "includeTableOfContents",
            expression = "java(Boolean.TRUE.equals(request.getIncludeTableOfContents()))")
    @Mapping(target = "languages", ignore = true)
    @Mapping(target = "schedule", ignore = true)
    @Mapping(target = "confluenceSpaceKeyFolderKey",
            expression = "java(request.getConfluenceSpaceKeyFolderKey() != null"
                    + " && !request.getConfluenceSpaceKeyFolderKey().isBlank()"
                    + " ? request.getConfluenceSpaceKeyFolderKey()"
                    + " : com.integration.management.constants.IntegrationManagementConstants.ROOT_FOLDER_KEY)")
    void updateEntity(ConfluenceIntegrationCreateUpdateRequest request, @MappingTarget ConfluenceIntegration entity);

    @Mapping(target = "itemType", source = "documentItemType")
    @Mapping(target = "itemSubtype", source = "documentItemSubtype")
    @Mapping(target = "connectionId",
            expression = "java(entity.getConnectionId() != null ? entity.getConnectionId().toString() : null)")
    @Mapping(target = "languageCodes", expression = "java(toLanguageCodes(entity.getLanguages()))")
    @Mapping(target = "itemSubtypeLabel", ignore = true)
    @Mapping(target = "dynamicDocumentTypeLabel", ignore = true)
    @Mapping(target = "nextRunAtUtc", ignore = true)
    ConfluenceIntegrationResponse toDetailsResponse(ConfluenceIntegration entity);

    @Mapping(target = "id",
            expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    CreationResponse toCreationResponse(ConfluenceIntegration entity);

    @Mapping(target = "itemType", source = "documentItemType")
    @Mapping(target = "itemSubtype", source = "documentItemSubtype")
    @Mapping(target = "itemSubtypeLabel", ignore = true)
    @Mapping(target = "dynamicDocumentTypeLabel", ignore = true)
    @Mapping(target = "languageCodes", ignore = true)
    @Mapping(target = "nextRunAtUtc", ignore = true)
    ConfluenceIntegrationSummaryResponse projectionToSummaryResponse(
            ConfluenceIntegrationSummaryProjection projection);

    @Mapping(target = "integrationId", source = "integration.id")
    @Mapping(target = "jobExecutionId", source = "jobExecution.id")
    @Mapping(target = "integrationName", source = "integration.name")
    @Mapping(target = "itemType", source = "integration.documentItemType")
    @Mapping(target = "itemSubtype", source = "integration.documentItemSubtype")
    @Mapping(target = "dynamicDocumentType", source = "integration.dynamicDocumentType")
    @Mapping(target = "languageCodes", expression = "java(toLanguageCodes(integration.getLanguages()))")
    @Mapping(target = "confluenceSpaceKey", source = "integration.confluenceSpaceKey")
    @Mapping(target = "confluenceSpaceKeyFolderKey", source = "integration.confluenceSpaceKeyFolderKey")
    @Mapping(target = "reportNameTemplate", source = "integration.reportNameTemplate")
    @Mapping(target = "includeTableOfContents",
            expression = "java(Boolean.TRUE.equals(integration.getIncludeTableOfContents()))")
    @Mapping(target = "businessTimezone", source = "integration.schedule.businessTimeZone")
    ConfluenceExecutionCommand toExecutionCommand(
            ConfluenceIntegration integration,
            IntegrationJobExecution jobExecution,
            String connectionSecretName,
            String tenantId,
            TriggerType triggeredBy,
            String triggeredByUser,
            Instant windowStart,
            Instant windowEnd);

    default List<String> toLanguageCodes(final List<Language> languages) {
        if (languages == null) {
            return List.of();
        }
        return languages.stream().map(Language::getCode).toList();
    }
}
