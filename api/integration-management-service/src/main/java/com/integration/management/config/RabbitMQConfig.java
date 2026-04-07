package com.integration.management.config;

import com.integration.execution.contract.queue.QueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for IMS.
 *
 * <p>IMS publishes {@code ArcGISExecutionCommand} and {@code JiraWebhookExecutionCommand} messages
 * to the respective command queues and listens on the result queues for completion messages from IES.
 *
 * <p>Active only when {@code app.messaging.transport=rabbitmq}.
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMQConfig {

    // --- ArcGIS ---

    @Bean
    public DirectExchange arcgisExchange() {
        return new DirectExchange(QueueNames.ARCGIS_EXCHANGE, true, false);
    }

    @Bean
    public Queue arcgisExecutionCommandQueue() {
        return new Queue(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue arcgisExecutionResultQueue() {
        return new Queue(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE, true);
    }

    @Bean
    public Binding commandQueueBinding(Queue arcgisExecutionCommandQueue, DirectExchange arcgisExchange) {
        return BindingBuilder.bind(arcgisExecutionCommandQueue)
                .to(arcgisExchange)
                .with(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE);
    }

    @Bean
    public Binding resultQueueBinding(Queue arcgisExecutionResultQueue, DirectExchange arcgisExchange) {
        return BindingBuilder.bind(arcgisExecutionResultQueue)
                .to(arcgisExchange)
                .with(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
    }

    // --- Jira Webhook ---

    @Bean
    public DirectExchange jiraWebhookExchange() {
        return new DirectExchange(QueueNames.JIRA_WEBHOOK_EXCHANGE, true, false);
    }

    @Bean
    public Queue jiraWebhookExecutionCommandQueue() {
        return new Queue(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue jiraWebhookExecutionResultQueue() {
        return new Queue(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE, true);
    }

    @Bean
    public Binding jiraCommandQueueBinding(
            Queue jiraWebhookExecutionCommandQueue, DirectExchange jiraWebhookExchange) {
        return BindingBuilder.bind(jiraWebhookExecutionCommandQueue)
                .to(jiraWebhookExchange)
                .with(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE);
    }

    @Bean
    public Binding jiraResultQueueBinding(
            Queue jiraWebhookExecutionResultQueue, DirectExchange jiraWebhookExchange) {
        return BindingBuilder.bind(jiraWebhookExecutionResultQueue)
                .to(jiraWebhookExchange)
                .with(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
    }

    // --- Confluence ---

    @Bean
    public DirectExchange confluenceExchange() {
        return new DirectExchange(QueueNames.CONFLUENCE_EXCHANGE, true, false);
    }

    @Bean
    public Queue confluenceExecutionCommandQueue() {
        return new Queue(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue confluenceExecutionResultQueue() {
        return new Queue(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE, true);
    }

    @Bean
    public Binding confluenceCommandQueueBinding(
            Queue confluenceExecutionCommandQueue, DirectExchange confluenceExchange) {
        return BindingBuilder.bind(confluenceExecutionCommandQueue)
                .to(confluenceExchange)
                .with(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE);
    }

    @Bean
    public Binding confluenceResultQueueBinding(
            Queue confluenceExecutionResultQueue, DirectExchange confluenceExchange) {
        return BindingBuilder.bind(confluenceExecutionResultQueue)
                .to(confluenceExchange)
                .with(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
    }

    // --- Shared infrastructure ---

    // --- Notifications ---

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(QueueNames.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueueIms() {
        return new Queue(QueueNames.NOTIFICATION_QUEUE_IMS, true);
    }

    @Bean
    public Binding notificationQueueBinding(
            Queue notificationQueueIms, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueueIms)
                .to(notificationExchange)
                .with(QueueNames.NOTIFICATION_ROUTING_KEY);
    }

    // --- Shared infrastructure ---

    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        return template;
    }
}
