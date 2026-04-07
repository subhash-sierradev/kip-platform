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
public class ServiceConnectionResponse {
    private String id;
    private int statusCode;
    private boolean success;
    private String message;
    private ConnectionStatus lastConnectionStatus;
    private Instant lastConnectionTest;
}
