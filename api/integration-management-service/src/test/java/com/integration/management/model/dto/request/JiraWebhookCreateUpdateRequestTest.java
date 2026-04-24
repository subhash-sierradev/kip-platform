package com.integration.management.model.dto.request;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookCreateUpdateRequest")
class JiraWebhookCreateUpdateRequestTest {

    @Test
    @DisplayName("builder populates request fields")
    void builderPopulatesRequestFields() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("Jira Hook")
                .description("Create issue on incoming payload")
                .connectionId(UUID.randomUUID())
                .fieldsMapping(validMappings())
                .samplePayload("{\"summary\":\"Issue\"}")
                .requestedTenantId("tenant-1")
                .build();

        assertThat(request.getName()).isEqualTo("Jira Hook");
        assertThat(request.getFieldsMapping()).hasSize(3);
        assertThat(request.getSamplePayload()).contains("summary");
        assertThat(request.getRequestedTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("bean validation rejects blank name and too-few field mappings")
    void beanValidationRejectsBlankNameAndTooFewFieldMappings() {
        JiraWebhookCreateUpdateRequest invalid = JiraWebhookCreateUpdateRequest.builder()
                .name(" ")
                .description("desc")
                .connectionId(UUID.randomUUID())
                .fieldsMapping(List.of(validMappings().getFirst(), validMappings().get(1)))
                .samplePayload("{\"a\":1}")
                .requestedTenantId("tenant-1")
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(invalid))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("name", "fieldsMapping");
        }
    }

    private List<JiraFieldMappingDto> validMappings() {
        return List.of(
                JiraFieldMappingDto.builder()
                        .jiraFieldId("summary")
                        .jiraFieldName("Summary")
                        .dataType(JiraDataType.STRING)
                        .template("{{summary}}")
                        .build(),
                JiraFieldMappingDto.builder()
                        .jiraFieldId("description")
                        .jiraFieldName("Description")
                        .dataType(JiraDataType.STRING)
                        .template("{{description}}")
                        .build(),
                JiraFieldMappingDto.builder()
                        .jiraFieldId("project")
                        .jiraFieldName("Project")
                        .dataType(JiraDataType.OBJECT)
                        .template("{{project}}")
                        .build());
    }
}
