package com.integration.management.mapper;

import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.model.dto.response.JiraWebhookEventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JiraWebhookEventMapper {
    JiraWebhookEventResponse toResponse(JiraWebhookEvent trigger);
}
