package com.integration.execution.config;

import com.integration.execution.contract.queue.QueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ messaging configuration.
 * <p>
 * Defines all RabbitMQ exchanges, queues, bindings, and message converters.
 * Active only when {@code app.messaging.transport=rabbitmq}.
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMQConfig {

    @Bean
    public DirectExchange arcGISExchange() {
        return new DirectExchange(QueueNames.ARCGIS_EXCHANGE, true, false);
    }

    @Bean
    public Queue arcGISExecutionCommandQueue() {
        return QueueBuilder.durable(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE).build();
    }

    @Bean
    public Queue arcGISExecutionResultQueue() {
        return QueueBuilder.durable(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE).build();
    }

    @Bean
    public Binding arcGISExecutionCommandBinding(
            final Queue arcGISExecutionCommandQueue,
            final DirectExchange arcGISExchange) {
        return BindingBuilder.bind(arcGISExecutionCommandQueue)
                .to(arcGISExchange)
                .with(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE);
    }

    @Bean
    public Binding arcGISExecutionResultBinding(
            final Queue arcGISExecutionResultQueue,
            final DirectExchange arcGISExchange) {
        return BindingBuilder.bind(arcGISExecutionResultQueue)
                .to(arcGISExchange)
                .with(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
    }

    @Bean
    public DirectExchange jiraWebhookExchange() {
        return new DirectExchange(QueueNames.JIRA_WEBHOOK_EXCHANGE, true, false);
    }

    @Bean
    public Queue jiraWebhookExecutionCommandQueue() {
        return QueueBuilder.durable(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE).build();
    }

    @Bean
    public Queue jiraWebhookExecutionResultQueue() {
        return QueueBuilder.durable(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE).build();
    }

    @Bean
    public Binding jiraWebhookExecutionCommandBinding(
            final Queue jiraWebhookExecutionCommandQueue,
            final DirectExchange jiraWebhookExchange) {
        return BindingBuilder.bind(jiraWebhookExecutionCommandQueue)
                .to(jiraWebhookExchange)
                .with(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE);
    }

    @Bean
    public Binding jiraWebhookExecutionResultBinding(
            final Queue jiraWebhookExecutionResultQueue,
            final DirectExchange jiraWebhookExchange) {
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
        return QueueBuilder.durable(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE).build();
    }

    @Bean
    public Queue confluenceExecutionResultQueue() {
        return QueueBuilder.durable(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE).build();
    }

    @Bean
    public Binding confluenceExecutionCommandBinding(
            final Queue confluenceExecutionCommandQueue,
            final DirectExchange confluenceExchange) {
        return BindingBuilder.bind(confluenceExecutionCommandQueue)
                .to(confluenceExchange)
                .with(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE);
    }

    @Bean
    public Binding confluenceExecutionResultBinding(
            final Queue confluenceExecutionResultQueue,
            final DirectExchange confluenceExchange) {
        return BindingBuilder.bind(confluenceExecutionResultQueue)
                .to(confluenceExchange)
                .with(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            final ConnectionFactory connectionFactory,
            final MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            final ConnectionFactory connectionFactory,
            final MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
