package com.integration.execution.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// TODO KIP-547 REMOVE — Temporary ArcGIS verification HTTP client configuration
@Configuration
public class ArcGISVerificationConfig {

    /**
     * Dedicated RestTemplate for ArcGIS verification calls.
     * Isolated from any other HTTP clients used in production flows.
     */
    @Bean
    public RestTemplate arcGISVerificationRestTemplate() {
        return new RestTemplate();
    }
}
