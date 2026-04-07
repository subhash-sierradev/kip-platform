package com.integration.execution.controller;

import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/arcgis")
@PreAuthorize("hasRole('feature_arcgis_integration')")
public class ArcGISFieldController {

    private final ArcGISApiClient arcGISApiClient;

    @GetMapping("/connections/{secretName}/features")
    public ResponseEntity<List<ArcGISFieldDto>> fetchArcGISFields(
            @PathVariable("secretName") String secretName) {
        log.info("Fetching ArcGIS fields for secretName: {}", secretName);
        return ResponseEntity.ok(arcGISApiClient.fetchArcGISFields(secretName));
    }
}
