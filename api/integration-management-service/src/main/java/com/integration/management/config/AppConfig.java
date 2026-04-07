package com.integration.management.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.io.IOException;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
public class AppConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("IntegrationAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule()) // important for Java 8 dates
                .build();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new SimpleModule()
                .addDeserializer(String.class, new JsonDeserializer<>() {
                    @Override
                    public String deserialize(JsonParser p, DeserializationContext ctx)
                            throws IOException {
                        String value = p.getValueAsString();
                        return value == null ? null : value.trim();
                    }
                }));
        return mapper;
    }

    /**
     * Interceptor to forward the JWT token from the incoming request to outgoing Feign client calls.
     * This ensures that the downstream services receive the same authentication context.
     */
    @Bean
    public RequestInterceptor jwtTokenForwardingInterceptor() {
        return (RequestTemplate template) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return;
            }
            if (!(authentication instanceof JwtAuthenticationToken jwtAuthToken)) {
                return;
            }
            String tokenValue = jwtAuthToken.getToken().getTokenValue();
            template.header("Authorization", "Bearer " + tokenValue);
        };
    }
}
