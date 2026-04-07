package com.integration.execution.controller;

import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.execution.service.KwGraphQLService;
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
@RequestMapping("/api")
@Validated
public class KwDocController {
    private final KwGraphQLService kwGraphQLService;

    @GetMapping("/dynamic-documents-types")
    public ResponseEntity<List<KwDynamicDocType>> getDynamicDocuments(
            @RequestParam @NotBlank String type,
            @RequestParam @NotBlank String subType) {
        List<KwDynamicDocType> docs = kwGraphQLService.fetchDynamicDocumentsTypes(type, subType);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/item-subtypes")
    public ResponseEntity<List<KwItemSubtypeDto>> getSubObjectLookups() {
        List<KwItemSubtypeDto> data = kwGraphQLService.fetchItemSubtypes();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/source-field-mappings")
    public ResponseEntity<List<KwDocField>> getFieldMappingData() {
        List<KwDocField> data = kwGraphQLService.fetchFieldMappingForLocations();
        return ResponseEntity.ok(data);
    }
}
