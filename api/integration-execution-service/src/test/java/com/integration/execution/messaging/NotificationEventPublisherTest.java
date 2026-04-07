package com.integration.execution.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Test
    void publish_sendsEventToNotificationExchange() {
        NotificationEventPublisher publisher = new NotificationEventPublisher(messagePublisher);
        NotificationEvent event = NotificationEvent.builder()
                .eventKey("ARCGIS_INTEGRATION_JOB_COMPLETED")
                .tenantId("tenant-1")
                .triggeredByUserId("user-1")
                .build();

        publisher.publish(event);

        verify(messagePublisher).publish(
                eq(QueueNames.NOTIFICATION_EXCHANGE),
                eq(QueueNames.NOTIFICATION_ROUTING_KEY),
                eq(event)
        );
    }
}
