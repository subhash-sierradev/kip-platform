package com.integration.management.notification.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.management.notification.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumes notification events from the notification queue and delegates to
 * {@link NotificationDispatchService} to persist and push SSE to connected clients.
 * This is the single entry-point for all platform events — whether originating in IMS or IES.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationDispatchService notificationDispatchService;

    public void handleNotificationEvent(final NotificationEvent event) {
        log.info("Received notification event: {} for tenant: {}",
                event.getEventKey(), event.getTenantId());
        notificationDispatchService.dispatch(
                event.getEventKey(),
                event.getTenantId(),
                event.getTriggeredByUserId(),
                event.getMetadata());
    }
}
