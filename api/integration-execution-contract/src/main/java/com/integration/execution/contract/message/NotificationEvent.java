package com.integration.execution.contract.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Message published to the notification exchange whenever a platform event occurs.
 * Both IMS and IES publish this message; IMS consumes it via the notification queue
 * and fans out to connected SSE clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    /** Maps directly to a {@code NotificationEventKey} enum name. */
    private String eventKey;
    private String tenantId;
    private String triggeredByUserId;
    private Map<String, Object> metadata;
}
