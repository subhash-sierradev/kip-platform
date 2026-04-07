package com.integration.management.mapper;

import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.model.dto.request.ConfluenceIntegrationCreateUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static com.integration.management.constants.IntegrationManagementConstants.ROOT_FOLDER_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfluenceIntegrationMapper")
class ConfluenceIntegrationMapperTest {

    private final ConfluenceIntegrationMapper mapper = Mappers.getMapper(ConfluenceIntegrationMapper.class);

    private ConfluenceIntegrationCreateUpdateRequest minimalRequest(String folderKey) {
        return ConfluenceIntegrationCreateUpdateRequest.builder()
                .name("Test Integration")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DOCUMENT_FINAL")
                .reportNameTemplate("Report {date}")
                .confluenceSpaceKey("MYSPACE")
                .confluenceSpaceKeyFolderKey(folderKey)
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .build();
    }

    @Test
    @DisplayName("toEntity defaults confluenceSpaceKeyFolderKey to ROOT when request value is null")
    void toEntity_nullFolderKey_defaultsToRoot() {
        ConfluenceIntegration entity = mapper.toEntity(minimalRequest(null));

        assertThat(entity.getConfluenceSpaceKeyFolderKey()).isEqualTo(ROOT_FOLDER_KEY);
    }

    @Test
    @DisplayName("toEntity defaults confluenceSpaceKeyFolderKey to ROOT when request value is blank")
    void toEntity_blankFolderKey_defaultsToRoot() {
        ConfluenceIntegration entity = mapper.toEntity(minimalRequest("   "));

        assertThat(entity.getConfluenceSpaceKeyFolderKey()).isEqualTo(ROOT_FOLDER_KEY);
    }

    @Test
    @DisplayName("toEntity defaults confluenceSpaceKeyFolderKey to ROOT when request value is empty string")
    void toEntity_emptyFolderKey_defaultsToRoot() {
        ConfluenceIntegration entity = mapper.toEntity(minimalRequest(""));

        assertThat(entity.getConfluenceSpaceKeyFolderKey()).isEqualTo(ROOT_FOLDER_KEY);
    }

    @Test
    @DisplayName("toEntity preserves explicit confluenceSpaceKeyFolderKey when provided")
    void toEntity_explicitFolderKey_preserved() {
        ConfluenceIntegration entity = mapper.toEntity(minimalRequest("PAGE-123"));

        assertThat(entity.getConfluenceSpaceKeyFolderKey()).isEqualTo("PAGE-123");
    }

    @Test
    @DisplayName("toEntity returns null for null request")
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("updateEntity defaults confluenceSpaceKeyFolderKey to ROOT when request value is null")
    void updateEntity_nullFolderKey_defaultsToRoot() {
        ConfluenceIntegration existing = ConfluenceIntegration.builder()
                .name("Old Name")
                .normalizedName("old_name")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DOCUMENT_FINAL")
                .reportNameTemplate("Old Report")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("OLD-KEY")
                .connectionId(UUID.randomUUID())
                .build();

        mapper.updateEntity(minimalRequest(null), existing);

        assertThat(existing.getConfluenceSpaceKeyFolderKey()).isEqualTo(ROOT_FOLDER_KEY);
    }

    @Test
    @DisplayName("updateEntity preserves explicit confluenceSpaceKeyFolderKey when provided")
    void updateEntity_explicitFolderKey_preserved() {
        ConfluenceIntegration existing = ConfluenceIntegration.builder()
                .name("Old")
                .normalizedName("old")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DOCUMENT_FINAL")
                .reportNameTemplate("Old Report")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .connectionId(UUID.randomUUID())
                .build();

        mapper.updateEntity(minimalRequest("NEW-PAGE"), existing);

        assertThat(existing.getConfluenceSpaceKeyFolderKey()).isEqualTo("NEW-PAGE");
    }

    @Test
    @DisplayName("toCreationResponse returns null id for entity without id set")
    void toCreationResponse_nullIdEntity() {
        ConfluenceIntegration entity = ConfluenceIntegration.builder()
                .name("My Integration")
                .normalizedName("my_integration")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DOCUMENT_FINAL")
                .reportNameTemplate("Report")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey(ROOT_FOLDER_KEY)
                .connectionId(UUID.randomUUID())
                .build();

        CreationResponse response = mapper.toCreationResponse(entity);
        assertThat(response.getId()).isNull();
    }
}
