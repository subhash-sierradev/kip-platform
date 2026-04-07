package com.integration.execution.controller;

import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfluenceLookupControllerTest {

    @Mock
    private ConfluenceApiClient confluenceApiClient;

    private ConfluenceLookupController controller;

    @BeforeEach
    void setUp() {
        controller = new ConfluenceLookupController(confluenceApiClient);
    }

    @Test
    void getSpaces_validSecretName_returnsSpaceList() {
        List<ConfluenceSpaceDto> spaces = List.of(
                ConfluenceSpaceDto.builder().key("DEV").name("Development").type("global").build(),
                ConfluenceSpaceDto.builder().key("OPS").name("Operations").type("global").build());
        when(confluenceApiClient.getSpaces("my-secret")).thenReturn(spaces);

        ResponseEntity<List<ConfluenceSpaceDto>> response = controller.getSpaces("my-secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(spaces);
        verify(confluenceApiClient).getSpaces("my-secret");
    }

    @Test
    void getPages_validSecretNameAndSpaceKey_returnsPageList() {
        List<ConfluencePageDto> pages = List.of(
                ConfluencePageDto.builder().id("101").title("Getting Started").type("folder").build());
        when(confluenceApiClient.getPages("my-secret", "DEV")).thenReturn(pages);

        ResponseEntity<List<ConfluencePageDto>> response = controller.getPages("my-secret", "DEV");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pages);
        verify(confluenceApiClient).getPages("my-secret", "DEV");
    }

    @Test
    void getSpaces_emptyResult_returnsOkWithEmptyList() {
        when(confluenceApiClient.getSpaces("empty-secret")).thenReturn(List.of());

        ResponseEntity<List<ConfluenceSpaceDto>> response = controller.getSpaces("empty-secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
