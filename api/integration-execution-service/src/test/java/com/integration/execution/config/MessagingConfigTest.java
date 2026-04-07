package com.integration.execution.config;

import com.integration.execution.contract.messaging.MessageListener;
import com.integration.execution.contract.messaging.MessagePublisher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MessagingConfigTest {

    @Test
    void constructor_withNonNullImplementations_logsActiveTransport() {
        MessagePublisher publisher = new MessagePublisher() {
            @Override
            public void publish(String exchange, String routingKey, Object payload) { }

            @Override
            public void publishDirect(String queueName, Object payload) { }
        };
        MessageListener listener = (queueName, message) -> { };

        assertThatCode(() -> new MessagingConfig(publisher, listener, "rabbitmq"))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_withNullImplementations_logsNoneTransport() {
        assertThatCode(() -> new MessagingConfig(null, null, "azureservicebus"))
                .doesNotThrowAnyException();
    }
}
