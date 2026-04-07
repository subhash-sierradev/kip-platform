package com.integration.execution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class IntegrationExecutionServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(IntegrationExecutionServiceApplication.class, args);
    }
}
