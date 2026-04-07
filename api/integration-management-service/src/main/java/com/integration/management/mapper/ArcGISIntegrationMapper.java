package com.integration.management.mapper;

import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.model.dto.request.ArcGISIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.response.ArcGISIntegrationResponse;
import com.integration.management.model.dto.response.ArcGISIntegrationSummaryResponse;
import com.integration.management.repository.projection.ArcGISIntegrationSummaryProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {IntegrationSchedulerMapper.class, IntegrationFieldMappingMapper.class})
public interface ArcGISIntegrationMapper extends NormalizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "normalizedName",
            expression = "java(normalize(request.getName()))"
    )
    ArcGISIntegration toEntity(ArcGISIntegrationCreateUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "normalizedName",
            expression = "java(normalize(request.getName()))"
    )
    void updateEntity(ArcGISIntegrationCreateUpdateRequest request, @MappingTarget ArcGISIntegration entity);

    ArcGISIntegrationResponse toResponse(ArcGISIntegration entity);

    ArcGISIntegrationResponse toDetailsResponse(ArcGISIntegration entity);

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    CreationResponse toCreationResponse(ArcGISIntegration entity);

    ArcGISIntegrationSummaryResponse projectionToResponse(ArcGISIntegrationSummaryProjection projection);

    @Named("joinList")
    default String joinList(List<String> values) {
        return values == null ? "" : String.join(",", values);
    }
}
