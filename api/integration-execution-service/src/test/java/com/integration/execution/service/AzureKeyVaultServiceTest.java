package com.integration.execution.service;

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.AzureKeyVaultSecretClient;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.exception.AzureKeyVaultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureKeyVaultServiceTest {

    @Mock
    private AzureKeyVaultSecretClient keyVaultSecretClient;

    @Mock
    private ObjectMapper objectMapper;

    private AzureKeyVaultService service;

    @BeforeEach
    void setUp() {
        service = new AzureKeyVaultService(keyVaultSecretClient, objectMapper);
    }

    @Test
    void saveSecret_success_storesSerializedPayload() throws Exception {
        IntegrationSecret secret = secret();
        when(objectMapper.writeValueAsString(secret)).thenReturn("{\"baseUrl\":\"https://x\"}");

        service.saveSecret("secret-a", secret);

        verify(keyVaultSecretClient).createJsonCredential("secret-a", "{\"baseUrl\":\"https://x\"}");
    }

    @Test
    void saveSecret_serializationError_throwsDomainException() throws Exception {
        IntegrationSecret secret = secret();
        when(objectMapper.writeValueAsString(secret)).thenThrow(new JsonProcessingException("bad json") { });

        assertThatThrownBy(() -> service.saveSecret("secret-a", secret))
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Failed to serialize credential data");
    }

    @Test
    void getSecret_success_deserializesAndReturnsSecret() throws Exception {
        KeyVaultSecret kvSecret = new KeyVaultSecret("secret-a", "{\"baseUrl\":\"https://x\"}");
        IntegrationSecret expected = secret();

        when(keyVaultSecretClient.get("secret-a")).thenReturn(kvSecret);
        when(objectMapper.readValue(eq("{\"baseUrl\":\"https://x\"}"), eq(IntegrationSecret.class)))
                .thenReturn(expected);

        IntegrationSecret result = service.getSecret("secret-a");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getSecret_whenSecretMissing_throwsCredentialNotFound() {
        when(keyVaultSecretClient.get("secret-a")).thenReturn(null);

        assertThatThrownBy(() -> service.getSecret("secret-a"))
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Credential not found for key: secret-a");
    }

    @Test
    void getSecret_whenSecretValueNull_throwsCredentialNotFound() {
        // KeyVaultSecret with null value
        KeyVaultSecret kvSecret = new KeyVaultSecret("secret-a", null);
        when(keyVaultSecretClient.get("secret-a")).thenReturn(kvSecret);

        assertThatThrownBy(() -> service.getSecret("secret-a"))
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Credential not found for key: secret-a");
    }

    @Test
    void getSecret_whenDeserializationFails_throwsDomainException() throws Exception {
        KeyVaultSecret kvSecret = new KeyVaultSecret("secret-a", "not-json");
        when(keyVaultSecretClient.get("secret-a")).thenReturn(kvSecret);
        when(objectMapper.readValue(eq("not-json"), eq(IntegrationSecret.class)))
                .thenThrow(new JsonProcessingException("invalid") { });

        assertThatThrownBy(() -> service.getSecret("secret-a"))
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Failed to deserialize credential data for key: secret-a");
    }

    @Test
    void getSecret_whenClientThrows_wrapsAsRetrieveOperationFailed() {
        when(keyVaultSecretClient.get("secret-a")).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.getSecret("secret-a"))
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Failed to retrieve credential with key: secret-a");
    }

    @Test
    void deleteSecret_whenClientFails_throwsDeleteOperationFailed() {
        doThrow(new RuntimeException("down")).when(keyVaultSecretClient).delete("secret-a");

        assertThatThrownBy(() -> service.deleteSecret("secret-a"))
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Failed to delete credential with key: secret-a");
    }

    @Test
    void listSecrets_success_returnsNames() {
        when(keyVaultSecretClient.listSecretNames()).thenReturn(List.of("a", "b"));

        List<String> names = service.listSecrets();

        assertThat(names).containsExactly("a", "b");
    }

    @Test
    void listSecrets_whenClientFails_throwsListOperationFailed() {
        when(keyVaultSecretClient.listSecretNames()).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.listSecrets())
                .isInstanceOf(AzureKeyVaultException.class)
                .hasMessageContaining("Failed to list credentials in Azure Key Vault");
    }

    private IntegrationSecret secret() {
        return IntegrationSecret.builder()
                .baseUrl("https://jira.example.com")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u").password("p").build())
                .build();
    }
}
