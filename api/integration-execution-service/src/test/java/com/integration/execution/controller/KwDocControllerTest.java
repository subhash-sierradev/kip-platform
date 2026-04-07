package com.integration.execution.controller;

import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.service.KwGraphQLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwDocControllerTest {

    @Mock
    private KwGraphQLService kwGraphQLService;

    private KwDocController controller;

    @BeforeEach
    void setUp() {
        controller = new KwDocController(kwGraphQLService);
    }

    @Test
    void getDynamicDocuments_validTypeAndSubType_returnsDocuments() {
        List<KwDynamicDocType> docs = List.of(new KwDynamicDocType("1", "Incident", List.of()));
        when(kwGraphQLService.fetchDynamicDocumentsTypes("DOCUMENT", "INCIDENT")).thenReturn(docs);

        ResponseEntity<List<KwDynamicDocType>> response = controller.getDynamicDocuments("DOCUMENT", "INCIDENT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(docs);
        verify(kwGraphQLService).fetchDynamicDocumentsTypes("DOCUMENT", "INCIDENT");
    }

    @Test
    void getSubObjectLookups_serviceReturnsData_returnsItemSubtypes() {
        List<KwItemSubtypeDto> data = List.of(new KwItemSubtypeDto("REPORT", "Report"));
        when(kwGraphQLService.fetchItemSubtypes()).thenReturn(data);

        ResponseEntity<List<KwItemSubtypeDto>> response = controller.getSubObjectLookups();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(data);
        verify(kwGraphQLService).fetchItemSubtypes();
    }

    @Test
    void getFieldMappingData_serviceReturnsData_returnsFieldMappings() {
        List<KwDocField> fields = List.of(
                KwDocField.builder().id(1).fieldName("locationId").fieldType("Long").build()
        );
        when(kwGraphQLService.fetchFieldMappingForLocations()).thenReturn(fields);

        ResponseEntity<List<KwDocField>> response = controller.getFieldMappingData();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(fields);
        verify(kwGraphQLService).fetchFieldMappingForLocations();
    }
}
