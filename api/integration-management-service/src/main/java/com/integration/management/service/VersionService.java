package com.integration.management.service;

import com.integration.management.ies.client.IesVersionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private static final String UNAVAILABLE = "unavailable";

    private final BuildProperties buildProperties;
    private final IesVersionClient iesVersionClient;

    public Map<String, Object> getVersionInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ims", buildImsVersion());
        result.put("ies", fetchIesVersion());
        return result;
    }

    private Map<String, String> buildImsVersion() {
        Map<String, String> ims = new LinkedHashMap<>();
        ims.put("version", buildProperties.getVersion());
        ims.put("name", buildProperties.getName());
        ims.put("buildTime", buildProperties.getTime().toString());
        return ims;
    }

    @Cacheable("iesVersionCache")
    public Map<String, String> fetchIesVersion() {
        try {
            return iesVersionClient.getVersion();
        } catch (Exception ex) {
            log.warn("Failed to fetch IES version: {}", ex.getMessage());
            Map<String, String> fallback = new LinkedHashMap<>();
            fallback.put("version", UNAVAILABLE);
            fallback.put("name", "integration-execution-service");
            return fallback;
        }
    }
}
