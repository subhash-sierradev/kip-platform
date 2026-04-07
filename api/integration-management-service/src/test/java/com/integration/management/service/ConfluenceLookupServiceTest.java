package com.integration.management.service;

import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.management.ies.client.IesConfluenceApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceLookupService")
class ConfluenceLookupServiceTest {

    @Mock
    private IesConfluenceApiClient iesConfluenceApiClient;

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    @InjectMocks
    private ConfluenceLookupService service;

    @Test
    @DisplayName("getSpacesByConnectionId resolves secret and delegates to client")
    void getSpacesByConnectionId_resolvesSecretAndDelegates() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<ConfluenceSpaceDto> expected = List.of(mock(ConfluenceSpaceDto.class));
        when(iesConfluenceApiClient.getSpaces("secret-1")).thenReturn(expected);

        List<ConfluenceSpaceDto> actual = service.getSpacesByConnectionId(connectionId, "tenant-1");

        assertThat(actual).isSameAs(expected);
        verify(iesConfluenceApiClient).getSpaces("secret-1");
    }

    @Test
    @DisplayName("getSpacesByConnectionId uses resolved connection name when looking up spaces")
    void getSpacesByConnectionId_usesConnectionNameFromService() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-2"))
                .thenReturn("my-confluence-secret");
        when(iesConfluenceApiClient.getSpaces("my-confluence-secret")).thenReturn(List.of());

        service.getSpacesByConnectionId(connectionId, "tenant-2");

        verify(integrationConnectionService).getIntegrationConnectionNameById(connectionId.toString(), "tenant-2");
        verify(iesConfluenceApiClient).getSpaces("my-confluence-secret");
    }

    @Test
    @DisplayName("getPagesByConnectionIdAndSpaceKey resolves secret and delegates to client")
    void getPagesByConnectionIdAndSpaceKey_resolvesSecretAndDelegates() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        List<ConfluencePageDto> expected = List.of(mock(ConfluencePageDto.class));
        when(iesConfluenceApiClient.getPages("secret-1", "MYSPACE")).thenReturn(expected);

        List<ConfluencePageDto> actual = service.getPagesByConnectionIdAndSpaceKey(connectionId, "tenant-1",
                "MYSPACE");

        assertThat(actual).isSameAs(expected);
        verify(iesConfluenceApiClient).getPages("secret-1", "MYSPACE");
    }

    @Test
    @DisplayName("getPagesByConnectionIdAndSpaceKey passes spaceKey to client")
    void getPagesByConnectionIdAndSpaceKey_passesSpaceKeyToClient() {
        UUID connectionId = UUID.randomUUID();
        when(integrationConnectionService.getIntegrationConnectionNameById(connectionId.toString(), "tenant-1"))
                .thenReturn("secret-1");
        when(iesConfluenceApiClient.getPages("secret-1", "DEVSPACE")).thenReturn(List.of());

        service.getPagesByConnectionIdAndSpaceKey(connectionId, "tenant-1", "DEVSPACE");

        verify(iesConfluenceApiClient).getPages("secret-1", "DEVSPACE");
    }
}
