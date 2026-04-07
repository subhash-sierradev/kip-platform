package com.integration.management.ies.client;

import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "execution-integration-connection")
public interface IesConnectionClient {

    @PostMapping("/api/integrations/connections/test-connection")
    ResponseEntity<ConnectionTestResponse> testAndCreateConnection(@RequestBody IntegrationConnectionRequest request);

    @PostMapping("/api/integrations/connections/{connectionId}/test")
    ConnectionTestResponse testExistingConnection(
            @PathVariable UUID connectionId, @RequestBody ConnectionReTestRequest request);

    @PutMapping("/api/integrations/connections/{connectionId}/secret")
    void rotateConnectionSecret(@PathVariable UUID connectionId,
                                @RequestBody IntegrationConnectionSecretRotateRequest request);

}
