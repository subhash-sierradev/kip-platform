package com.integration.management.controller;

import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.management.service.KwIntegrationService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kw")
@Validated
public class KwIntegrationController {
    private final KwIntegrationService kwIntegrationService;

    @GetMapping("/dynamic-documents-types")
    public ResponseEntity<List<KwDynamicDocType>> getDynamicDocuments(
            @RequestParam @NotBlank String type,
            @RequestParam @NotBlank String subType) {
        List<KwDynamicDocType> docs = kwIntegrationService.getDynamicDocuments(type, subType);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/item-subtypes")
    public ResponseEntity<List<KwItemSubtypeDto>> getSubObjectLookups() {
        List<KwItemSubtypeDto> data = kwIntegrationService.getItemSubtypesList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/source-field-mappings")
    public ResponseEntity<List<KwDocField>> getFieldMappingData() {
        List<KwDocField> data = kwIntegrationService.getFieldMappingForLocations();
        return ResponseEntity.ok(data);
    }
}
