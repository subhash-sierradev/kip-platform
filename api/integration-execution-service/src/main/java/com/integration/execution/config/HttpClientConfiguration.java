package com.integration.execution.config;

import com.integration.execution.config.properties.HttpClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.time.Duration;

/**
 * Centralized HTTP client configuration for multiple service integrations.
 * Creates dedicated HTTP client instances for Jira, ArcGIS, Azure Key Vault, and Kaseware
 * with service-specific optimizations and connection pooling.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(HttpClientProperties.class)
@RequiredArgsConstructor
public class HttpClientConfiguration {

    private static final String SERVICE_JIRA = "jira";
    private static final String SERVICE_ARCGIS = "arcgis";
    private static final String SERVICE_KW = "kw";
    private static final String SERVICE_CONFLUENCE = "confluence";

    private final HttpClientProperties properties;

    @Bean("jiraHttpClient")
    public CloseableHttpClient jiraHttpClient() {
        HttpClientProperties.ServiceConfig config = properties.getJira();
        return createHttpClient(SERVICE_JIRA, config);
    }

    @Bean("arcgisHttpClient")
    public CloseableHttpClient arcgisHttpClient() {
        HttpClientProperties.ServiceConfig config = properties.getArcgis();
        return createHttpClient(SERVICE_ARCGIS, config);
    }

    @Bean("kwHttpClient")
    public CloseableHttpClient kwHttpClient() {
        HttpClientProperties.ServiceConfig config = properties.getKaseware();
        return createHttpClient(SERVICE_KW, config);
    }

    @Bean("confluenceHttpClient")
    public CloseableHttpClient confluenceHttpClient() {
        HttpClientProperties.ServiceConfig config = properties.getConfluence();
        return createHttpClient(SERVICE_CONFLUENCE, config);
    }

    private CloseableHttpClient createHttpClient(String serviceName, HttpClientProperties.ServiceConfig config) {
        PoolingHttpClientConnectionManager connectionManager = createConnectionManager(config);
        RequestConfig requestConfig = createRequestConfig(config);
        DefaultHttpRequestRetryStrategy retryStrategy = createRetryStrategy(config);

        var clientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryStrategy(retryStrategy)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(
                        Duration.ofMinutes(config.getConnectionPool().getIdleEvictMinutes()))
                );

        // Configure proxy if enabled
        if (serviceName.equals(SERVICE_KW) && properties.getProxy() != null && properties.getProxy().isEnabled()) {
            HttpHost proxy = new HttpHost(properties.getProxy().getHost(), properties.getProxy().getPort());
            clientBuilder.setProxy(proxy);
            log.info("************* Configured proxy for KW HTTP client *************");
        }

        return clientBuilder.build();
    }

    private PoolingHttpClientConnectionManager createConnectionManager(HttpClientProperties.ServiceConfig config) {
        SSLContext sslContext = SSLContexts.createSystemDefault();
        DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(config.getTimeouts().getConnectSeconds()))
                .setSocketTimeout(Timeout.ofSeconds(config.getTimeouts().getSocketSeconds()))
                .setTimeToLive(TimeValue.ofMinutes(config.getConnectionPool().getTtlMinutes()))
                .build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(config.getTimeouts().getSocketSeconds()))
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)  // Keep connections alive
                .setRcvBufSize(65536)  // 64KB receive buffer
                .setSndBufSize(65536)  // 64KB send buffer
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, hostnameVerifier))
                .setDefaultConnectionConfig(connectionConfig)
                .setDefaultSocketConfig(socketConfig)
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setMaxConnTotal(config.getConnectionPool().getMaxTotal())
                .setMaxConnPerRoute(config.getConnectionPool().getMaxPerRoute())
                .build();

        // Set route-specific maximum connections if configured
        if (config.getConnectionPool().getRouteMaxConnections() != null) {
            config.getConnectionPool().getRouteMaxConnections().forEach((route, maxConn) -> {
                try {
                    String[] hostPort = route.split(":");
                    HttpHost host = new HttpHost(hostPort[0],
                            hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 443);
                    connectionManager.setMaxPerRoute(new HttpRoute(host), maxConn);
                } catch (Exception e) {
                    log.warn("Failed to configure route-specific max connections for {}: {}", route, e.getMessage());
                }
            });
        }

        return connectionManager;
    }

    /**
     * Creates request configuration with service-specific timeouts.
     */
    private RequestConfig createRequestConfig(HttpClientProperties.ServiceConfig config) {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(config.getTimeouts().getConnectionRequestSeconds()))
                .setResponseTimeout(Timeout.ofSeconds(config.getTimeouts().getResponseSeconds()))
                .setRedirectsEnabled(config.getRedirectsEnabled())
                .setMaxRedirects(config.getMaxRedirects())
                .setCircularRedirectsAllowed(false)
                .build();
    }

    /**
     * Creates retry strategy with service-specific configuration.
     */
    private DefaultHttpRequestRetryStrategy createRetryStrategy(HttpClientProperties.ServiceConfig config) {
        if (config.getRetry() == null) {
            return new DefaultHttpRequestRetryStrategy(0, TimeValue.ofSeconds(0));
        }
        return new DefaultHttpRequestRetryStrategy(
                config.getRetry().getMaxAttempts(),
                TimeValue.ofSeconds(config.getRetry().getIntervalSeconds())
        );
    }
}