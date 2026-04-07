package com.integration.management.notification.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes platform notification events to the notification exchange.
 * IMS consumes these events via {@link NotificationListener} and dispatches them to SSE clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final MessagePublisher messagePublisher;

    public void publish(final NotificationEvent event) {
        log.debug("Publishing notification event: {} for tenant: {}",
                event.getEventKey(), event.getTenantId());
        messagePublisher.publish(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.NOTIFICATION_ROUTING_KEY,
                event);
    }
}
