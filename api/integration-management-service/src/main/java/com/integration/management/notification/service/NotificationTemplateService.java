package com.integration.management.notification.service;

import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationMapper notificationMapper;

    @Cacheable(value = "notificationTemplatesByTenantCache", key = "#tenantId")
    public List<NotificationTemplateResponse> getTemplatesForTenant(String tenantId) {
        log.debug("Fetching notification templates for tenant: {}", tenantId);
        return notificationTemplateRepository.findByTenantId(tenantId)
            .stream()
            .map(notificationMapper::toTemplateResponse)
            .toList();
    }
}
