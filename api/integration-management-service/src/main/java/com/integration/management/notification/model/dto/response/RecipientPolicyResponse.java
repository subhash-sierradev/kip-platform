package com.integration.management.notification.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientPolicyResponse {

    private UUID id;
    private String tenantId;
    private UUID ruleId;
    private String eventKey;
    private String recipientType;
    private List<RecipientUserInfo> users;
    private Instant createdDate;
    private String createdBy;
    private Instant lastModifiedDate;
    private String lastModifiedBy;
}
