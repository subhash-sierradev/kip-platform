package com.integration.management.config;

import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("RabbitMQConfig")
class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    @DisplayName("creates expected exchanges and queues")
    void createsExpectedExchangesAndQueues() {
        DirectExchange arcgisExchange = config.arcgisExchange();
        DirectExchange jiraExchange = config.jiraWebhookExchange();
        DirectExchange confluenceExchange = config.confluenceExchange();
        TopicExchange notificationExchange = config.notificationExchange();

        Queue arcgisCommand = config.arcgisExecutionCommandQueue();
        Queue arcgisResult = config.arcgisExecutionResultQueue();
        Queue jiraCommand = config.jiraWebhookExecutionCommandQueue();
        Queue jiraResult = config.jiraWebhookExecutionResultQueue();
        Queue confluenceCommand = config.confluenceExecutionCommandQueue();
        Queue confluenceResult = config.confluenceExecutionResultQueue();
        Queue notificationQueue = config.notificationQueueIms();

        assertThat(arcgisExchange.getName()).isEqualTo(QueueNames.ARCGIS_EXCHANGE);
        assertThat(jiraExchange.getName()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXCHANGE);
        assertThat(confluenceExchange.getName()).isEqualTo(QueueNames.CONFLUENCE_EXCHANGE);
        assertThat(notificationExchange.getName()).isEqualTo(QueueNames.NOTIFICATION_EXCHANGE);

        assertThat(arcgisCommand.getName()).isEqualTo(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE);
        assertThat(arcgisResult.getName()).isEqualTo(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
        assertThat(jiraCommand.getName()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE);
        assertThat(jiraResult.getName()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
        assertThat(confluenceCommand.getName()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE);
        assertThat(confluenceResult.getName()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
        assertThat(notificationQueue.getName()).isEqualTo(QueueNames.NOTIFICATION_QUEUE_IMS);

        assertThat(arcgisCommand.isDurable()).isTrue();
        assertThat(jiraCommand.isDurable()).isTrue();
        assertThat(confluenceCommand.isDurable()).isTrue();
        assertThat(notificationQueue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("creates expected bindings")
    void createsExpectedBindings() {
        Binding arcgisCommandBinding = config.commandQueueBinding(config.arcgisExecutionCommandQueue(), config.arcgisExchange());
        Binding arcgisResultBinding = config.resultQueueBinding(config.arcgisExecutionResultQueue(), config.arcgisExchange());
        Binding jiraCommandBinding = config.jiraCommandQueueBinding(
                config.jiraWebhookExecutionCommandQueue(),
                config.jiraWebhookExchange());
        Binding jiraResultBinding = config.jiraResultQueueBinding(
                config.jiraWebhookExecutionResultQueue(),
                config.jiraWebhookExchange());
        Binding confluenceCommandBinding = config.confluenceCommandQueueBinding(
                config.confluenceExecutionCommandQueue(),
                config.confluenceExchange());
        Binding confluenceResultBinding = config.confluenceResultQueueBinding(
                config.confluenceExecutionResultQueue(),
                config.confluenceExchange());
        Binding notificationBinding = config.notificationQueueBinding(
                config.notificationQueueIms(),
                config.notificationExchange());

        assertThat(arcgisCommandBinding.getRoutingKey()).isEqualTo(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE);
        assertThat(arcgisResultBinding.getRoutingKey()).isEqualTo(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
        assertThat(jiraCommandBinding.getRoutingKey()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE);
        assertThat(jiraResultBinding.getRoutingKey()).isEqualTo(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
        assertThat(confluenceCommandBinding.getRoutingKey()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE);
        assertThat(confluenceResultBinding.getRoutingKey()).isEqualTo(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
        assertThat(notificationBinding.getRoutingKey()).isEqualTo(QueueNames.NOTIFICATION_ROUTING_KEY);
    }

    @Test
    @DisplayName("creates rabbitTemplate with configured converter")
    void createsRabbitTemplateWithConfiguredConverter() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter converter = config.jacksonJsonMessageConverter();

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory, converter);

        assertThat(rabbitTemplate.getMessageConverter()).isSameAs(converter);
    }
}
