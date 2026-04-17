package com.integration.management.notification.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventPublisher")
class NotificationEventPublisherTest {

    @Mock
    private MessagePublisher messagePublisher;

    private NotificationEventPublisher notificationEventPublisher;

    @BeforeEach
    void setUp() {
        notificationEventPublisher = new NotificationEventPublisher(messagePublisher);
    }

    private NotificationEvent buildEvent() {
        return NotificationEvent.builder()
                .eventKey("SITE_CONFIG_UPDATED")
                .tenantId("tenant-1")
                .triggeredByUserId("user-1")
                .metadata(Map.of("key", "value"))
                .build();
    }

    @Test
    @DisplayName("publish sends event to notification exchange with correct routing key")
    void publish_sendsToNotificationExchange() {
        NotificationEvent event = buildEvent();

        notificationEventPublisher.publish(event);

        verify(messagePublisher).publish(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.NOTIFICATION_ROUTING_KEY,
                event);
    }

    @Nested
    @DisplayName("publishAfterCommit")
    class PublishAfterCommit {

        @Test
        @DisplayName("publishes immediately when no active transaction")
        void publishesImmediately_whenNoActiveTransaction() {
            NotificationEvent event = buildEvent();

            // No transaction active (default test context)
            notificationEventPublisher.publishAfterCommit(event);

            verify(messagePublisher).publish(
                    QueueNames.NOTIFICATION_EXCHANGE,
                    QueueNames.NOTIFICATION_ROUTING_KEY,
                    event);
        }

        @Test
        @DisplayName("defers publishing until after transaction commit")
        void defersPublishing_untilAfterCommit() {
            NotificationEvent event = buildEvent();

            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                notificationEventPublisher.publishAfterCommit(event);
                // Not published yet — still inside transaction
                verify(messagePublisher, never()).publish(
                        QueueNames.NOTIFICATION_EXCHANGE,
                        QueueNames.NOTIFICATION_ROUTING_KEY,
                        event);

                // Simulate afterCommit
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(sync -> sync.afterCommit());

                // Now it should be published
                verify(messagePublisher, times(1)).publish(
                        QueueNames.NOTIFICATION_EXCHANGE,
                        QueueNames.NOTIFICATION_ROUTING_KEY,
                        event);
            } finally {
                TransactionSynchronizationManager.setActualTransactionActive(false);
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("safePublish swallows exception and does not rethrow")
        void safePublish_swallowsException() {
            NotificationEvent event = buildEvent();
            doThrow(new RuntimeException("broker down"))
                    .when(messagePublisher).publish(
                            QueueNames.NOTIFICATION_EXCHANGE,
                            QueueNames.NOTIFICATION_ROUTING_KEY,
                            event);

            // publishAfterCommit outside transaction calls safePublish — must not throw
            assertThatNoException().isThrownBy(() ->
                    notificationEventPublisher.publishAfterCommit(event));
        }
    }
}
