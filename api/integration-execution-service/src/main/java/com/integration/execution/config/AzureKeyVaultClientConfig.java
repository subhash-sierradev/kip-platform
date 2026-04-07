package com.integration.execution.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.integration.execution.config.properties.AzureKeyVaultProperties;
import com.integration.execution.config.properties.HttpClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;

/**
 * Azure Key Vault configuration class.
 * Creates SecretClient bean using configuration properties from application.yml
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AzureKeyVaultClientConfig {

    private final AzureKeyVaultProperties azureKeyVaultProperties;
    private final HttpClientProperties httpClientProperties;

    @Bean
    public SecretClient secretClient() {

        String keyVaultUrl = azureKeyVaultProperties.getUrl();
        if (!StringUtils.hasText(keyVaultUrl)) {
            throw new IllegalStateException(
                    "Azure Key Vault URL is required. Please configure integration.azure.keyvault.url"
            );
        }

        SecretClientBuilder clientBuilder =
                new SecretClientBuilder().vaultUrl(keyVaultUrl);

        clientBuilder.credential(resolveCredential());

        HttpClient httpClient = buildAzureHttpClientWithOptionalProxy();
        if (httpClient != null) {
            clientBuilder.httpClient(httpClient);
        }

        return clientBuilder.buildClient();
    }

    private TokenCredential resolveCredential() {

        if (StringUtils.hasText(azureKeyVaultProperties.getClientId())
                && StringUtils.hasText(azureKeyVaultProperties.getClientSecret())
                && StringUtils.hasText(azureKeyVaultProperties.getTenantId())) {

            log.info("Configuring Azure Key Vault with client secret authentication");

            return new ClientSecretCredentialBuilder()
                    .clientId(azureKeyVaultProperties.getClientId())
                    .clientSecret(azureKeyVaultProperties.getClientSecret())
                    .tenantId(azureKeyVaultProperties.getTenantId())
                    .build();
        }

        log.info("Configuring Azure Key Vault with DefaultAzureCredential");
        return new DefaultAzureCredentialBuilder().build();
    }

    private HttpClient buildAzureHttpClientWithOptionalProxy() {

        HttpClientProperties.ProxyConfig proxyConfig = httpClientProperties.getProxy();

        if (proxyConfig == null || !proxyConfig.isEnabled()) {
            log.debug("Azure Key Vault proxy not configured or disabled");
            return null;
        }
        log.info("Configuring Azure Key Vault HTTP client proxy");
        ProxyOptions proxyOptions = new ProxyOptions(
                ProxyOptions.Type.HTTP,
                new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())
        );
        return new NettyAsyncHttpClientBuilder()
                .proxy(proxyOptions)
                .build();
    }
}
