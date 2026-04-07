package com.integration.management.ies.client;

import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "execution-confluence-api")
public interface IesConfluenceApiClient {

    @GetMapping("/api/integrations/confluence/connections/{secretName}/spaces")
    List<ConfluenceSpaceDto> getSpaces(@PathVariable String secretName);

    @GetMapping("/api/integrations/confluence/connections/{secretName}/spaces/{spaceKey}/pages")
    List<ConfluencePageDto> getPages(@PathVariable String secretName, @PathVariable String spaceKey);
}
