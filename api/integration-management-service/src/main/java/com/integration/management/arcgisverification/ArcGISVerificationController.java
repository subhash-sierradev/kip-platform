package com.integration.management.arcgisverification;

import com.integration.execution.contract.rest.response.arcgis.ArcGISVerificationPageResponse;
import com.integration.management.ies.client.IesArcGISVerificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO KIP-547 REMOVE — Temporary admin endpoint for ArcGIS feature service verification
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/management/arcgis/verification")
@PreAuthorize("hasRole('tenant_admin') and hasRole('feature_arcgis_integration')")
public class ArcGISVerificationController {

    private final IesArcGISVerificationClient iesArcGISVerificationClient;

    @GetMapping("/features")
    public ResponseEntity<ArcGISVerificationPageResponse> getFeatures(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String objectId,
            @RequestParam(required = false) String locationId) {

        log.info("ArcGIS verification: offset={}, objectId={}, locationId={}",
                offset, objectId, locationId);

        ArcGISVerificationPageResponse response =
                iesArcGISVerificationClient.getFeatures(offset, objectId, locationId);
        return ResponseEntity.ok(response);
    }
}

