package com.integration.execution.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatsDto {
    private int expectedJobs;
    private int actualJobs;
    private String lastChecked;
}

