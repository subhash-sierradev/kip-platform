package com.integration.execution.config;

import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void arcGISExchange_hasCorrectNameAndProperties() {
        DirectExchange exchange = config.arcGISExchange();

        assertThat(exchange.getName()).isEqualTo(QueueNames.ARCGIS_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void arcGISQueues_areNamedAndDurable() {
        Queue commandQueue = config.arcGISExecutionCommandQueue();
        Queue resultQueue = config.arcGISExecutionResultQueue();

        assertThat(commandQueue.getName()).isEqualTo(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE);
        assertThat(commandQueue.isDurable()).isTrue();
        assertThat(resultQueue.getName()).isEqualTo(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
        assertThat(resultQueue.isDurable()).isTrue();
    }

    @Test
    void arcGISBindings_haveCorrectRoutingKeys() {
        DirectExchange exchange = config.arcGISExchange();
        Queue commandQueue = config.arcGISExecutionCommandQueue();
        Queue resultQueue = config.arcGISExecutionResultQueue();

        Binding commandBinding = config.arcGISExecutionCommandBinding(commandQueue, exchange);
        Binding resultBinding = config.arcGISExecutionResultBinding(resultQueue, exchange);

        assertThat(commandBinding.getRoutingKey()).isEqualTo(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE);
        assertThat(resultBinding.getRoutingKey()).isEqualTo(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
    }

    @Test
    void jiraWebhookExchange_hasCorrectNameAndProperties() {
        DirectExchange exchange = config.jiraWebhookExchange();

        assertThat(exchange.getName()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void jiraWebhookQueues_areNamedAndDurable() {
        Queue commandQueue = config.jiraWebhookExecutionCommandQueue();
        Queue resultQueue = config.jiraWebhookExecutionResultQueue();

        assertThat(commandQueue.getName()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE);
        assertThat(commandQueue.isDurable()).isTrue();
        assertThat(resultQueue.getName()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
        assertThat(resultQueue.isDurable()).isTrue();
    }

    @Test
    void jiraWebhookBindings_haveCorrectRoutingKeys() {
        DirectExchange exchange = config.jiraWebhookExchange();
        Queue commandQueue = config.jiraWebhookExecutionCommandQueue();
        Queue resultQueue = config.jiraWebhookExecutionResultQueue();

        Binding commandBinding = config.jiraWebhookExecutionCommandBinding(commandQueue, exchange);
        Binding resultBinding = config.jiraWebhookExecutionResultBinding(resultQueue, exchange);

        assertThat(commandBinding.getRoutingKey()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE);
        assertThat(resultBinding.getRoutingKey()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
    }

    @Test
    void messageConverter_returnsJacksonJsonMessageConverter() {
        MessageConverter converter = config.messageConverter();

        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    void rabbitTemplate_usesProvidedConverterAndConnectionFactory() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter converter = config.messageConverter();

        RabbitTemplate template = config.rabbitTemplate(connectionFactory, converter);

        assertThat(template).isNotNull();
        assertThat(template.getMessageConverter()).isEqualTo(converter);
    }

    @Test
    void rabbitListenerContainerFactory_hasConverterAndConnectionFactory() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter converter = config.messageConverter();

        SimpleRabbitListenerContainerFactory factory =
                config.rabbitListenerContainerFactory(connectionFactory, converter);

        assertThat(factory).isNotNull();
    }

    @Test
    void confluenceExchange_hasCorrectNameAndProperties() {
        DirectExchange exchange = config.confluenceExchange();

        assertThat(exchange.getName()).isEqualTo(QueueNames.CONFLUENCE_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void confluenceQueues_areNamedAndDurable() {
        Queue commandQueue = config.confluenceExecutionCommandQueue();
        Queue resultQueue = config.confluenceExecutionResultQueue();

        assertThat(commandQueue.getName()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE);
        assertThat(commandQueue.isDurable()).isTrue();
        assertThat(resultQueue.getName()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
        assertThat(resultQueue.isDurable()).isTrue();
    }

    @Test
    void confluenceBindings_haveCorrectRoutingKeys() {
        DirectExchange exchange = config.confluenceExchange();
        Queue commandQueue = config.confluenceExecutionCommandQueue();
        Queue resultQueue = config.confluenceExecutionResultQueue();

        Binding commandBinding = config.confluenceExecutionCommandBinding(commandQueue, exchange);
        Binding resultBinding = config.confluenceExecutionResultBinding(resultQueue, exchange);

        assertThat(commandBinding.getRoutingKey()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE);
        assertThat(resultBinding.getRoutingKey()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
    }
}

