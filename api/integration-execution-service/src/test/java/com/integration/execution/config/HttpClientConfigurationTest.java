package com.integration.execution.config;

import com.integration.execution.config.properties.HttpClientProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientConfigurationTest {

    @Test
    void createsServiceClients_withProxyEnabledAndRetryAndRouteConfig() throws Exception {
        HttpClientProperties properties = new HttpClientProperties();

        HttpClientProperties.ServiceConfig arcgis = properties.getArcgis();
        HttpClientProperties.ServiceConfig.RetryConfig retryConfig = new HttpClientProperties.ServiceConfig.RetryConfig();
        retryConfig.setMaxAttempts(2);
        retryConfig.setIntervalSeconds(1);
        arcgis.setRetry(retryConfig);

        HttpClientProperties.ProxyConfig proxy = new HttpClientProperties.ProxyConfig();
        proxy.setEnabled(true);
        proxy.setHost("localhost");
        proxy.setPort(8888);
        properties.setProxy(proxy);

        HttpClientProperties.ServiceConfig.ConnectionPoolConfig kwPool = properties.getKaseware().getConnectionPool();
        kwPool.setRouteMaxConnections(Map.of("localhost:443", 10, "invalid-route", 2));

        HttpClientConfiguration configuration = new HttpClientConfiguration(properties);

        try (CloseableHttpClient jira = configuration.jiraHttpClient();
             CloseableHttpClient arcgisClient = configuration.arcgisHttpClient();
             CloseableHttpClient kw = configuration.kwHttpClient()) {
            assertThat(jira).isNotNull();
            assertThat(arcgisClient).isNotNull();
            assertThat(kw).isNotNull();
        }
    }

    @Test
    void createsServiceClients_withNullProxyAndNullRetry() throws Exception {
        HttpClientProperties properties = new HttpClientProperties();
        // proxy left null — covers null-proxy branch in kwHttpClient
        // all retry configs left null — covers createRetryStrategy null-retry branch

        HttpClientConfiguration configuration = new HttpClientConfiguration(properties);

        try (CloseableHttpClient jira = configuration.jiraHttpClient();
             CloseableHttpClient arcgisClient = configuration.arcgisHttpClient();
             CloseableHttpClient kw = configuration.kwHttpClient()) {
            assertThat(jira).isNotNull();
            assertThat(arcgisClient).isNotNull();
            assertThat(kw).isNotNull();
        }
    }

    @Test
    void createsServiceClients_withProxyDisabled() throws Exception {
        HttpClientProperties properties = new HttpClientProperties();

        HttpClientProperties.ProxyConfig proxy = new HttpClientProperties.ProxyConfig();
        proxy.setEnabled(false);
        proxy.setHost("proxy.internal");
        proxy.setPort(3128);
        properties.setProxy(proxy);

        HttpClientConfiguration configuration = new HttpClientConfiguration(properties);

        try (CloseableHttpClient jira = configuration.jiraHttpClient();
             CloseableHttpClient arcgisClient = configuration.arcgisHttpClient();
             CloseableHttpClient kw = configuration.kwHttpClient()) {
            assertThat(jira).isNotNull();
            assertThat(arcgisClient).isNotNull();
            assertThat(kw).isNotNull();
        }
    }
}
