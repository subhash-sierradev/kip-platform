package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Configuration properties for HTTP client instances.
 * Maps to integration.http-clients configuration in YAML files.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "integration.http-clients")
public class HttpClientProperties {

    @Valid
    private ProxyConfig proxy;

    @Valid
    @NotNull
    private ServiceConfig jira = new ServiceConfig();

    @Valid
    @NotNull
    private ServiceConfig arcgis = new ServiceConfig();

    @Valid
    @NotNull
    private ServiceConfig kaseware = new ServiceConfig();

    @Valid
    @NotNull
    private ServiceConfig confluence = new ServiceConfig();

    @Data
    public static class ServiceConfig {
        @Valid
        @NotNull
        private ConnectionPoolConfig connectionPool = new ConnectionPoolConfig();

        @Valid
        @NotNull
        private TimeoutConfig timeouts = new TimeoutConfig();

        private RetryConfig retry;
        private boolean redirectsEnabled = true;

        @Min(0)
        private int maxRedirects = 3;

        // Getter methods for fields that don't have them due to @Data
        public boolean getRedirectsEnabled() {
            return redirectsEnabled;
        }

        @Data
        public static class ConnectionPoolConfig {
            @Min(1)
            private int maxTotal = 20;

            @Min(1)
            private int maxPerRoute = 5;

            @Min(1)
            private long ttlMinutes = 5;

            @Min(1)
            private long idleEvictMinutes = 2;

            /**
             * Route-specific maximum connections.
             * Key format: "hostname:port" (port optional, defaults to 443)
             * Value: maximum connections for that route
             */
            private Map<String, Integer> routeMaxConnections;
        }

        @Data
        public static class TimeoutConfig {
            @Min(1)
            private long connectSeconds = 10;

            @Min(1)
            private long socketSeconds = 30;

            @Min(1)
            private long responseSeconds = 60;

            @Min(1)
            private long connectionRequestSeconds = 10;
        }

        @Data
        public static class RetryConfig {
            @Min(0)
            private int maxAttempts = 3;

            @Min(0)
            private long intervalSeconds = 2;
        }
    }

    @Data
    public static class ProxyConfig {
        private boolean enabled;
        private String host;
        private int port = 8080;
    }
}