package com.integration.management.config;

import com.integration.execution.contract.messaging.MessageListener;
import com.integration.execution.contract.messaging.MessagePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

@DisplayName("MessagingConfig")
class MessagingConfigTest {

    @Test
    @DisplayName("constructor initializes with non-null publisher and listener")
    void constructor_nonNullDependencies_doesNotThrow() {
        MessagePublisher publisher = mock(MessagePublisher.class);
        MessageListener listener = mock(MessageListener.class);

        assertThatCode(() -> new MessagingConfig(publisher, listener, "rabbitmq"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("constructor initializes when publisher and listener are null")
    void constructor_nullDependencies_doesNotThrow() {
        assertThatCode(() -> new MessagingConfig(null, null, "rabbitmq"))
                .doesNotThrowAnyException();
    }
}
