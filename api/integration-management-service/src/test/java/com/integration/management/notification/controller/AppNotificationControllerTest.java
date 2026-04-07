package com.integration.management.notification.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.execution.contract.model.enums.NotificationReadFilter;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationCountResponse;
import com.integration.management.notification.service.AppNotificationService;
import com.integration.management.notification.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppNotificationController")
class AppNotificationControllerTest {

    private static final String TENANT_ID = "tenant-xyz";
    private static final String USER_ID = "user-xyz";
    private static final String BASE_URL = "/api/management/notifications";

    @Mock
    private AppNotificationService appNotificationService;

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @InjectMocks
    private AppNotificationController appNotificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(appNotificationController)
            .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Nested
    @DisplayName("GET notifications")
    class GetNotifications {

        @Test
        @DisplayName("should return 200 with page of notifications")
        void get_notifications_returns_ok() throws Exception {
            AppNotificationResponse resp = AppNotificationResponse.builder()
                    .id(UUID.randomUUID()).tenantId(TENANT_ID).build();
            when(appNotificationService.getNotifications(
                    eq(TENANT_ID),
                    eq(USER_ID),
                    any(),
                    eq(NotificationReadFilter.ALL),
                    isNull()))
                    .thenReturn(new PageImpl<>(List.of(resp), PageRequest.of(0, 20), 1));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("should pass read and severity filters")
        void get_notifications_with_filters_returns_ok() throws Exception {
            AppNotificationResponse resp = AppNotificationResponse.builder()
                    .id(UUID.randomUUID()).tenantId(TENANT_ID).build();

            when(appNotificationService.getNotifications(
                    eq(TENANT_ID),
                    eq(USER_ID),
                    any(),
                    eq(NotificationReadFilter.UNREAD),
                    eq(NotificationSeverity.ERROR)))
                    .thenReturn(new PageImpl<>(List.of(resp), PageRequest.of(0, 20), 1));

            mockMvc.perform(get(BASE_URL)
                    .queryParam("readFilter", "UNREAD")
                    .queryParam("severity", "ERROR")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET count")
    class GetCount {

        @Test
        @DisplayName("should return 200 with tab counts")
        void get_count_returns_ok() throws Exception {
            when(appNotificationService.getNotificationCounts(TENANT_ID, USER_ID))
                .thenReturn(NotificationCountResponse.builder()
                    .allCount(15L)
                    .unreadCount(5L)
                    .readCount(10L)
                    .errorCount(2L)
                    .infoCount(6L)
                    .successCount(4L)
                    .warningCount(3L)
                    .build());

            mockMvc.perform(get(BASE_URL + "/count")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allCount").value(15))
                .andExpect(jsonPath("$.unreadCount").value(5));
        }
    }

    @Nested
    @DisplayName("GET stream (SSE)")
    class StreamNotifications {

        @Test
        @DisplayName("should return 200 and SSE emitter for stream endpoint")
        void stream_notifications_returns_ok() throws Exception {
            SseEmitter emitter = new SseEmitter(0L);
            when(sseEmitterRegistry.register(USER_ID)).thenReturn(emitter);

            mockMvc.perform(get(BASE_URL + "/stream")
                    .requestAttr(X_USER_ID, USER_ID)
                    .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

            verify(sseEmitterRegistry).register(USER_ID);
        }
    }

    @Nested
    @DisplayName("PATCH read")
    class MarkRead {

        @Test
        @DisplayName("should return 204 when marking notifications as read")
        void markRead_validRequest_returnsNoContent() throws Exception {
            UUID notificationId = UUID.randomUUID();
            when(appNotificationService.markAsRead(eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(1);

            mockMvc.perform(patch(BASE_URL + "/read")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"notificationIds\":[\"" + notificationId + "\"]}"))
                .andExpect(status().isNoContent());

            verify(appNotificationService).markAsRead(eq(TENANT_ID), eq(USER_ID), any());
        }
    }

    @Nested
    @DisplayName("PATCH read/all")
    class MarkAllRead {

        @Test
        @DisplayName("should return 204 when marking all notifications as read")
        void markAllRead_returnsNoContent() throws Exception {
            when(appNotificationService.markAllAsRead(TENANT_ID, USER_ID)).thenReturn(2);

            mockMvc.perform(patch(BASE_URL + "/read/all")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                .andExpect(status().isNoContent());

            verify(appNotificationService).markAllAsRead(TENANT_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("DELETE operations")
    class DeleteOperations {

        @Test
        @DisplayName("should return 204 when deleting one notification")
        void deleteSingle_validRequest_returnsNoContent() throws Exception {
            UUID notificationId = UUID.randomUUID();
            when(appNotificationService.deleteById(TENANT_ID, USER_ID, notificationId)).thenReturn(1);

            mockMvc.perform(delete(BASE_URL + "/{id}", notificationId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                .andExpect(status().isNoContent());

            verify(appNotificationService).deleteById(TENANT_ID, USER_ID, notificationId);
        }

        @Test
        @DisplayName("should return 204 when deleting all notifications")
        void deleteAll_validRequest_returnsNoContent() throws Exception {
            when(appNotificationService.deleteAll(TENANT_ID, USER_ID)).thenReturn(3);

            mockMvc.perform(delete(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                .andExpect(status().isNoContent());

            verify(appNotificationService).deleteAll(TENANT_ID, USER_ID);
        }
    }
}
