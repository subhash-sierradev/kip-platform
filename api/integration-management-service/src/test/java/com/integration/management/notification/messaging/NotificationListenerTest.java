package com.integration.management.notification.messaging;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.management.notification.service.NotificationDispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationListener")
class NotificationListenerTest {

    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    @DisplayName("delegates to dispatch service with all event fields")
    void delegates_to_dispatch_service() {
        Map<String, Object> metadata = Map.of("integrationName", "My Map");
        NotificationEvent event = NotificationEvent.builder()
                .eventKey("ARCGIS_INTEGRATION_CREATED")
                .tenantId("tenant-1")
                .triggeredByUserId("user-1")
                .metadata(metadata)
                .build();

        notificationListener.handleNotificationEvent(event);

        verify(notificationDispatchService).dispatch(
                "ARCGIS_INTEGRATION_CREATED", "tenant-1", "user-1", metadata);
    }

    @Test
    @DisplayName("delegates to dispatch service when metadata is null")
    void delegates_when_metadata_null() {
        NotificationEvent event = NotificationEvent.builder()
                .eventKey("SITE_CONFIG_UPDATED")
                .tenantId("tenant-1")
                .triggeredByUserId(null)
                .metadata(null)
                .build();

        notificationListener.handleNotificationEvent(event);

        verify(notificationDispatchService).dispatch("SITE_CONFIG_UPDATED", "tenant-1", null, null);
    }
}
