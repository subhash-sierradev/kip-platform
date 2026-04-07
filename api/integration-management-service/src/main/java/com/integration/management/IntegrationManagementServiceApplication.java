package com.integration.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.integration.management.ies.client")
@EnableScheduling
public class IntegrationManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationManagementServiceApplication.class, args);
    }
}
