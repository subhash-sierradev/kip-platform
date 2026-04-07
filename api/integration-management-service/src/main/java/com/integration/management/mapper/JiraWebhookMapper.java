package com.integration.management.mapper;

import com.integration.management.entity.JiraWebhook;
import com.integration.management.model.dto.request.JiraWebhookCreateUpdateRequest;
import com.integration.management.model.dto.response.JiraWebhookDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JiraWebhookMapper extends NormalizationMapper {
    @Mapping(
            target = "normalizedName",
            expression = "java(normalize(request.getName()))"
    )
    JiraWebhook toEntity(JiraWebhookCreateUpdateRequest request);

    JiraWebhookDetailResponse toResponse(JiraWebhook webhook);

    @Mapping(
            target = "normalizedName",
            expression = "java(normalize(request.getName()))"
    )
    void updateEntity(@MappingTarget JiraWebhook existingWebhook, JiraWebhookCreateUpdateRequest request);
}
