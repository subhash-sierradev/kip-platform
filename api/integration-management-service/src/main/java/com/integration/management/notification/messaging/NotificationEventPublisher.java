package com.integration.management.notification.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    /**
     * Defers publishing until after the surrounding transaction commits.
     * If no transaction is active, publishes immediately.
     * Prevents orphaned notifications when the DB transaction is rolled back.
     */
    public void publishAfterCommit(final NotificationEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safePublish(event);
                }
            });
        } else {
            safePublish(event);
        }
    }

    private void safePublish(final NotificationEvent event) {
        try {
            publish(event);
        } catch (Exception ex) {
            log.error("Failed to publish notification event '{}': {}",
                    event.getEventKey(), ex.getMessage(), ex);
        }
    }
}
