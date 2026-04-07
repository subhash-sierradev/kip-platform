package com.integration.management.notification.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    @DisplayName("publishes event to notification exchange with correct routing key")
    void publishes_to_notification_exchange() {
        NotificationEvent event = NotificationEvent.builder()
                .eventKey("SITE_CONFIG_UPDATED")
                .tenantId("tenant-1")
                .triggeredByUserId("user-1")
                .metadata(Map.of("key", "value"))
                .build();

        notificationEventPublisher.publish(event);

        verify(messagePublisher).publish(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.NOTIFICATION_ROUTING_KEY,
                event);
    }

    @Test
    @DisplayName("publishAfterCommit publishes immediately when no transaction is active")
    void publishAfterCommit_publishesImmediately_whenNoTransaction() {
        NotificationEvent event = NotificationEvent.builder()
                .eventKey("CONFLUENCE_INTEGRATION_ENABLED")
                .tenantId("tenant-1")
                .triggeredByUserId("user-1")
                .build();

        notificationEventPublisher.publishAfterCommit(event);

        verify(messagePublisher).publish(
                QueueNames.NOTIFICATION_EXCHANGE,
                QueueNames.NOTIFICATION_ROUTING_KEY,
                event);
    }

    @Test
    @DisplayName("publishAfterCommit registers afterCommit synchronization when transaction is active")
    void publishAfterCommit_defersToAfterCommit_whenTransactionActive() {
        NotificationEvent event = NotificationEvent.builder()
                .eventKey("CONFLUENCE_INTEGRATION_DISABLED")
                .tenantId("tenant-1")
                .triggeredByUserId("user-1")
                .build();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            notificationEventPublisher.publishAfterCommit(event);

            // Not published yet — waiting for afterCommit
            verifyNoInteractions(messagePublisher);

            // Simulate afterCommit
            List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
            assertThat(syncs).hasSize(1);
            syncs.getFirst().afterCommit();

            verify(messagePublisher).publish(
                    QueueNames.NOTIFICATION_EXCHANGE,
                    QueueNames.NOTIFICATION_ROUTING_KEY,
                    event);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
