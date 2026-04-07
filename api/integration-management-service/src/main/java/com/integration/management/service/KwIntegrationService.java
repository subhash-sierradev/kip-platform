package com.integration.management.service;

import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.management.ies.client.IesKwApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KwIntegrationService {

    private final IesKwApiClient iesKwApiClient;

    public List<KwDynamicDocType> getDynamicDocuments(String type, String subType) {
        try {
            return iesKwApiClient.getDynamicDocuments(type, subType);
        } catch (Exception e) {
            log.error("Error fetching dynamic documents for type: {}, subType: {}. Error: {}",
                    type, subType, e.getMessage(), e);
            return List.of(); // Return empty list on error to avoid breaking the flow
        }
    }

    public List<KwItemSubtypeDto> getItemSubtypesList() {
        try {
            return iesKwApiClient.getItemSubtypes();
        }  catch (Exception e) {
            log.error("Error fetching item subtypes. Error: {}", e.getMessage(), e);
            return List.of(); // Return empty list on error to avoid breaking the flow
        }
    }

    public String getItemSubtypeDisplayValue(String itemSubtype) {
        if (itemSubtype == null || itemSubtype.isBlank()) {
            return null;
        }
        return getItemSubtypesList().stream()
                .filter(s -> Objects.equals(itemSubtype, s.code()))
                .map(KwItemSubtypeDto::displayValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public String getDynamicDocumentTypeDisplayValue(String type, String subType, String dynamicDocumentType) {
        List<KwDynamicDocType> docs = iesKwApiClient.getDynamicDocuments(type, subType);
        if (docs == null || docs.isEmpty()) {
            return null;
        }
        return docs.stream()
                .filter(d -> Objects.equals(dynamicDocumentType, d.id()))
                .map(KwDynamicDocType::title)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public List<KwDocField> getFieldMappingForLocations() {
        return iesKwApiClient.getSourceFieldMappings();
    }
}
