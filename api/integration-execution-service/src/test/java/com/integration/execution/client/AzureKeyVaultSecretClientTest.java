package com.integration.execution.client;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.polling.SyncPoller;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static com.integration.execution.constants.HttpConstants.JSON_CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureKeyVaultSecretClientTest {

    @Mock
    private SecretClient secretClient;

    @Mock
    private SyncPoller<DeletedSecret, Void> deletePoller;

    @Mock
    private PagedIterable<SecretProperties> pagedIterable;

    private AzureKeyVaultSecretClient client;

    @BeforeEach
    void setUp() {
        client = new AzureKeyVaultSecretClient(secretClient);
    }

    @Test
    void createJsonCredential_setsContentTypeAndTag() {
        client.createJsonCredential("secret-a", "{\"k\":\"v\"}");

        ArgumentCaptor<KeyVaultSecret> secretCaptor = ArgumentCaptor.forClass(KeyVaultSecret.class);
        verify(secretClient).setSecret(secretCaptor.capture());

        KeyVaultSecret created = secretCaptor.getValue();
        assertThat(created.getName()).isEqualTo("secret-a");
        assertThat(created.getValue()).isEqualTo("{\"k\":\"v\"}");
        assertThat(created.getProperties().getContentType()).isEqualTo(JSON_CONTENT_TYPE);
        assertThat(created.getProperties().getTags()).containsEntry("type", "json-credential");
    }

    @Test
    void get_returnsSecretFromClient() {
        KeyVaultSecret expected = new KeyVaultSecret("secret-a", "value");
        when(secretClient.getSecret("secret-a")).thenReturn(expected);

        KeyVaultSecret result = client.get("secret-a");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void delete_invokesWaitForCompletion() {
        when(secretClient.beginDeleteSecret("secret-a")).thenReturn(deletePoller);

        client.delete("secret-a");

        verify(deletePoller).waitForCompletion();
    }

    @Test
    void purge_delegatesToSecretClient() {
        client.purge("secret-a");

        verify(secretClient).purgeDeletedSecret("secret-a");
    }

    @Test
    void listSecretNames_mapsSecretPropertiesToNames() {
        SecretProperties s1 = mock(SecretProperties.class);
        SecretProperties s2 = mock(SecretProperties.class);
        when(s1.getName()).thenReturn("alpha");
        when(s2.getName()).thenReturn("beta");
        when(secretClient.listPropertiesOfSecrets()).thenReturn(pagedIterable);
        when(pagedIterable.stream()).thenReturn(Stream.of(s1, s2));

        List<String> names = client.listSecretNames();

        assertThat(names).containsExactly("alpha", "beta");
    }
}
