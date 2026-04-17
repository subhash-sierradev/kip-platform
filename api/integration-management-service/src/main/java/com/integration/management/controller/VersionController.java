package com.integration.management.controller;

import com.integration.management.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/management")
public class VersionController {

    private final VersionService versionService;

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersion() {
        return ResponseEntity.ok(versionService.getVersionInfo());
    }
}
