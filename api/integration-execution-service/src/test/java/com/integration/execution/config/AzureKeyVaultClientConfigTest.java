package com.integration.execution.config;

import com.azure.security.keyvault.secrets.SecretClient;
import com.integration.execution.config.properties.AzureKeyVaultProperties;
import com.integration.execution.config.properties.HttpClientProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureKeyVaultClientConfigTest {

    @Test
    void secretClient_withoutVaultUrl_throwsIllegalStateException() {
        AzureKeyVaultProperties kvProperties = new AzureKeyVaultProperties();
        HttpClientProperties httpClientProperties = new HttpClientProperties();
        AzureKeyVaultClientConfig config = new AzureKeyVaultClientConfig(kvProperties, httpClientProperties);

        assertThatThrownBy(config::secretClient)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Azure Key Vault URL is required");
    }

    @Test
    void secretClient_withVaultUrlAndDefaultCredential_buildsClient() {
        AzureKeyVaultProperties kvProperties = new AzureKeyVaultProperties();
        kvProperties.setUrl("https://example-vault.vault.azure.net/");

        HttpClientProperties httpClientProperties = new HttpClientProperties();
        HttpClientProperties.ProxyConfig proxy = new HttpClientProperties.ProxyConfig();
        proxy.setEnabled(false);
        httpClientProperties.setProxy(proxy);

        AzureKeyVaultClientConfig config = new AzureKeyVaultClientConfig(kvProperties, httpClientProperties);

        SecretClient client = config.secretClient();

        assertThat(client).isNotNull();
    }

    @Test
    void secretClient_withClientSecretCredentials_buildsClient() {
        AzureKeyVaultProperties kvProperties = new AzureKeyVaultProperties();
        kvProperties.setUrl("https://example-vault.vault.azure.net/");
        kvProperties.setClientId("test-client-id");
        kvProperties.setClientSecret("test-client-secret");
        kvProperties.setTenantId("test-tenant-id");

        HttpClientProperties httpClientProperties = new HttpClientProperties();
        HttpClientProperties.ProxyConfig proxy = new HttpClientProperties.ProxyConfig();
        proxy.setEnabled(false);
        httpClientProperties.setProxy(proxy);

        AzureKeyVaultClientConfig config = new AzureKeyVaultClientConfig(kvProperties, httpClientProperties);

        SecretClient client = config.secretClient();

        assertThat(client).isNotNull();
    }

    @Test
    void secretClient_withNullProxy_buildsClient() {
        AzureKeyVaultProperties kvProperties = new AzureKeyVaultProperties();
        kvProperties.setUrl("https://example-vault.vault.azure.net/");

        HttpClientProperties httpClientProperties = new HttpClientProperties();
        // proxy left as null — covers null proxyConfig branch

        AzureKeyVaultClientConfig config = new AzureKeyVaultClientConfig(kvProperties, httpClientProperties);

        SecretClient client = config.secretClient();

        assertThat(client).isNotNull();
    }

    @Test
    void secretClient_withProxyEnabled_buildsClient() {
        AzureKeyVaultProperties kvProperties = new AzureKeyVaultProperties();
        kvProperties.setUrl("https://example-vault.vault.azure.net/");

        HttpClientProperties httpClientProperties = new HttpClientProperties();
        HttpClientProperties.ProxyConfig proxy = new HttpClientProperties.ProxyConfig();
        proxy.setEnabled(true);
        proxy.setHost("proxy.example.com");
        proxy.setPort(8080);
        httpClientProperties.setProxy(proxy);

        AzureKeyVaultClientConfig config = new AzureKeyVaultClientConfig(kvProperties, httpClientProperties);

        SecretClient client = config.secretClient();

        assertThat(client).isNotNull();
    }

    @Test
    void secretClient_withBlankUrl_throwsIllegalStateException() {
        AzureKeyVaultProperties kvProperties = new AzureKeyVaultProperties();
        kvProperties.setUrl("   ");
        HttpClientProperties httpClientProperties = new HttpClientProperties();
        AzureKeyVaultClientConfig config = new AzureKeyVaultClientConfig(kvProperties, httpClientProperties);

        assertThatThrownBy(config::secretClient)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Azure Key Vault URL is required");
    }
}
