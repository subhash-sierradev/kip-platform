package com.integration.execution.contract.model;

import com.integration.execution.contract.model.enums.JiraDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraFieldMappingDto {
    private String id;
    @NotBlank(message = "Jira field ID is required")
    @Size(min = 1, max = 100, message = "Jira field ID must be between {min} and {max} characters")
    private String jiraFieldId;
    @NotBlank(message = "Jira field name is required")
    @Size(min = 1, max = 100, message = "Jira field name must be between {min} and {max} characters")
    private String jiraFieldName;
    @Size(max = 100, message = "Display label cannot exceed {max} characters")
    private String displayLabel;
    @NotNull(message = "Data type is required")
    private JiraDataType dataType;
    @Size(max = 1000, message = "Template cannot exceed {max} characters")
    private String template;
    @Builder.Default
    private Boolean required = false;
    @Size(max = 500, message = "Default value cannot exceed {max} characters")
    private String defaultValue;
    private Map<String, Object> metadata;
}