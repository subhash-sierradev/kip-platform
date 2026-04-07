package com.integration.management.model.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceIntegrationCreateUpdateRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeDocumentItemFields() throws Exception {
        String json = """
                {
                  \"name\": \"Daily Report\",
                  \"documentItemType\": \"DOCUMENT\",
                  \"documentItemSubtype\": \"DOCUMENT_FINAL_DYNAMIC\"
                }
                """;

        ConfluenceIntegrationCreateUpdateRequest request = objectMapper.readValue(
                json,
                ConfluenceIntegrationCreateUpdateRequest.class);

        assertThat(request.getDocumentItemType()).isEqualTo("DOCUMENT");
        assertThat(request.getDocumentItemSubtype()).isEqualTo("DOCUMENT_FINAL_DYNAMIC");
    }

    @Test
    void shouldDeserializeFrontendAliasItemFields() throws Exception {
        String json = """
                {
                  \"name\": \"Daily Report\",
                  \"itemType\": \"DOCUMENT\",
                  \"itemSubtype\": \"DOCUMENT_FINAL_DYNAMIC\"
                }
                """;

        ConfluenceIntegrationCreateUpdateRequest request = objectMapper.readValue(
                json,
                ConfluenceIntegrationCreateUpdateRequest.class);

        assertThat(request.getDocumentItemType()).isEqualTo("DOCUMENT");
        assertThat(request.getDocumentItemSubtype()).isEqualTo("DOCUMENT_FINAL_DYNAMIC");
    }
}
