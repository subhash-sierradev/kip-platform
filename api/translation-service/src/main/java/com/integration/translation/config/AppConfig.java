package com.integration.translation.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.integration.translation.config.properties.OllamaProperties;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Core application configuration.
 *
 * <p>Registers the primary Spring beans shared across the service:
 * <ul>
 *   <li>{@link ObjectMapper} — Jackson mapper with Java-time support and lenient
 *       deserialization (ignores unknown fields, required for Ollama response
 *       evolution).</li>
 *   <li>{@link RestTemplate} — Apache HttpClient 5-backed template with explicit
 *       UTF-8 string converter, connection pooling, and per-request timeouts
 *       sourced from {@link OllamaProperties}.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class AppConfig {

    /**
     * Produces the shared {@link ObjectMapper} with JavaTimeModule enabled.
     *
     * @return configured Jackson {@code ObjectMapper}
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Produces a {@link RestTemplate} backed by Apache HttpClient 5 with:
     * <ul>
     *   <li>UTF-8 {@link StringHttpMessageConverter} registered first so multi-byte
     *       characters (Japanese, Russian, Arabic, …) are preserved without
     *       corruption.</li>
     *   <li>Connection pool sized for moderate concurrency without over-allocating
     *       file descriptors.</li>
     *   <li>Connect and response timeouts driven by {@link OllamaProperties} so
     *       operators can tune them via YAML without a code change.</li>
     * </ul>
     *
     * @param props Ollama configuration properties
     * @return configured {@code RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate(final OllamaProperties props) {
        PoolingHttpClientConnectionManager connManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(20)
                        .setMaxConnPerRoute(10)
                        .setConnectionTimeToLive(TimeValue.of(5, TimeUnit.MINUTES))
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(
                    Timeout.of(props.getConnectTimeoutSeconds(), TimeUnit.SECONDS))
                .setResponseTimeout(
                    Timeout.of(props.getTimeoutSeconds(), TimeUnit.SECONDS))
                .setConnectionRequestTimeout(
                    Timeout.of(props.getConnectTimeoutSeconds(), TimeUnit.SECONDS))
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);

        // Ensure UTF-8 StringHttpMessageConverter is first so multi-byte text is
        // serialised correctly when posting the prompt and reading the response.
        restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));

        return restTemplate;
    }
}





