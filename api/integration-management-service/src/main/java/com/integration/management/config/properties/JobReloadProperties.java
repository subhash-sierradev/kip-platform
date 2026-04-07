package com.integration.management.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "integration-platform.job-reload")
public class JobReloadProperties {
    private boolean enabled = true;
    private boolean async = true;
    private boolean retryFailed = true;
    private int maxRetryAttempts = 3;
    private long retryDelayMillis = 5000L;
    private boolean verboseLogging;
    private long timeoutMillis = 300000L;
}
