package com.integration.management.controller;

import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.management.entity.CredentialType;
import com.integration.management.entity.Language;
import com.integration.management.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterDataController {
    private final MasterDataService masterDataService;

    @GetMapping("/credential-types")
    public ResponseEntity<List<CredentialType>> getAllEnabledCredentialTypes() {
        log.info("Fetching all enabled credential types");
        return ResponseEntity.ok(masterDataService.getAllCredentialTypes());
    }

    @GetMapping("/credential-types/{authType}")
    public ResponseEntity<CredentialType> findByCredentialAuthType(@PathVariable CredentialAuthType authType) {
        return ResponseEntity.ok(masterDataService.findByCredentialAuthType(authType));
    }

    @GetMapping("/master-data/languages")
    public ResponseEntity<List<Language>> getAllActiveLanguages() {
        log.info("Fetching all active languages");
        return ResponseEntity.ok(masterDataService.getAllActiveLanguages());
    }
}
