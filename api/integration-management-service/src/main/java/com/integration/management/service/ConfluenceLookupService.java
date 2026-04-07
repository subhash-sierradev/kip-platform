package com.integration.management.service;

import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.management.ies.client.IesConfluenceApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for Confluence resource lookups (spaces, pages) via the execution service.
 * Provides cached read-only access to external Confluence data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceLookupService {

    private final IesConfluenceApiClient iesConfluenceApiClient;
    private final IntegrationConnectionService integrationConnectionService;

    @Cacheable(value = "confluenceSpacesByConnectionCache", key = "#connectionId + ':' + #tenantId")
    public List<ConfluenceSpaceDto> getSpacesByConnectionId(final UUID connectionId, final String tenantId) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesConfluenceApiClient.getSpaces(secretName);
    }

    @Cacheable(value = "confluencePagesByConnectionCache",
            key = "#connectionId + ':' + #tenantId + ':' + #spaceKey")
    public List<ConfluencePageDto> getPagesByConnectionIdAndSpaceKey(
            final UUID connectionId, final String tenantId, final String spaceKey) {
        String secretName = integrationConnectionService
                .getIntegrationConnectionNameById(connectionId.toString(), tenantId);
        return iesConfluenceApiClient.getPages(secretName, spaceKey);
    }
}
