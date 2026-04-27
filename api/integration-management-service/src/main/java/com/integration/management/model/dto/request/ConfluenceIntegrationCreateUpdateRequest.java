package com.integration.management.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating or updating a Confluence integration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluenceIntegrationCreateUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed {max} characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed {max} characters")
    private String description;

    @JsonAlias("itemType")
    @NotBlank(message = "Document item type is required")
    @Size(max = 100, message = "Document item type cannot exceed {max} characters")
    private String documentItemType;

    @JsonAlias("itemSubtype")
    @NotBlank(message = "Document item subtype is required")
    @Size(max = 100, message = "Document item subtype cannot exceed {max} characters")
    private String documentItemSubtype;

    @Size(max = 100, message = "Dynamic document type cannot exceed {max} characters")
    private String dynamicDocumentType;

    @NotEmpty(message = "At least one language code is required")
    private List<String> languageCodes;

    /**
     * BCP-47 source language of the FreeMarker report template.
     * Defaults to "en" (English). When a target language in {@code languageCodes}
     * differs from this value the Translation API will be invoked.
     */
    @Size(max = 10, message = "Source language code cannot exceed {max} characters")
    private String sourceLanguage = "en";

    @NotBlank(message = "Report name template is required")
    @Size(max = 255, message = "Report name template cannot exceed {max} characters")
    private String reportNameTemplate;

    @NotBlank(message = "Confluence space key is required")
    @Size(max = 100, message = "Confluence space key cannot exceed {max} characters")
    private String confluenceSpaceKey;

    @Size(max = 100, message = "Confluence space key folder key cannot exceed {max} characters")
    private String confluenceSpaceKeyFolderKey = "ROOT";

    private Boolean includeTableOfContents;

    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @NotNull(message = "Schedule is required")
    @Valid
    private IntegrationScheduleRequest schedule;
}
