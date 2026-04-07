package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.KwLocationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwDocumentMapperTest {

    @Mock
    private KwLocationMapper kwLocationMapper;

    private KwDocumentMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new KwDocumentMapper(kwLocationMapper);
        objectMapper = new ObjectMapper();
    }

    @Test
    void convertToDocumentDtos_withNonArray_returnsEmpty() throws Exception {
        JsonNode node = objectMapper.readTree("{\"id\":\"a\"}");

        assertThat(mapper.convertToDocumentDtos(node)).isEmpty();
    }

    @Test
    void convertToDocumentDtos_skipsInvalidAndFailedDocuments() throws Exception {
        JsonNode node = objectMapper.readTree("""
                [
                  {"id":"doc-1","title":"A","relatedEntities":[]},
                  {"title":"Missing id","relatedEntities":[]},
                  {"id":"doc-3","title":"C","relatedEntities":[]}
                ]
                """);

        when(kwLocationMapper.extractLocations(any()))
                .thenReturn(List.of(new KwLocationDto("loc-1", 1L, 2L, "HQ", "Address", null, null,
                        null, null, null, null, null, null, null, null)))
                .thenThrow(new RuntimeException("mapper failure"));

        List<KwDocumentDto> documents = mapper.convertToDocumentDtos(node);

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getId()).isEqualTo("doc-1");
    }

    @Test
    void convertToDocumentDto_nullNode_returnsNull() {
        assertThat(mapper.convertToDocumentDto(null)).isNull();
    }

    @Test
    void convertToDocumentDto_withoutLocations_returnsNull() throws Exception {
        JsonNode doc = objectMapper.readTree("{\"id\":\"doc-1\",\"relatedEntities\":[]}");
        when(kwLocationMapper.extractLocations(any())).thenReturn(List.of());

        assertThat(mapper.convertToDocumentDto(doc)).isNull();
    }

    @Test
    void convertToDocumentDto_withLocations_mapsFields() throws Exception {
        JsonNode doc = objectMapper.readTree("""
                {
                  "id":"doc-1",
                  "title":"Case Document",
                  "documentType":"REPORT",
                  "createdTimestamp":10,
                  "updatedTimestamp":20,
                  "relatedEntities":[]
                }
                """);
        when(kwLocationMapper.extractLocations(any()))
                .thenReturn(List.of(new KwLocationDto("loc-1", 1L, 2L, "HQ", "Address", null, null,
                        null, null, null, null, null, null, null, null)));

        KwDocumentDto document = mapper.convertToDocumentDto(doc);

        assertThat(document).isNotNull();
        assertThat(document.getId()).isEqualTo("doc-1");
        assertThat(document.getTitle()).isEqualTo("Case Document");
        assertThat(document.getDocumentType()).isEqualTo("REPORT");
        assertThat(document.getCreatedTimestamp()).isEqualTo(10L);
        assertThat(document.getUpdatedTimestamp()).isEqualTo(20L);
        assertThat(document.getLocations()).hasSize(1);
    }
}
