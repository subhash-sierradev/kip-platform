package com.integration.execution.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class AzureServiceBusConfigTest {

    @Test
    void constructor_logsWarning_withoutThrowing() {
        assertThatCode(AzureServiceBusConfig::new).doesNotThrowAnyException();
    }
}
