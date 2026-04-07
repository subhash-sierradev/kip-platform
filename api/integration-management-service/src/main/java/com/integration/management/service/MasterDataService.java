package com.integration.management.service;

import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.management.entity.CredentialType;
import com.integration.management.entity.Language;
import com.integration.management.repository.CredentialTypeRepository;
import com.integration.management.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataService {
    private final CredentialTypeRepository credentialTypeRepository;
    private final LanguageRepository languageRepository;

    @Cacheable(value = "credentialTypesCache")
    public List<CredentialType> getAllCredentialTypes() {
        return credentialTypeRepository.findByIsEnabledTrue();
    }

    @Cacheable(value = "credentialTypesCache", key = "#credentialAuthType")
    public CredentialType findByCredentialAuthType(CredentialAuthType credentialAuthType) {
        return credentialTypeRepository.findByCredentialAuthType(credentialAuthType)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credential auth type: " + credentialAuthType));
    }

    @Cacheable(value = "languagesCache")
    public List<Language> getAllActiveLanguages() {
        return languageRepository.findAllByIsEnabledTrueOrderBySortOrderAsc();
    }
}
