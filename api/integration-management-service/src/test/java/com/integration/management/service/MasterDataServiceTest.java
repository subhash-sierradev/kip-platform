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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MasterDataService")
class MasterDataServiceTest {

    @Mock
    private CredentialTypeRepository credentialTypeRepository;

    @InjectMocks
    private MasterDataService masterDataService;

    @Test
    @DisplayName("getAllCredentialTypes should delegate to repository")
    void getAllCredentialTypes_delegates() {
        List<CredentialType> expected = List.of();
        when(credentialTypeRepository.findByIsEnabledTrue()).thenReturn(expected);

        List<CredentialType> actual = masterDataService.getAllCredentialTypes();

        assertThat(actual).isSameAs(expected);
        verify(credentialTypeRepository).findByIsEnabledTrue();
    }

    @Test
    @DisplayName("findByCredentialAuthType should throw IllegalArgumentException when missing")
    void findByCredentialAuthType_missing_throws() {
        when(credentialTypeRepository.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> masterDataService.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credential auth type");
    }
}
