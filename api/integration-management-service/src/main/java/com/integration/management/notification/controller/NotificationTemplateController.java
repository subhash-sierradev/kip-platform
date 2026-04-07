package com.integration.management.notification.controller;

import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;

@Slf4j
@RestController
@RequestMapping("/api/management/notifications/templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> getTemplates(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching notification templates for tenant: {}", tenantId);
        return ResponseEntity.ok(notificationTemplateService.getTemplatesForTenant(tenantId));
    }
}
