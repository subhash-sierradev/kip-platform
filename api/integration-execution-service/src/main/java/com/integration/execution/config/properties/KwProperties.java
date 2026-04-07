package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "integration.kaseware")
public class KwProperties {

    @Valid
    @NotNull
    private Auth auth = new Auth();

    @Valid
    @NotNull
    private GraphQL graphql = new GraphQL();

    @Data
    public static class Auth {
        @NotNull
        private String tokenUrl;

        @NotNull
        private String clientId;

        @NotNull
        private String username;

        @NotNull
        private String password;

        @NotNull
        private String grantType;

        private String scope;

        @NotNull
        private Duration tokenRefreshInterval;

        private int maxRetries = 3;

        @Valid
        private Headers headers = new Headers();

        @Data
        public static class Headers {
            private String userAgent;
            private String origin;
            private String referer;
            private String acceptEncoding;
        }
    }

    @Data
    public static class GraphQL {
        @NotNull
        private String endpoint;
    }
}