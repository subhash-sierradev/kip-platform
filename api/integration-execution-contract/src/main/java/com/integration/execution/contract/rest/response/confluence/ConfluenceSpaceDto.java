package com.integration.execution.contract.rest.response.confluence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Confluence space returned by the spaces API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluenceSpaceDto {
    private String key;
    private String name;
    private String type;
    private String description;
}
