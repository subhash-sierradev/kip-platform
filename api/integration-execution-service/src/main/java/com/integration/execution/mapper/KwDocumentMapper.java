package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.KwLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KwDocumentMapper {

    private final KwLocationMapper kwLocationMapper;

    public List<KwDocumentDto> convertToDocumentDtos(final JsonNode searchResults) {
        if (searchResults == null || !searchResults.isArray()) {
            log.warn("Search results is not an array, returning empty list");
            return List.of();
        }

        List<KwDocumentDto> documents = new ArrayList<>();
        for (JsonNode docNode : searchResults) {
            try {
                KwDocumentDto document = convertToDocumentDto(docNode);
                if (document != null) {
                    documents.add(document);
                }
            } catch (Exception e) {
                String docId = docNode.path("id").asText("unknown");
                log.error("Failed to convert document with id={}: {}", docId, e.getMessage(), e);
            }
        }
        log.info("Converted {} documents from search results", documents.size());
        return documents;
    }

    public KwDocumentDto convertToDocumentDto(final JsonNode docNode) {
        if (docNode == null || docNode.isNull()) {
            return null;
        }

        String id = docNode.path("id").asText(null);
        if (id == null || id.isBlank()) {
            log.warn("Document missing required 'id' field, skipping");
            return null;
        }

        String title = docNode.path("title").asText(null);
        long createdTimestamp = docNode.path("createdTimestamp").asLong(0L);
        long updatedTimestamp = docNode.path("updatedTimestamp").asLong(0L);
        String documentType = docNode.path("documentType").asText(null);

        JsonNode relatedEntities = docNode.path("relatedEntities");
        List<KwLocationDto> locations = kwLocationMapper.extractLocations(relatedEntities);

        if (!locations.isEmpty()) {
            KwDocumentDto document = new KwDocumentDto();
            document.setId(id);
            document.setTitle(title);
            document.setDocumentType(documentType);
            document.setCreatedTimestamp(createdTimestamp);
            document.setUpdatedTimestamp(updatedTimestamp);
            document.setLocations(locations);
            return document;
        } else {
            log.info("Document id={} has no related locations, skipping", id);
            return null;
        }
    }
}
