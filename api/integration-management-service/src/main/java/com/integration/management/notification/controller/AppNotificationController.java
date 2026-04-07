package com.integration.management.notification.controller;

import com.integration.management.notification.model.dto.request.MarkNotificationsReadRequest;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationCountResponse;
import com.integration.execution.contract.model.enums.NotificationReadFilter;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.service.AppNotificationService;
import com.integration.management.notification.sse.SseEmitterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;

@Slf4j
@RestController
@RequestMapping("/api/management/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    private final AppNotificationService appNotificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @RequestAttribute(X_USER_ID) String userId,
            HttpServletResponse response) throws IOException {
        // Disable proxy buffering so nginx/caddy/ALB flush each SSE frame immediately.
        // Without these headers the proxy buffers the entire response until the
        // 30-minute emitter timeout fires, causing notifications to never arrive in
        // remote (proxy-fronted) environments even though the server sends them.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        log.info("Opening SSE stream for user: {}", userId);
        SseEmitter emitter = sseEmitterRegistry.register(userId);
        emitter.send(SseEmitter.event()
                .name("notification")
                .data("{\"type\":\"CONNECTED\"}"));
        return emitter;
    }

    @GetMapping
    public ResponseEntity<Page<AppNotificationResponse>> getNotifications(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @RequestParam(defaultValue = "ALL") NotificationReadFilter readFilter,
            @RequestParam(required = false) NotificationSeverity severity,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching notifications for tenant: {}, user: {}", tenantId, userId);
        return ResponseEntity.ok(
            appNotificationService.getNotifications(tenantId, userId, pageable, readFilter, severity));
    }

    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getNotificationCounts(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Fetching notification counts for tenant: {}, user: {}", tenantId, userId);
        return ResponseEntity.ok(appNotificationService.getNotificationCounts(tenantId, userId));
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @Valid @RequestBody MarkNotificationsReadRequest request) {
        log.info("Marking notifications as read for tenant: {}, user: {}", tenantId, userId);
        appNotificationService.markAsRead(tenantId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read/all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Marking all notifications as read for tenant: {}, user: {}", tenantId, userId);
        appNotificationService.markAllAsRead(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @PathVariable UUID id) {
        log.info("Deleting notification {} for tenant: {}, user: {}", id, tenantId, userId);
        appNotificationService.deleteById(tenantId, userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Deleting all notifications for tenant: {}, user: {}", tenantId, userId);
        appNotificationService.deleteAll(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
