package com.integration.execution.contract.queue;

/**
 * RabbitMQ queue and exchange name constants shared between IMS and IES.
 */
public final class QueueNames {

    public static final String ARCGIS_EXCHANGE = "integration.arcgis.exchange";
    public static final String ARCGIS_EXECUTION_COMMAND_QUEUE = "integration.arcgis.execution.command";
    public static final String ARCGIS_EXECUTION_RESULT_QUEUE = "integration.arcgis.execution.result";

    public static final String JIRA_WEBHOOK_EXCHANGE = "integration.jira.exchange";
    public static final String JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE = "integration.jira.execution.command";
    public static final String JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE = "integration.jira.execution.result";

    public static final String CONFLUENCE_EXCHANGE = "integration.confluence.exchange";
    public static final String CONFLUENCE_EXECUTION_COMMAND_QUEUE = "integration.confluence.execution.command";
    public static final String CONFLUENCE_EXECUTION_RESULT_QUEUE = "integration.confluence.execution.result";

    // --- Notifications (used by both IMS and IES) ---
    public static final String NOTIFICATION_EXCHANGE = "integration.notification.exchange";
    public static final String NOTIFICATION_QUEUE_IMS = "integration.notification.ims";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.event";

    private QueueNames() {
        throw new UnsupportedOperationException("Utility class");
    }
}
