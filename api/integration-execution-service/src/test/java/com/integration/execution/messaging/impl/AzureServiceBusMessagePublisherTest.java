package com.integration.execution.messaging.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureServiceBusMessagePublisherTest {

    @Test
    void constructor_logsWarning_withoutThrowing() {
        assertThatCode(AzureServiceBusMessagePublisher::new).doesNotThrowAnyException();
    }

    @Test
    void publish_throwsUnsupportedOperationException() {
        AzureServiceBusMessagePublisher publisher = new AzureServiceBusMessagePublisher();

        assertThatThrownBy(() -> publisher.publish("exchange", "key", "payload"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("implementation is pending");
    }

    @Test
    void publishDirect_throwsUnsupportedOperationException() {
        AzureServiceBusMessagePublisher publisher = new AzureServiceBusMessagePublisher();

        assertThatThrownBy(() -> publisher.publishDirect("queue", "payload"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("implementation is pending");
    }
}
