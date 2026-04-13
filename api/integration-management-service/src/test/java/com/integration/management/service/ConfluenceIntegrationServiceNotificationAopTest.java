package com.integration.management.service;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.management.notification.aop.NotificationAspect;
import com.integration.management.notification.aop.NotificationMetadataProvider;
import com.integration.management.notification.aop.PublishNotification;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ConfluenceIntegrationService - @PublishNotification wiring")
class ConfluenceIntegrationServiceNotificationAopTest {

    @Nested
    @DisplayName("triggerJobExecution annotation")
    class TriggerJobExecutionAnnotation {

        private PublishNotification resolveAnnotation() throws NoSuchMethodException {
            Method method = ConfluenceIntegrationService.class.getMethod(
                    "triggerJobExecution", UUID.class, String.class, String.class);
            return method.getAnnotation(PublishNotification.class);
        }

        @Test
        @DisplayName("annotation is present on method")
        void annotation_is_present() throws NoSuchMethodException {
            assertThat(resolveAnnotation()).isNotNull();
        }

        @Test
        @DisplayName("event key is CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN")
        void event_key_is_confluence_job_adhoc_run() throws NoSuchMethodException {
            assertThat(resolveAnnotation().eventKey())
                    .isEqualTo(NotificationEventKey.CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN);
        }

        @Test
        @DisplayName("metadata provider bean name is confluenceNotificationMetadataProvider")
        void metadata_provider_bean_is_confluence_provider() throws NoSuchMethodException {
            assertThat(resolveAnnotation().metadataProvider())
                    .isEqualTo("confluenceNotificationMetadataProvider");
        }

        @Test
        @DisplayName("entity ID SpEL resolves from integrationId parameter")
        void entity_id_spel_resolves_integration_id_parameter() throws NoSuchMethodException {
            assertThat(resolveAnnotation().entityId())
                    .isEqualTo("#integrationId");
        }

        @Test
        @DisplayName("tenant ID and user ID use default SpEL expressions")
        void tenant_and_user_spel_use_defaults() throws NoSuchMethodException {
            PublishNotification ann = resolveAnnotation();
            assertThat(ann.tenantId()).isEqualTo("#tenantId");
            assertThat(ann.userId()).isEqualTo("#userId");
        }
    }

    @Nested
    @DisplayName("triggerJobExecution - AOP publishes notification event")
    @ExtendWith(MockitoExtension.class)
    class TriggerJobExecutionAopBehaviour {

        private static final UUID INTEGRATION_ID = UUID.randomUUID();
        private static final String TENANT_ID = "tenant-abc";
        private static final String USER_ID = "user-xyz";

        @Mock
        private NotificationEventPublisher notificationEventPublisher;
        @Mock
        private ApplicationContext applicationContext;
        @Mock
        private ProceedingJoinPoint joinPoint;
        @Mock
        private MethodSignature methodSignature;
        @Mock
        private NotificationMetadataProvider metadataProvider;

        @InjectMocks
        private NotificationAspect notificationAspect;

        private PublishNotification annotation;

        @BeforeEach
        void setUp() throws Throwable {
            Method method = ConfluenceIntegrationService.class.getMethod(
                    "triggerJobExecution", UUID.class, String.class, String.class);
            annotation = method.getAnnotation(PublishNotification.class);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(methodSignature.getParameterNames())
                    .thenReturn(new String[]{"integrationId", "tenantId", "userId"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{INTEGRATION_ID, TENANT_ID, USER_ID});
            when(joinPoint.proceed()).thenReturn(null);

            when(applicationContext.getBean(
                    "confluenceNotificationMetadataProvider", NotificationMetadataProvider.class))
                    .thenReturn(metadataProvider);
            when(metadataProvider.resolve(INTEGRATION_ID, TENANT_ID))
                    .thenReturn(Map.of(
                            "integrationName", "My Confluence Report",
                            "integrationId", INTEGRATION_ID.toString()));
        }

        @Test
        @DisplayName("publishes event with CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN key")
        void publishes_event_with_correct_event_key() throws Throwable {
            notificationAspect.around(joinPoint, annotation);

            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publishAfterCommit(captor.capture());
            assertThat(captor.getValue().getEventKey())
                    .isEqualTo(NotificationEventKey.CONFLUENCE_INTEGRATION_JOB_ADHOC_RUN.name());
        }

        @Test
        @DisplayName("confluenceNotificationMetadataProvider is called with integration ID and tenant ID")
        void metadata_provider_called_with_integration_id_and_tenant_id() throws Throwable {
            notificationAspect.around(joinPoint, annotation);

            verify(metadataProvider).resolve(INTEGRATION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("published event contains metadata returned by confluenceNotificationMetadataProvider")
        void published_event_contains_provider_metadata() throws Throwable {
            notificationAspect.around(joinPoint, annotation);

            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publishAfterCommit(captor.capture());
            assertThat(captor.getValue().getMetadata())
                    .containsEntry("integrationName", "My Confluence Report")
                    .containsEntry("integrationId", INTEGRATION_ID.toString());
        }

        @Test
        @DisplayName("published event carries tenant ID and user ID resolved from method arguments")
        void published_event_carries_tenant_and_user_ids() throws Throwable {
            notificationAspect.around(joinPoint, annotation);

            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publishAfterCommit(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captor.getValue().getTriggeredByUserId()).isEqualTo(USER_ID);
        }
    }
}
