package com.integration.execution.config;

import com.integration.execution.contract.queue.QueueNames;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the notification topic exchange in IES.
 * IES publishes notification events to this exchange; IMS owns the queue and binding.
 */
@Configuration
public class NotificationExchangeConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(QueueNames.NOTIFICATION_EXCHANGE, true, false);
    }
}
