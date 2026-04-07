package com.integration.management.service;

import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.management.entity.CredentialType;
import com.integration.management.repository.CredentialTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialTypeService")
class CredentialTypeServiceTest {

    @Mock
    private CredentialTypeRepository credentialTypeRepository;

    @InjectMocks
    private CredentialTypeService service;

    @Test
    @DisplayName("getAllCredentialTypes returns enabled credential types")
    void getAllCredentialTypes_returnsEnabled() {
        CredentialType apiKey = new CredentialType();
        apiKey.setCredentialAuthType(CredentialAuthType.BASIC_AUTH);
        when(credentialTypeRepository.findByIsEnabledTrue()).thenReturn(List.of(apiKey));

        List<CredentialType> result = service.getAllCredentialTypes();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCredentialAuthType()).isEqualTo(CredentialAuthType.BASIC_AUTH);
    }

    @Test
    @DisplayName("findByCredentialAuthType returns matching credential type")
    void findByCredentialAuthType_returnsMatch() {
        CredentialType oauth2 = new CredentialType();
        oauth2.setCredentialAuthType(CredentialAuthType.OAUTH2);
        when(credentialTypeRepository.findByCredentialAuthType(CredentialAuthType.OAUTH2))
                .thenReturn(Optional.of(oauth2));

        CredentialType result = service.findByCredentialAuthType(CredentialAuthType.OAUTH2);

        assertThat(result.getCredentialAuthType()).isEqualTo(CredentialAuthType.OAUTH2);
    }

    @Test
    @DisplayName("findByCredentialAuthType throws IllegalArgumentException when missing")
    void findByCredentialAuthType_missing_throws() {
        when(credentialTypeRepository.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credential auth type");
    }
}
