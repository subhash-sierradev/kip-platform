package com.integration.execution.messaging.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureServiceBusMessageListenerTest {

    @Test
    void constructor_logsWarning_withoutThrowing() {
        assertThatCode(AzureServiceBusMessageListener::new).doesNotThrowAnyException();
    }

    @Test
    void onMessageReceived_throwsUnsupportedOperationException() {
        AzureServiceBusMessageListener listener = new AzureServiceBusMessageListener();

        assertThatThrownBy(() -> listener.onMessageReceived("queue", "message"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("implementation is pending");
    }
}
