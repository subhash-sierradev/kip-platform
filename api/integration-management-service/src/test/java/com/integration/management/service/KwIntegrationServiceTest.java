package com.integration.management.service;

import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.management.ies.client.IesKwApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KwIntegrationService")
class KwIntegrationServiceTest {

    @Mock
    private IesKwApiClient iesKwApiClient;

    @InjectMocks
    private KwIntegrationService kwIntegrationService;

    @Test
    @DisplayName("getItemSubtypeDisplayValue should return null for blank code and not call feign")
    void getItemSubtypeDisplayValue_blank_returnsNull() {
        assertThat(kwIntegrationService.getItemSubtypeDisplayValue(" ")).isNull();
        verify(iesKwApiClient, never()).getItemSubtypes();
    }

    @Test
    @DisplayName("getItemSubtypeDisplayValue should lookup display value by code")
    void getItemSubtypeDisplayValue_looksUp() {
        when(iesKwApiClient.getItemSubtypes()).thenReturn(List.of(
                new KwItemSubtypeDto("A", "Alpha"),
                new KwItemSubtypeDto("B", "Beta")));

        assertThat(kwIntegrationService.getItemSubtypeDisplayValue("B")).isEqualTo("Beta");
        verify(iesKwApiClient).getItemSubtypes();
    }

    @Test
    @DisplayName("getDynamicDocumentTypeDisplayValue should return null when feign returns empty")
    void getDynamicDocumentTypeDisplayValue_empty_returnsNull() {
        when(iesKwApiClient.getDynamicDocuments("T", "S")).thenReturn(List.of());

        assertThat(kwIntegrationService.getDynamicDocumentTypeDisplayValue("T", "S", "id")).isNull();
    }

    @Test
    @DisplayName("getDynamicDocumentTypeDisplayValue should map title for matching id")
    void getDynamicDocumentTypeDisplayValue_mapsTitle() {
        when(iesKwApiClient.getDynamicDocuments("T", "S")).thenReturn(List.of(
                new KwDynamicDocType("x", "X", List.of()),
                new KwDynamicDocType("y", "Y", List.of())));

        assertThat(kwIntegrationService.getDynamicDocumentTypeDisplayValue("T", "S", "y")).isEqualTo("Y");
    }

    @Test
    @DisplayName("getDynamicDocumentTypeDisplayValue should return null when feign returns null")
    void getDynamicDocumentTypeDisplayValue_null_returnsNull() {
        when(iesKwApiClient.getDynamicDocuments("T", "S")).thenReturn(null);

        assertThat(kwIntegrationService.getDynamicDocumentTypeDisplayValue("T", "S", "id")).isNull();
    }

    @Test
    @DisplayName("getItemSubtypeDisplayValue should return null when code has no match")
    void getItemSubtypeDisplayValue_noMatch_returnsNull() {
        when(iesKwApiClient.getItemSubtypes()).thenReturn(List.of(
                new KwItemSubtypeDto("A", "Alpha")));

        assertThat(kwIntegrationService.getItemSubtypeDisplayValue("Z")).isNull();
    }

    @Test
    @DisplayName("getDynamicDocumentTypeDisplayValue should return null when no id match")
    void getDynamicDocumentTypeDisplayValue_noMatch_returnsNull() {
        when(iesKwApiClient.getDynamicDocuments("T", "S")).thenReturn(List.of(
                new KwDynamicDocType("x", "X", List.of())));

        assertThat(kwIntegrationService.getDynamicDocumentTypeDisplayValue("T", "S", "unknown")).isNull();
    }

    @Test
    @DisplayName("getItemSubtypeDisplayValue should return null when display value is null")
    void getItemSubtypeDisplayValue_nullDisplayValue_returnsNull() {
        when(iesKwApiClient.getItemSubtypes()).thenReturn(List.of(
                new KwItemSubtypeDto("A", null)));

        assertThat(kwIntegrationService.getItemSubtypeDisplayValue("A")).isNull();
    }

    @Test
    @DisplayName("getItemSubtypeDisplayValue should return null when code is null")
    void getItemSubtypeDisplayValue_null_returnsNull() {
        assertThat(kwIntegrationService.getItemSubtypeDisplayValue(null)).isNull();
    }

    @Test
    @DisplayName("getFieldMappingForLocations should delegate to feign client")
    void getFieldMappingForLocations_delegates() {
        KwDocField field = KwDocField.builder()
                .id(1).fieldName("Field One").fieldType("STRING").isMandatory(false).build();
        when(iesKwApiClient.getSourceFieldMappings()).thenReturn(List.of(field));

        assertThat(kwIntegrationService.getFieldMappingForLocations()).containsExactly(field);
        verify(iesKwApiClient).getSourceFieldMappings();
    }

    @Test
    @DisplayName("getDynamicDocuments should delegate to feign client")
    void getDynamicDocuments_delegates() {
        KwDynamicDocType doc = new KwDynamicDocType("d1", "Doc 1", List.of());
        when(iesKwApiClient.getDynamicDocuments("TYPE", "SUB")).thenReturn(List.of(doc));

        assertThat(kwIntegrationService.getDynamicDocuments("TYPE", "SUB")).containsExactly(doc);
        verify(iesKwApiClient).getDynamicDocuments("TYPE", "SUB");
    }

    @Test
    @DisplayName("getItemSubtypesList should delegate to feign client")
    void getItemSubtypesList_delegates() {
        KwItemSubtypeDto dto = new KwItemSubtypeDto("A", "Alpha");
        when(iesKwApiClient.getItemSubtypes()).thenReturn(List.of(dto));

        assertThat(kwIntegrationService.getItemSubtypesList()).containsExactly(dto);
        verify(iesKwApiClient).getItemSubtypes();
    }
}
