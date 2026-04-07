package com.integration.execution.contract.rest.response;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResponse {

    private boolean success;
    private int statusCode;
    private String message;
    private String secretName;
    private ConnectionStatus connectionStatus;
    private Instant lastConnectionTest;
}
