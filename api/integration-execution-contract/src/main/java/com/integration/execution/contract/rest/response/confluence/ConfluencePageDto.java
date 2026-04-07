package com.integration.execution.contract.rest.response.confluence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Confluence page or folder returned by the pages API.
 * {@code parentTitle} is null for root-level items.
 * {@code type} is {@code page} or {@code folder}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluencePageDto {
    private String id;
    private String title;
    private String parentTitle;
    private String type;
}
