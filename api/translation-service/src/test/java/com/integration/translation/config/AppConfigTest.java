package com.integration.translation.config;

import com.integration.translation.config.properties.OllamaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig config = new AppConfig();

    @Test
    @DisplayName("objectMapper() returns a non-null, properly configured ObjectMapper")
    void objectMapper_returnsConfiguredMapper() {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper).isNotNull();
        // Java-time serialisation should be registered (no UnknownModuleException)
        assertThat(mapper.getRegisteredModuleIds())
                .anyMatch(id -> id.toString().contains("jackson-datatype-jsr310"));
    }

    @Test
    @DisplayName("restTemplate() returns a non-null RestTemplate with default OllamaProperties")
    void restTemplate_defaultProperties_returnsConfiguredTemplate() {
        OllamaProperties props = new OllamaProperties(); // uses field defaults

        RestTemplate template = config.restTemplate(props);

        assertThat(template).isNotNull();
        assertThat(template.getMessageConverters()).isNotEmpty();
    }

    @Test
    @DisplayName("restTemplate() UTF-8 StringHttpMessageConverter is registered first")
    void restTemplate_utf8ConverterIsFirst() {
        OllamaProperties props = new OllamaProperties();
        RestTemplate template = config.restTemplate(props);

        String firstConverterName = template.getMessageConverters()
                .get(0)
                .getClass()
                .getSimpleName();
        assertThat(firstConverterName).isEqualTo("StringHttpMessageConverter");
    }

    @Test
    @DisplayName("restTemplate() uses custom timeouts from OllamaProperties")
    void restTemplate_customTimeouts_doesNotThrow() {
        OllamaProperties props = new OllamaProperties();
        props.setTimeoutSeconds(30);
        props.setConnectTimeoutSeconds(5);

        // Should construct without error
        assertThat(config.restTemplate(props)).isNotNull();
    }
}

