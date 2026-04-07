package com.integration.management.notification.controller;

import com.integration.management.notification.model.dto.response.NotificationEventCatalogResponse;
import com.integration.management.notification.service.NotificationEventCatalogService;
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
@RequestMapping("/api/management/notifications/events")
@RequiredArgsConstructor
public class NotificationEventCatalogController {

    private final NotificationEventCatalogService notificationEventCatalogService;

    @GetMapping
    public ResponseEntity<List<NotificationEventCatalogResponse>> getAllEnabled(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all notification event catalog entries for tenant: {}", tenantId);
        return ResponseEntity.ok(notificationEventCatalogService.getAllEnabled(tenantId));
    }
}
