package com.integration.management.model.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDependentItemResponse {
    private String id;
    private String name;
    private Boolean isEnabled;
    private String description;
    private Instant lastRunAt;
}
