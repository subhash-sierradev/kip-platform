package com.integration.management.notification.aop;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.model.enums.NotificationEventKey;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationAspect")
class NotificationAspectTest {

    @Mock private NotificationEventPublisher notificationEventPublisher;
    @Mock private ApplicationContext applicationContext;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature methodSignature;

    @InjectMocks
    private NotificationAspect notificationAspect;

    // A small helper class used to resolve a real Method object
    @SuppressWarnings("unused")
    private static final class SampleService {
        public String doWork(final String tenantId, final String userId) {
            return "result";
        }
    }

    private Method sampleMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        sampleMethod = SampleService.class.getMethod("doWork", String.class, String.class);
    }

    private void stubJoinPoint(String[] paramNames, Object[] args)
            throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(paramNames);
        when(methodSignature.getMethod()).thenReturn(sampleMethod);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("result");
    }

    private PublishNotification buildAnnotation() {
        PublishNotification ann = mock(PublishNotification.class);
        when(ann.eventKey()).thenReturn(NotificationEventKey.SITE_CONFIG_UPDATED);
        lenient().when(ann.tenantId()).thenReturn("#tenantId");
        lenient().when(ann.userId()).thenReturn("#userId");
        lenient().when(ann.metadata()).thenReturn("");
        lenient().when(ann.metadataProvider()).thenReturn("");
        return ann;
    }

    @Nested
    @DisplayName("around - success path")
    class AroundSuccess {

        @Test
        @DisplayName("publishes event when method succeeds")
        void publishes_event_on_success() throws Throwable {
            stubJoinPoint(new String[]{"tenantId", "userId"},
                    new Object[]{"tenant-1", "user-1"});
            PublishNotification ann = buildAnnotation();

            Object result = notificationAspect.around(joinPoint, ann);

            assertThat(result).isEqualTo("result");
            ArgumentCaptor<NotificationEvent> captor =
                    ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publish(captor.capture());
            assertThat(captor.getValue().getEventKey()).isEqualTo("SITE_CONFIG_UPDATED");
            assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-1");
            assertThat(captor.getValue().getTriggeredByUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("includes inline SpEL metadata in published event")
        void includes_inline_spel_metadata() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"tenantId", "userId"});
            when(methodSignature.getMethod()).thenReturn(sampleMethod);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"tenant-1", "user-1"});
            when(joinPoint.proceed()).thenReturn("my-config");

            PublishNotification ann = mock(PublishNotification.class);
            when(ann.eventKey()).thenReturn(NotificationEventKey.SITE_CONFIG_UPDATED);
            when(ann.tenantId()).thenReturn("#tenantId");
            when(ann.userId()).thenReturn("#userId");
            when(ann.metadata()).thenReturn("{'configKey': #result}");
            when(ann.metadataProvider()).thenReturn("");

            notificationAspect.around(joinPoint, ann);

            ArgumentCaptor<NotificationEvent> captor =
                    ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publish(captor.capture());
            assertThat(captor.getValue().getMetadata())
                    .containsEntry("configKey", "my-config");
        }
    }

    @Nested
    @DisplayName("around - method throws")
    class AroundMethodThrows {

        @Test
        @DisplayName("re-throws exception and does not publish event")
        void rethrows_and_skips_publish() throws Throwable {
            doThrow(new RuntimeException("service failed")).when(joinPoint).proceed();
            PublishNotification ann = buildAnnotation();

            assertThatThrownBy(() -> notificationAspect.around(joinPoint, ann))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("service failed");

            verify(notificationEventPublisher, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("around - publish failure")
    class AroundPublishFailure {

        @Test
        @DisplayName("swallows publish exception and returns method result")
        void swallows_publish_exception() throws Throwable {
            stubJoinPoint(new String[]{"tenantId", "userId"},
                    new Object[]{"tenant-1", "user-1"});
            PublishNotification ann = buildAnnotation();
            doThrow(new RuntimeException("rabbit down"))
                    .when(notificationEventPublisher).publish(any());

            Object result = notificationAspect.around(joinPoint, ann);

            assertThat(result).isEqualTo("result");
        }
    }

    @Nested
    @DisplayName("around - no metadata at all")
    class AroundNoMetadata {

        @Test
        @DisplayName("publishes event with null metadata when neither provider nor inline SpEL configured")
        void publishes_null_metadata_when_no_metadata_config() throws Throwable {
            stubJoinPoint(new String[]{"tenantId", "userId"},
                    new Object[]{"tenant-1", "user-1"});

            PublishNotification ann = mock(PublishNotification.class);
            when(ann.eventKey()).thenReturn(NotificationEventKey.SITE_CONFIG_UPDATED);
            when(ann.tenantId()).thenReturn("#tenantId");
            when(ann.userId()).thenReturn("#userId");
            when(ann.metadata()).thenReturn("");   // no inline SpEL
            when(ann.metadataProvider()).thenReturn(""); // no provider

            notificationAspect.around(joinPoint, ann);

            ArgumentCaptor<NotificationEvent> captor =
                    ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publish(captor.capture());
            assertThat(captor.getValue().getMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("around - metadata provider")
    class AroundMetadataProvider {

        @Test
        @DisplayName("calls provider bean before proceed and includes metadata in event")
        void calls_provider_before_proceed() throws Throwable {
            String beanName = "siteConfigNotificationMetadataProvider";
            NotificationMetadataProvider mockProvider = mock(NotificationMetadataProvider.class);
            when(applicationContext.getBean(beanName, NotificationMetadataProvider.class))
                    .thenReturn(mockProvider);
            when(mockProvider.resolve(any(), any()))
                    .thenReturn(Map.of("configKey", "theme"));

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"tenantId", "userId"});
            when(methodSignature.getMethod()).thenReturn(sampleMethod);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"tenant-1", "user-1"});
            when(joinPoint.proceed()).thenReturn(null);

            PublishNotification ann = mock(PublishNotification.class);
            when(ann.eventKey()).thenReturn(NotificationEventKey.SITE_CONFIG_UPDATED);
            when(ann.tenantId()).thenReturn("#tenantId");
            when(ann.userId()).thenReturn("#userId");
            lenient().when(ann.metadata()).thenReturn("");
            when(ann.metadataProvider()).thenReturn(beanName);
            when(ann.entityId()).thenReturn("#tenantId");

            notificationAspect.around(joinPoint, ann);

            ArgumentCaptor<NotificationEvent> captor =
                    ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publish(captor.capture());
            assertThat(captor.getValue().getMetadata())
                    .containsEntry("configKey", "theme");
        }

        @Test
        @DisplayName("uses empty metadata map when provider throws")
        void uses_empty_map_when_provider_throws() throws Throwable {
            String beanName = "failingProvider";
            when(applicationContext.getBean(beanName, NotificationMetadataProvider.class))
                    .thenThrow(new RuntimeException("provider error"));

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"tenantId", "userId"});
            when(methodSignature.getMethod()).thenReturn(sampleMethod);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"tenant-1", "user-1"});
            when(joinPoint.proceed()).thenReturn("result");

            PublishNotification ann = mock(PublishNotification.class);
            when(ann.eventKey()).thenReturn(NotificationEventKey.SITE_CONFIG_UPDATED);
            when(ann.tenantId()).thenReturn("#tenantId");
            when(ann.userId()).thenReturn("#userId");
            lenient().when(ann.metadata()).thenReturn("");
            when(ann.metadataProvider()).thenReturn(beanName);
            when(ann.entityId()).thenReturn("#tenantId");

            Object result = notificationAspect.around(joinPoint, ann);

            assertThat(result).isEqualTo("result");
            ArgumentCaptor<NotificationEvent> captor =
                    ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationEventPublisher).publish(captor.capture());
            assertThat(captor.getValue().getMetadata()).isEmpty();
        }
    }
}
