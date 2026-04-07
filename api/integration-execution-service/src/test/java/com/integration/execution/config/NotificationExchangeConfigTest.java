package com.integration.execution.config;

import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationExchangeConfigTest {

    private final NotificationExchangeConfig config = new NotificationExchangeConfig();

    @Test
    void notificationExchange_isNotNull() {
        assertThat(config.notificationExchange()).isNotNull();
    }

    @Test
    void notificationExchange_hasCorrectName() {
        TopicExchange exchange = config.notificationExchange();

        assertThat(exchange.getName()).isEqualTo(QueueNames.NOTIFICATION_EXCHANGE);
    }

    @Test
    void notificationExchange_isDurable() {
        TopicExchange exchange = config.notificationExchange();

        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void notificationExchange_isNotAutoDelete() {
        TopicExchange exchange = config.notificationExchange();

        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void notificationExchange_isTopicExchange() {
        assertThat(config.notificationExchange()).isInstanceOf(TopicExchange.class);
    }
}
