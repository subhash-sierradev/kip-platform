package com.integration.execution.controller;

import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
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
class ArcGISFieldControllerTest {

    @Mock
    private ArcGISApiClient arcGISApiClient;

    private ArcGISFieldController controller;

    @BeforeEach
    void setUp() {
        controller = new ArcGISFieldController(arcGISApiClient);
    }

    @Test
    void fetchArcGISFields_validSecretName_returnsFields() {
        List<ArcGISFieldDto> fields = List.of(
                ArcGISFieldDto.builder().name("OBJECTID").type("esriFieldTypeOID").build(),
                ArcGISFieldDto.builder().name("CaseNumber").type("esriFieldTypeString").build()
        );
        when(arcGISApiClient.fetchArcGISFields("secret")).thenReturn(fields);

        ResponseEntity<List<ArcGISFieldDto>> response = controller.fetchArcGISFields("secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(fields);
        verify(arcGISApiClient).fetchArcGISFields("secret");
    }
}
