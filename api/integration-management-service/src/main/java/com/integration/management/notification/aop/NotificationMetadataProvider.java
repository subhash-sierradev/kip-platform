package com.integration.management.notification.aop;

import java.util.Map;

/**
 * Strategy for resolving notification metadata by fetching entity details from the database.
 * Implementations are Spring beans registered by name in {@link PublishNotification#metadataProvider()}.
 * The aspect calls {@link #resolve} BEFORE the advised method proceeds so the entity still exists.
 */
public interface NotificationMetadataProvider {

    Map<String, Object> resolve(Object entityId, String tenantId);
}
