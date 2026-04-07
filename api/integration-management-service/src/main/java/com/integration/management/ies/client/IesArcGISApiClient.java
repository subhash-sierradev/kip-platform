package com.integration.management.ies.client;

import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "execution-arcgis-feature-layer")
public interface IesArcGISApiClient {

    @GetMapping("/api/integrations/arcgis/connections/{secretName}/features")
    List<ArcGISFieldDto> fetchArcGISFields(@PathVariable String secretName);
}
