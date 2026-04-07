package com.integration.execution.controller;

import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/confluence")
@PreAuthorize("hasRole('feature_confluence_integration')")
public class ConfluenceLookupController {

    private final ConfluenceApiClient confluenceApiClient;

    @GetMapping("/connections/{secretName}/spaces")
    public ResponseEntity<List<ConfluenceSpaceDto>> getSpaces(
            @PathVariable @NotBlank String secretName) {
        log.info("Fetching Confluence spaces for secretName: {}", secretName);
        return ResponseEntity.ok(confluenceApiClient.getSpaces(secretName));
    }

    @GetMapping("/connections/{secretName}/spaces/{spaceKey}/pages")
    public ResponseEntity<List<ConfluencePageDto>> getPages(
            @PathVariable @NotBlank String secretName,
            @PathVariable @NotBlank String spaceKey) {
        log.info("Fetching Confluence pages for secretName: {}, spaceKey: {}", secretName, spaceKey);
        return ResponseEntity.ok(confluenceApiClient.getPages(secretName, spaceKey));
    }
}
