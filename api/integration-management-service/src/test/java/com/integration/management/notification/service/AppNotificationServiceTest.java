package com.integration.management.notification.service;

import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.notification.entity.AppNotification;
import com.integration.execution.contract.model.enums.NotificationReadFilter;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.request.MarkNotificationsReadRequest;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationCountResponse;
import com.integration.management.notification.repository.AppNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppNotificationService")
class AppNotificationServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-1";

    @Mock
    private AppNotificationRepository appNotificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private AppNotificationService appNotificationService;

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("maps page of entities to DTOs")
        void maps_page_of_entities_to_dtos() {
            AppNotification entity = buildNotification();
            AppNotificationResponse response = buildResponse();
            Pageable pageable = PageRequest.of(0, 20);
            Page<AppNotification> entityPage = new PageImpl<>(List.of(entity));

            when(appNotificationRepository
                    .findByTenantIdAndUserIdWithFilters(TENANT_ID, USER_ID, null, null, pageable))
                    .thenReturn(entityPage);
            when(notificationMapper.toAppNotificationResponse(entity)).thenReturn(response);

            Page<AppNotificationResponse> result =
                    appNotificationService.getNotifications(
                            TENANT_ID,
                            USER_ID,
                            pageable,
                            NotificationReadFilter.ALL,
                            null);

            assertThat(result.getContent()).containsExactly(response);
            verify(appNotificationRepository)
                    .findByTenantIdAndUserIdWithFilters(TENANT_ID, USER_ID, null, null, pageable);
        }

        @Test
        @DisplayName("returns empty page when no notifications exist")
        void returns_empty_page_when_none_exist() {
            Pageable pageable = PageRequest.of(0, 20);
            when(appNotificationRepository
                    .findByTenantIdAndUserIdWithFilters(TENANT_ID, USER_ID, null, null, pageable))
                    .thenReturn(Page.empty());

            Page<AppNotificationResponse> result =
                    appNotificationService.getNotifications(
                            TENANT_ID,
                            USER_ID,
                            pageable,
                            NotificationReadFilter.ALL,
                            null);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("applies read and severity filters")
        void applies_read_and_severity_filters() {
            Pageable pageable = PageRequest.of(0, 20);
            when(appNotificationRepository.findByTenantIdAndUserIdWithFilters(
                    TENANT_ID,
                    USER_ID,
                    Boolean.FALSE,
                    NotificationSeverity.ERROR,
                    pageable))
                    .thenReturn(Page.empty());

            appNotificationService.getNotifications(
                    TENANT_ID,
                    USER_ID,
                    pageable,
                    NotificationReadFilter.UNREAD,
                    NotificationSeverity.ERROR);

            verify(appNotificationRepository).findByTenantIdAndUserIdWithFilters(
                    TENANT_ID,
                    USER_ID,
                    Boolean.FALSE,
                    NotificationSeverity.ERROR,
                    pageable);
        }

        @Test
        @DisplayName("applies read-only filter")
        void applies_read_only_filter() {
            Pageable pageable = PageRequest.of(0, 20);
            when(appNotificationRepository.findByTenantIdAndUserIdWithFilters(
                    TENANT_ID,
                    USER_ID,
                    Boolean.TRUE,
                    null,
                    pageable))
                    .thenReturn(Page.empty());

            appNotificationService.getNotifications(
                    TENANT_ID,
                    USER_ID,
                    pageable,
                    NotificationReadFilter.READ,
                    null);

            verify(appNotificationRepository).findByTenantIdAndUserIdWithFilters(
                    TENANT_ID,
                    USER_ID,
                    Boolean.TRUE,
                    null,
                    pageable);
        }
    }

    @Nested
    @DisplayName("getNotificationCounts")
    class GetNotificationCounts {

        @Test
        @DisplayName("returns counts for all tabs")
        void returns_counts_for_all_tabs() {
            NotificationCountResponse expected = NotificationCountResponse.builder()
                    .allCount(12L)
                    .unreadCount(5L)
                    .readCount(7L)
                    .errorCount(2L)
                    .infoCount(4L)
                    .successCount(3L)
                    .warningCount(3L)
                    .build();
            when(appNotificationRepository.countAllGrouped(TENANT_ID, USER_ID)).thenReturn(expected);

            NotificationCountResponse result =
                    appNotificationService.getNotificationCounts(TENANT_ID, USER_ID);

            assertThat(result.getAllCount()).isEqualTo(12L);
            assertThat(result.getUnreadCount()).isEqualTo(5L);
            assertThat(result.getReadCount()).isEqualTo(7L);
            assertThat(result.getErrorCount()).isEqualTo(2L);
            assertThat(result.getInfoCount()).isEqualTo(4L);
            assertThat(result.getSuccessCount()).isEqualTo(3L);
            assertThat(result.getWarningCount()).isEqualTo(3L);
            verify(appNotificationRepository).countAllGrouped(TENANT_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("delegates to repository and returns updated row count")
        void delegates_to_repository_and_returns_row_count() {
            List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
            MarkNotificationsReadRequest request =
                    MarkNotificationsReadRequest.builder().notificationIds(ids).build();

            when(appNotificationRepository.markAsReadByIds(TENANT_ID, USER_ID, ids)).thenReturn(2);

            int result = appNotificationService.markAsRead(TENANT_ID, USER_ID, request);

            assertThat(result).isEqualTo(2);
            verify(appNotificationRepository).markAsReadByIds(TENANT_ID, USER_ID, ids);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("marks all notifications as read for user")
        void marks_all_notifications_as_read_for_user() {
            when(appNotificationRepository.markAllAsReadByUser(TENANT_ID, USER_ID)).thenReturn(5);

            int result = appNotificationService.markAllAsRead(TENANT_ID, USER_ID);

            assertThat(result).isEqualTo(5);
            verify(appNotificationRepository).markAllAsReadByUser(TENANT_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("delegates to repository and returns rows deleted")
        void delegates_to_repository_and_returns_rows_deleted() {
            UUID id = UUID.randomUUID();
            when(appNotificationRepository.deleteByIdForUser(TENANT_ID, USER_ID, id)).thenReturn(1);

            int result = appNotificationService.deleteById(TENANT_ID, USER_ID, id);

            assertThat(result).isEqualTo(1);
            verify(appNotificationRepository).deleteByIdForUser(TENANT_ID, USER_ID, id);
        }

        @Test
        @DisplayName("returns zero when notification not found")
        void returns_zero_when_not_found() {
            UUID id = UUID.randomUUID();
            when(appNotificationRepository.deleteByIdForUser(TENANT_ID, USER_ID, id)).thenReturn(0);

            int result = appNotificationService.deleteById(TENANT_ID, USER_ID, id);

            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("delegates to repository and returns count")
        void delegates_to_repository_and_returns_count() {
            when(appNotificationRepository
                    .deleteAllByTenantIdAndUserId(TENANT_ID, USER_ID))
                    .thenReturn(3);

            int result = appNotificationService.deleteAll(TENANT_ID, USER_ID);

            assertThat(result).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("saves and returns persisted notification")
        void saves_and_returns_persisted_notification() {
            AppNotification notification = buildNotification();
            when(appNotificationRepository.save(notification)).thenReturn(notification);

            AppNotification result = appNotificationService.save(notification);

            assertThat(result).isSameAs(notification);
            verify(appNotificationRepository).save(notification);
        }
    }

    private AppNotification buildNotification() {
        return AppNotification.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .type(NotificationEventKey.SITE_CONFIG_UPDATED)
                .severity(NotificationSeverity.INFO)
                .title("Test")
                .message("Test message")
                .isRead(false)
                .build();
    }

    private AppNotificationResponse buildResponse() {
        return AppNotificationResponse.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .title("Test")
                .build();
    }
}
