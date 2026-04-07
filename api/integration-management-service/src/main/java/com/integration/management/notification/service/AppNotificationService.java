package com.integration.management.notification.service;

import com.integration.execution.contract.model.enums.NotificationReadFilter;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.request.MarkNotificationsReadRequest;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationCountResponse;
import com.integration.management.notification.repository.AppNotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppNotificationService {

    private final AppNotificationRepository appNotificationRepository;
    private final NotificationMapper notificationMapper;

    public Page<AppNotificationResponse> getNotifications(
            String tenantId,
            String userId,
            Pageable pageable,
            NotificationReadFilter readFilter,
            NotificationSeverity severityFilter) {
        Boolean isReadFilter = toReadFlag(readFilter);
        return appNotificationRepository
                .findByTenantIdAndUserIdWithFilters(
                        tenantId,
                        userId,
                        isReadFilter,
                        severityFilter,
                        pageable)
                .map(notificationMapper::toAppNotificationResponse);
    }

    public NotificationCountResponse getNotificationCounts(String tenantId, String userId) {
        log.debug("Fetching notification counts for tenant: {}, user: {}", tenantId, userId);
        return appNotificationRepository.countAllGrouped(tenantId, userId);
    }

    private Boolean toReadFlag(NotificationReadFilter readFilter) {
        if (readFilter == null || readFilter == NotificationReadFilter.ALL) {
            return null;
        }
        return readFilter == NotificationReadFilter.READ;
    }

    @Transactional
    public int markAsRead(String tenantId, String userId, MarkNotificationsReadRequest request) {
        log.info("Marking {} notifications as read for tenant: {}, user: {}",
                request.getNotificationIds().size(), tenantId, userId);
        return appNotificationRepository.markAsReadByIds(
                tenantId, userId, request.getNotificationIds());
    }

    @Transactional
    public int markAllAsRead(String tenantId, String userId) {
        log.info("Marking all notifications as read for tenant: {}, user: {}", tenantId, userId);
        return appNotificationRepository.markAllAsReadByUser(tenantId, userId);
    }

    @Transactional
    public int deleteById(String tenantId, String userId, UUID id) {
        log.debug("Deleting notification {} for tenant: {}, user: {}", id, tenantId, userId);
        return appNotificationRepository.deleteByIdForUser(tenantId, userId, id);
    }

    @Transactional
    public int deleteAll(String tenantId, String userId) {
        log.debug("Deleting all notifications for tenant: {}, user: {}", tenantId, userId);
        return appNotificationRepository.deleteAllByTenantIdAndUserId(tenantId, userId);
    }

    @Transactional
    public AppNotification save(AppNotification notification) {
        log.debug("Persisting notification for tenant: {}, user: {}",
                notification.getTenantId(), notification.getUserId());
        return appNotificationRepository.save(notification);
    }
}
