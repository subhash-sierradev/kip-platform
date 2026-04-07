package com.integration.management.notification.aop;

import com.integration.execution.contract.model.enums.NotificationEventKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Publishes a {@code NotificationEvent} via RabbitMQ after a method returns successfully.
 *
 * <p>Two metadata strategies (mutually exclusive):
 * <ul>
 *   <li><b>Inline SpEL</b> — use {@link #metadata()} referencing {@code #result} for methods that
 *       return the entity. Example:
 *       {@code metadata = "{'integrationName': #result.name}"}</li>
 *   <li><b>Provider</b> — use {@link #metadataProvider()} + {@link #entityId()} for void methods
 *       (e.g. delete). The aspect calls the provider bean <em>before</em> the method proceeds so
 *       the entity is still in the database. Example:
 *       {@code metadataProvider = "arcGISNotificationMetadataProvider", entityId = "#id"}</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishNotification {

    /** Maps directly to a shared notification event key. */
    NotificationEventKey eventKey();

    /** SpEL expression resolving to the tenant ID string. Default: {@code #tenantId}. */
    String tenantId() default "#tenantId";

    /** SpEL expression resolving to the triggered-by user ID string. Default: {@code #userId}. */
    String userId() default "#userId";

    /**
     * Inline SpEL expression evaluating to {@code Map<String, Object>} metadata.
     * Use when the method returns the entity and {@code #result} is available.
     * Leave empty when using {@link #metadataProvider()}.
     */
    String metadata() default "";

    /**
     * Spring bean name of a {@link NotificationMetadataProvider} called <em>before</em> proceed.
     * Use for void methods where the entity must be fetched before it is modified/deleted.
     * Leave empty when using inline {@link #metadata()}.
     */
    String metadataProvider() default "";

    /**
     * SpEL expression resolving to the entity ID passed to the provider.
     * Required when {@link #metadataProvider()} is set. Example: {@code "#id"}.
     */
    String entityId() default "";
}
