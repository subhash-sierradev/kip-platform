package com.integration.management.ies.client;

import com.integration.execution.contract.rest.response.arcgis.ArcGISVerificationPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// TODO KIP-547 REMOVE — Temporary Feign client for ArcGIS feature service verification
@FeignClient(name = "execution-arcgis-verification")
public interface IesArcGISVerificationClient {

    @GetMapping("/api/execution/arcgis/verification/features")
    ArcGISVerificationPageResponse getFeatures(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "objectId", required = false) String objectId,
            @RequestParam(name = "locationId", required = false) String locationId);
}
