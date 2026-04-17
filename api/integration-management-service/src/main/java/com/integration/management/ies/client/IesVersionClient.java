package com.integration.management.ies.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "execution-version")
public interface IesVersionClient {

    @GetMapping("/api/execution/version")
    Map<String, String> getVersion();
}
