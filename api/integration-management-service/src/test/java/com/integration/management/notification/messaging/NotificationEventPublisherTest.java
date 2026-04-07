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

import java.util.Map;

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
}
