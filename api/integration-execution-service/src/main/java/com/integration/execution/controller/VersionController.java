package com.integration.execution.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/execution")
public class VersionController {

    private final BuildProperties buildProperties;

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        Map<String, String> versionInfo = new LinkedHashMap<>();
        versionInfo.put("version", buildProperties.getVersion());
        versionInfo.put("name", buildProperties.getName());
        versionInfo.put("buildTime", buildProperties.getTime().toString());
        return ResponseEntity.ok(versionInfo);
    }
}
