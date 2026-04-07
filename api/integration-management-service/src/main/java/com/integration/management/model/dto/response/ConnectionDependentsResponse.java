package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDependentsResponse {
    private ServiceType serviceType;

    @Builder.Default
    private List<ConnectionDependentItemResponse> integrations = new ArrayList<>();

    public boolean hasAnyDependents() {
        return integrations != null && !integrations.isEmpty();
    }
}
