package com.integration.management.notification.mapper;

import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.management.notification.entity.NotificationTemplate;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationEventCatalogResponse;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationEventCatalogResponse toEventCatalogResponse(NotificationEventCatalog entity);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventKey", source = "event.eventKey")
    @Mapping(target = "entityType", source = "event.entityType")
    NotificationRuleResponse toRuleResponse(NotificationRule entity);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventKey", source = "event.eventKey")
    NotificationTemplateResponse toTemplateResponse(NotificationTemplate entity);

    AppNotificationResponse toAppNotificationResponse(AppNotification entity);

    @Mapping(target = "ruleId", source = "rule.id")
    @Mapping(target = "eventKey", source = "rule.event.eventKey")
    @Mapping(target = "recipientType", expression = "java(entity.getRecipientType().name())")
    RecipientPolicyResponse toRecipientPolicyResponse(NotificationRecipientPolicy entity);
}
