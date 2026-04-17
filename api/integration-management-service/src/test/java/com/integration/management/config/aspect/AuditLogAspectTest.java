package com.integration.management.config.aspect;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.EntityType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.AuditLog;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.exception.IntegrationApiException;
import com.integration.management.service.ArcGISIntegrationService;
import com.integration.management.service.ClientIpAddressResolver;
import com.integration.management.service.ConfluenceIntegrationService;
import com.integration.management.service.IntegrationConnectionService;
import com.integration.management.service.JiraWebhookEventService;
import com.integration.management.service.JiraWebhookService;
import com.integration.management.service.SettingsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAspect")
class AuditLogAspectTest {

    static class NameRequest {
        private final String name;

        NameRequest(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class IdRequest {
        private final String id;

        IdRequest(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    static class ResponseBodyWithIdAndName {
        private final String id;
        private final String name;

        ResponseBodyWithIdAndName(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    static class Targets {
        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> create(String id, NameRequest request) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.CREATE)
        public ResponseEntity<?> createFromResponse() {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.PENDING, entityIdParam = "id")
        public ResponseEntity<?> toggle(String id, NameRequest request) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.EXECUTE, entityIdParam = "id")
        public ResponseEntity<?> execute(String id, NameRequest request) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.DELETE, entityIdParam = "id")
        public ResponseEntity<?> delete(String id) {
            return ResponseEntity.noContent().build();
        }

        @AuditLoggable(entityType = EntityType.CACHE, action = AuditActivity.EXECUTE, entityIdValue = "allCaches")
        public ResponseEntity<?> clearAllCaches() {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK_EVENT, action = AuditActivity.RETRY, entityIdParam = "id")
        public ResponseEntity<?> retry(String id) {
            return ResponseEntity.ok().build();
        }
    }

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private JiraWebhookEventService triggerHistoryService;
    @Mock
    private JiraWebhookService jiraWebhookService;
    @Mock
    private IntegrationConnectionService integrationConnectionService;
    @Mock
    private ArcGISIntegrationService arcGISIntegrationService;
    @Mock
    private ConfluenceIntegrationService confluenceIntegrationService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private ClientIpAddressResolver clientIpAddressResolver;

    @InjectMocks
    private AuditLogAspect aspect;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("logAudit should persist SUCCESS audit log when entityId and name resolve from parameters")
    void logAudit_success_savesAuditLog() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        ResponseEntity<?> proceeded = ResponseEntity.ok().build();
        when(joinPoint.proceed()).thenReturn(proceeded);
        when(clientIpAddressResolver.resolveClientIpAddress(any())).thenReturn("203.0.113.10");

        Object out = aspect.logAudit(joinPoint);

        assertThat(out).isSameAs(proceeded);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(EntityType.INTEGRATION_CONNECTION);
        assertThat(saved.getEntityId()).isEqualTo("conn-123");
        assertThat(saved.getEntityName()).isEqualTo("My Connection");
        assertThat(saved.getAction()).isEqualTo(AuditActivity.CREATE);
        assertThat(saved.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getPerformedBy()).isEqualTo("user-1");
        assertThat(saved.getClientIpAddress()).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("logAudit should mark FAILED and rethrow when business method throws")
    void logAudit_exception_marksFailed_andRethrows() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        RuntimeException boom = new RuntimeException("boom");
        when(joinPoint.proceed()).thenThrow(boom);

        assertThatThrownBy(() -> aspect.logAudit(joinPoint))
                .isSameAs(boom);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.FAILED);
    }

    @Test
    @DisplayName("logAudit should skip persistence when entityId cannot be resolved")
    void logAudit_missingEntityId_skipsSave() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"other"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {new NameRequest("My Connection")});
        ResponseEntity<?> proceeded = ResponseEntity.ok().build();
        when(joinPoint.proceed()).thenReturn(proceeded);

        Object out = aspect.logAudit(joinPoint);

        assertThat(out).isSameAs(proceeded);
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logAudit should include retry metadata for JIRA_WEBHOOK_EVENT RETRY")
    void logAudit_retry_populatesMetadata() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("retry", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"evt-1"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        JiraWebhookEvent event = JiraWebhookEvent.builder().webhookId("wh-1").build();
        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-1")).thenReturn(event);
        when(jiraWebhookService.getJiraWebhookNameById("wh-1")).thenReturn("Webhook One");

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        Map<String, Object> metadata = captor.getValue().getMetadata();
        assertThat(metadata).containsEntry("originalTriggerId", "evt-1");
        assertThat(metadata).containsEntry("webhookName", "Webhook One");
        assertThat(captor.getValue().getEntityName()).isEqualTo("Webhook One");
    }

    @Test
    @DisplayName("logAudit should resolve entityId from CreationResponse and entityName from response")
    void logAudit_creationResponse_resolvesEntityIdAndName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("createFromResponse");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {});
        when(joinPoint.getArgs()).thenReturn(new Object[] {});

        ResponseEntity<?> proceeded = ResponseEntity.ok(new CreationResponse("resp-1", "Created Name"));
        when(joinPoint.proceed()).thenReturn(proceeded);

        Object out = aspect.logAudit(joinPoint);

        assertThat(out).isSameAs(proceeded);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("resp-1");
        assertThat(captor.getValue().getEntityName()).isEqualTo("Created Name");
    }

    @Test
    @DisplayName("logAudit should resolve entityId from response body getId() and entityName from response")
    void logAudit_responseBody_resolvesEntityIdReflectively() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("createFromResponse");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {});
        when(joinPoint.getArgs()).thenReturn(new Object[] {});

        ResponseEntity<?> proceeded = ResponseEntity.ok(new ResponseBodyWithIdAndName("resp-2", "Body Name"));
        when(joinPoint.proceed()).thenReturn(proceeded);

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("resp-2");
        assertThat(captor.getValue().getEntityName()).isEqualTo("Body Name");
    }

    @Test
    @DisplayName("logAudit should resolve PENDING action to ENABLED and set SUCCESS")
    void logAudit_pending_true_resolvesEnabled() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("toggle", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-1", new NameRequest("Conn")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(Boolean.TRUE));

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.ENABLED);
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    @DisplayName("logAudit should fall back to default entityName when request name is blank")
    void logAudit_blankRequestName_usesDefaultName_andSaves() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("   ")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("Connection - conn-123");
    }

    @Test
    @DisplayName("logAudit should not fail business operation when audit persistence throws")
    void logAudit_auditRepoThrows_isNonBlocking() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        ResponseEntity<?> proceeded = ResponseEntity.ok().build();
        when(joinPoint.proceed()).thenReturn(proceeded);
        doThrow(new RuntimeException("db down")).when(auditLogRepository).save(any(AuditLog.class));

        Object out = aspect.logAudit(joinPoint);

        assertThat(out).isSameAs(proceeded);
    }

    static class CacheTargets {
        @AuditLoggable(entityType = EntityType.CACHE, action = AuditActivity.EXECUTE, entityIdValue = "allCaches")
        public boolean clearAllCachesBoolean() {
            return true;
        }

        @AuditLoggable(entityType = EntityType.CACHE, action = AuditActivity.EXECUTE, entityIdValue = "cacheA")
        public boolean clearCacheBoolean() {
            return true;
        }

        @AuditLoggable(entityType = EntityType.SITE_CONFIG, action = AuditActivity.DELETE, entityIdParam = "id")
        public ResponseEntity<?> deleteSiteConfig(String id) {
            return ResponseEntity.noContent().build();
        }
    }

    @Test
    @DisplayName("logAudit should name CACHE entries based on entityIdValue")
    void logAudit_cacheEntityName_isDerived() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = CacheTargets.class.getMethod("clearAllCachesBoolean");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {});
        when(joinPoint.proceed()).thenReturn(Boolean.TRUE);

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("System Cache (All Caches)");
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    @DisplayName("logAudit should use SITE_CONFIG fallback name when key resolution fails")
    void logAudit_siteConfig_delete_usesFallbackNameOnLookupFailure() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UUID id = UUID.randomUUID();
        when(settingsService.getSiteConfigKeyById(id, "tenant-1"))
                .thenThrow(new RuntimeException("db"));

        Method method = CacheTargets.class.getMethod("deleteSiteConfig", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {id.toString()});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.noContent().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).startsWith("Site Configuration - ");
    }

    @Test
    @DisplayName("logAudit should resolve direct boolean PENDING action to DISABLED")
    void logAudit_pending_false_resolvesDisabled_forDirectBooleanReturn() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("toggle", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-1", new NameRequest("Conn")});
        when(joinPoint.proceed()).thenReturn(Boolean.FALSE);

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.DISABLED);
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    @DisplayName("logAudit should keep PENDING action and mark IN_PROGRESS when return type isn't boolean")
    void logAudit_pending_nonBoolean_keepsPending_andInProgress() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("toggle", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-1", new NameRequest("Conn")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok("ok"));

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.PENDING);
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.IN_PROGRESS);
    }

    @Test
    @DisplayName("logAudit should map non-2xx EXECUTE response to FAILED")
    void logAudit_execute_non2xx_marksFailed() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("execute", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-500", new NameRequest("Conn")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.status(500).build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.FAILED);
    }

    @Test
    @DisplayName("logAudit should pre-resolve entityId and entityName for DELETE")
    void logAudit_delete_prefetchesNameBeforeProceed() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(integrationConnectionService.getIntegrationConnectionNameById("conn-del", "tenant-1"))
                .thenReturn("Conn Delete");

        Method method = Targets.class.getMethod("delete", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-del"});
        ResponseEntity<?> proceeded = ResponseEntity.noContent().build();
        when(joinPoint.proceed()).thenReturn(proceeded);

        Object out = aspect.logAudit(joinPoint);

        assertThat(out).isSameAs(proceeded);
        verify(integrationConnectionService).getIntegrationConnectionNameById("conn-del", "tenant-1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("conn-del");
        assertThat(captor.getValue().getEntityName()).isEqualTo("Conn Delete");
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.DELETE);
    }

    @Test
    @DisplayName("logAudit should skip persistence when pre-delete entityName is blank")
    void logAudit_delete_blankName_skipsSave() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(integrationConnectionService.getIntegrationConnectionNameById("conn-del", "tenant-1"))
                .thenReturn("");

        Method method = Targets.class.getMethod("delete", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-del"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.noContent().build());

        aspect.logAudit(joinPoint);

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logAudit should swallow audit persistence exceptions")
    void logAudit_auditSaveFails_isNonBlocking() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        ResponseEntity<?> proceeded = ResponseEntity.ok().build();
        when(joinPoint.proceed()).thenReturn(proceeded);
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("db down"));

        Object out = aspect.logAudit(joinPoint);

        assertThat(out).isSameAs(proceeded);
    }

    @Test
    @DisplayName("logAudit should resolve entityId from annotation value and resolve CACHE name")
    void logAudit_entityIdValue_usesAnnotationValue_andCacheNaming() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("clearAllCaches");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("allCaches");
        assertThat(captor.getValue().getEntityName()).isEqualTo("System Cache (All Caches)");
    }

    @Test
    @DisplayName("logAudit should include retryMetadataError when retry metadata fetch fails")
    void logAudit_retry_metadataError_whenLookupFails() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("retry", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"evt-err"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-err"))
                .thenThrow(new RuntimeException("lookup failed"));

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMetadata())
                .containsEntry("originalTriggerId", "evt-err")
                .containsKey("retryMetadataError");
    }

    @Test
    @DisplayName("logAudit should throw when request attributes are missing")
    void logAudit_missingRequestAttributes_throws() throws Throwable {
        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        assertThatThrownBy(() -> aspect.logAudit(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing request attribute");

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logAudit should resolve entityId from getter when not a direct parameter")
    void logAudit_entityIdGetterExtraction_resolvesFromArgument() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("createFromResponse");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"req"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {new IdRequest("id-from-getter")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new ResponseBodyWithIdAndName("ignored", "Name")));

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("id-from-getter");
    }

    @Test
    @DisplayName("logAudit should fall back to remote address when proxy headers are missing")
    void logAudit_missingProxyHeaders_usesRemoteAddress() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        request.setRemoteAddr("192.168.1.25");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(clientIpAddressResolver.resolveClientIpAddress(any())).thenReturn("192.168.1.25");

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getClientIpAddress()).isEqualTo("192.168.1.25");
    }

    @Test
    @DisplayName("logAudit should fallback to X-Real-IP when X-Forwarded-For is invalid")
    void logAudit_invalidXForwardedFor_usesXRealIp() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        request.addHeader("X-Forwarded-For", "spoofed-not-an-ip, 203.0.113.1");
        request.addHeader("X-Real-IP", "203.0.113.5");
        request.setRemoteAddr("192.168.1.25");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(clientIpAddressResolver.resolveClientIpAddress(any())).thenReturn("203.0.113.5");

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getClientIpAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("logAudit should fallback to remoteAddr when proxy headers are invalid")
    void logAudit_invalidProxyHeaders_usesRemoteAddress() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        request.addHeader("X-Forwarded-For", "spoofed-invalid-value");
        request.addHeader("X-Real-IP", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff:1");
        request.setRemoteAddr("192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(clientIpAddressResolver.resolveClientIpAddress(any())).thenReturn("192.168.1.100");

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getClientIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("logAudit should persist null clientIpAddress when all IP sources are invalid")
    void logAudit_allIpSourcesInvalid_setsNullClientIp() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        request.addHeader("X-Forwarded-For", "spoofed-invalid-value");
        request.addHeader("X-Real-IP", "also-invalid");
        request.setRemoteAddr("not-an-ip");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-123", new NameRequest("My Connection")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getClientIpAddress()).isNull();
    }

    // -----------------------------------------------------------------------
    // Additional branch-coverage tests for fetchEntityNameFromDatabase,
    // getDefaultEntityName, extractFieldOrGetter, retry metadata, etc.
    // -----------------------------------------------------------------------

    static class JiraWebhookTargets {
        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> createWebhook(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.ARCGIS_INTEGRATION, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> createArcGIS(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.CONFLUENCE_INTEGRATION, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> createConfluence(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK_EVENT, action = AuditActivity.EXECUTE, entityIdParam = "id")
        public ResponseEntity<?> executeWebhookEvent(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.SITE_CONFIG, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> createSiteConfig(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.CACHE, action = AuditActivity.EXECUTE, entityIdValue = "specificCache")
        public ResponseEntity<?> clearSpecificCache() {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.RETRY, entityIdParam = "id")
        public ResponseEntity<?> retryExecution(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.INTEGRATION, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> createIntegration(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK, action = AuditActivity.CREATE, entityIdParam = "id")
        public ResponseEntity<?> createJiraWebhook(String id) {
            return ResponseEntity.ok().build();
        }

        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK, action = AuditActivity.DELETE, entityIdParam = "id")
        public ResponseEntity<?> deleteJiraWebhook(String id) {
            return ResponseEntity.ok().build();
        }
    }

    static class JiraWebhookToggleTargets {
        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK, action = AuditActivity.PENDING, entityIdParam = "id")
        public boolean toggleActiveStatus(String id) {
            return true;
        }
    }

    static class WebhookNameRequest {
        private final String webhookName;

        WebhookNameRequest(String webhookName) {
            this.webhookName = webhookName;
        }

        public String getWebhookName() {
            return webhookName;
        }
    }

    static class IntegrationNameRequest {
        private final String integrationName;

        IntegrationNameRequest(String integrationName) {
            this.integrationName = integrationName;
        }

        public String getIntegrationName() {
            return integrationName;
        }
    }


    static class ConfigKeyResponse {
        private final String configKey;
        private final String id;

        ConfigKeyResponse(String id, String configKey) {
            this.id = id;
            this.configKey = configKey;
        }

        public String getId() {
            return id;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should resolve JIRA_WEBHOOK name")
    void logAudit_jiraWebhookType_resolvesNameFromDatabase() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(jiraWebhookService.getJiraWebhookNameById("wh-id")).thenReturn("My Webhook");

        Method method = JiraWebhookTargets.class.getMethod("createJiraWebhook", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"wh-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("My Webhook");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should resolve ARCGIS_INTEGRATION name")
    void logAudit_arcGISType_resolvesNameFromDatabase() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(arcGISIntegrationService.getArcGISIntegrationNameById("arcgis-id")).thenReturn("ArcGIS Int");

        Method method = JiraWebhookTargets.class.getMethod("createArcGIS", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"arcgis-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("ArcGIS Int");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should resolve CONFLUENCE_INTEGRATION name")
    void logAudit_confluenceType_resolvesNameFromDatabase() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(confluenceIntegrationService.getConfluenceIntegrationNameById("conf-id")).thenReturn("Confluence Int");

        Method method = JiraWebhookTargets.class.getMethod("createConfluence", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conf-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("Confluence Int");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should handle JIRA_WEBHOOK_EVENT with null webhookId")
    void logAudit_webhookEventWithNullWebhookId_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        JiraWebhookEvent event = JiraWebhookEvent.builder().webhookId(null).build();
        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-null-wh")).thenReturn(event);

        Method method = JiraWebhookTargets.class.getMethod("executeWebhookEvent", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"evt-null-wh"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("Webhook Event");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should handle JIRA_WEBHOOK_EVENT with null event")
    void logAudit_webhookEventWithNullEvent_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-null")).thenReturn(null);

        Method method = JiraWebhookTargets.class.getMethod("executeWebhookEvent", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"evt-null"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("Webhook Event");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should resolve JIRA_WEBHOOK_EVENT with valid webhook name")
    void logAudit_webhookEventWithValidWebhook_resolvesWebhookName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        JiraWebhookEvent event = JiraWebhookEvent.builder().webhookId("wh-99").build();
        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-valid")).thenReturn(event);
        when(jiraWebhookService.getJiraWebhookNameById("wh-99")).thenReturn("My Webhook");

        Method method = JiraWebhookTargets.class.getMethod("executeWebhookEvent", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"evt-valid"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("My Webhook");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase should resolve SITE_CONFIG with valid UUID")
    void logAudit_siteConfigType_resolvesNameFromSettings() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UUID configId = UUID.randomUUID();
        when(settingsService.getSiteConfigKeyById(configId, "tenant-1")).thenReturn("logo_url");

        Method method = JiraWebhookTargets.class.getMethod("createSiteConfig", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {configId.toString()});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("logo_url");
    }

    @Test
    @DisplayName("logAudit should use specific CACHE name for non-allCaches entityIdValue")
    void logAudit_specificCacheEntity_usesSpecificCacheName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = JiraWebhookTargets.class.getMethod("clearSpecificCache");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("System Cache (specificCache)");
    }

    @Test
    @DisplayName("logAudit should handle IntegrationApiException from handler rethrow")
    void logAudit_handlerThrowsIntegrationApiException_rethrowsException() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = JiraWebhookTargets.class.getMethod("createJiraWebhook", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"wh-id"});

        IntegrationApiException apiException = new IntegrationApiException("API error", 400);
        when(joinPoint.proceed()).thenThrow(apiException);
        when(jiraWebhookService.getJiraWebhookNameById("wh-id")).thenReturn("Test Webhook");

        assertThatThrownBy(() -> aspect.logAudit(joinPoint))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessage("API error");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.FAILED);
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.CREATE);
    }

    @Test
    @DisplayName("logAudit should skip audit when entityId is blank")
    void logAudit_blankEntityId_skipsAuditLog() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = JiraWebhookTargets.class.getMethod("createJiraWebhook", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {""});  // blank entityId
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = (ResponseEntity<?>) aspect.logAudit(joinPoint);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("logAudit should use fallback name when entityName resolves blank")
    void logAudit_blankEntityName_usesFallbackName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = JiraWebhookTargets.class.getMethod("createJiraWebhook", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"wh-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(jiraWebhookService.getJiraWebhookNameById("wh-id")).thenReturn("");  // blank name

        ResponseEntity<?> result = (ResponseEntity<?>) aspect.logAudit(joinPoint);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        // When name is blank, a fallback entity name is generated - audit IS saved
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isNotBlank();
    }

    @Test
    @DisplayName("auditLogRepository.save fails should not fail business operation")
    void logAudit_auditPersistenceFails_continuesBusinessOperation() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = JiraWebhookTargets.class.getMethod("createJiraWebhook", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"wh-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(jiraWebhookService.getJiraWebhookNameById("wh-id")).thenReturn("Test Webhook");
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> result = (ResponseEntity<?>) aspect.logAudit(joinPoint);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("resolveFinalAction should handle PENDING action with Boolean result")
    void logAudit_pendingActionWithBooleanResult_resolvesFinalAction() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = JiraWebhookToggleTargets.class.getMethod("toggleActiveStatus", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"wh-id"});
        when(joinPoint.proceed()).thenReturn(true);  // Direct boolean
        when(jiraWebhookService.getJiraWebhookNameById("wh-id")).thenReturn("Test Webhook");

        Boolean result = (Boolean) aspect.logAudit(joinPoint);

        assertThat(result).isTrue();
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isIn(AuditActivity.ENABLED, AuditActivity.DISABLED);
        assertThat(saved.getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    static class ResponseEntityBooleanToggle {
        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.PENDING, entityIdParam = "id")
        public ResponseEntity<Boolean> toggleViaResponseEntity(String id) {
            return ResponseEntity.ok(true);
        }
    }

    @Test
    @DisplayName("resolveFinalAction should handle ResponseEntity<Boolean> for PENDING action → ENABLED")
    void logAudit_pendingWithResponseEntityBoolean_resolvesEnabled() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = ResponseEntityBooleanToggle.class.getMethod("toggleViaResponseEntity", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-1"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(Boolean.TRUE));
        when(integrationConnectionService.getIntegrationConnectionNameById("conn-1", "tenant-1")).thenReturn("Conn");

        ResponseEntity<?> result = (ResponseEntity<?>) aspect.logAudit(joinPoint);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.ENABLED);
        assertThat(captor.getValue().getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    @DisplayName("resolveFinalAction should handle ResponseEntity<Boolean>=false for PENDING action → DISABLED")
    void logAudit_pendingWithResponseEntityBooleanFalse_resolvesDisabled() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = ResponseEntityBooleanToggle.class.getMethod("toggleViaResponseEntity", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conn-1"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(Boolean.FALSE));
        when(integrationConnectionService.getIntegrationConnectionNameById("conn-1", "tenant-1")).thenReturn("Conn");

        ResponseEntity<?> result = (ResponseEntity<?>) aspect.logAudit(joinPoint);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActivity.DISABLED);
    }

    static class ConfluenceTargets {
        @AuditLoggable(entityType = EntityType.CONFLUENCE_INTEGRATION, action = AuditActivity.UPDATE, entityIdParam = "id")
        public ResponseEntity<?> updateConfluence(String id) {
            return ResponseEntity.ok().build();
        }
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase resolves CONFLUENCE_INTEGRATION name from service via ConfluenceTargets")
    void logAudit_confluenceType_resolvesNameFromDatabase_viaConfluenceTargets() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(confluenceIntegrationService.getConfluenceIntegrationNameById("conf-id")).thenReturn("Conf Integration");

        Method method = ConfluenceTargets.class.getMethod("updateConfluence", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conf-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("Conf Integration");
        assertThat(captor.getValue().getEntityType()).isEqualTo(EntityType.CONFLUENCE_INTEGRATION);
    }

    static class IntegrationTargets {
        @AuditLoggable(entityType = EntityType.INTEGRATION, action = AuditActivity.UPDATE, entityIdParam = "id")
        public ResponseEntity<?> updateIntegration(String id) {
            return ResponseEntity.ok().build();
        }
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase uses getDefaultEntityName for INTEGRATION type - no special resolver")
    void logAudit_integrationType_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = IntegrationTargets.class.getMethod("updateIntegration", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"integ-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // INTEGRATION type falls through to default in fetchEntityNameFromDatabase → "Unknown Entity"
        // or triggers getDefaultEntityName for "Integration - integ-id"
        assertThat(captor.getValue().getEntityType()).isEqualTo(EntityType.INTEGRATION);
        assertThat(captor.getValue().getEntityName()).isNotBlank();
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase falls back to default when ArcGIS service returns null")
    void logAudit_arcgisType_nullNameFromService_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(arcGISIntegrationService.getArcGISIntegrationNameById("arc-id")).thenReturn(null);

        Method method = JiraWebhookTargets.class.getMethod("createArcGIS", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"arc-id"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // null name → getDefaultEntityName → "ArcGIS Integration - arc-id"
        assertThat(captor.getValue().getEntityName()).isEqualTo("ArcGIS Integration - arc-id");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase falls back to default when Confluence service returns null")
    void logAudit_confluenceType_nullNameFromService_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(confluenceIntegrationService.getConfluenceIntegrationNameById("conf-null")).thenReturn(null);

        Method method = ConfluenceTargets.class.getMethod("updateConfluence", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"conf-null"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // null name → getDefaultEntityName → "Confluence Integration - conf-null"
        assertThat(captor.getValue().getEntityName()).isEqualTo("Confluence Integration - conf-null");
    }

    // -----------------------------------------------------------------------
    // Additional branch coverage: getDefaultEntityName INTEGRATION_CONNECTION
    // -----------------------------------------------------------------------

    static class ConnectionTargets {
        @AuditLoggable(entityType = EntityType.INTEGRATION_CONNECTION, action = AuditActivity.UPDATE, entityIdParam = "id")
        public ResponseEntity<?> updateConnection(String id) {
            return ResponseEntity.ok().build();
        }
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase falls back to getDefaultEntityName for INTEGRATION_CONNECTION when service throws")
    void logAudit_integrationConnectionType_serviceThrows_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(integrationConnectionService.getIntegrationConnectionNameById("conn-ex", "tenant-1"))
                .thenThrow(new RuntimeException("db error"));

        Method method = ConnectionTargets.class.getMethod("updateConnection", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"conn-ex"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // exception → getDefaultEntityName → "Connection - conn-ex"
        assertThat(captor.getValue().getEntityName()).isEqualTo("Connection - conn-ex");
    }

    static class WebhookDeleteTargets {
        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK, action = AuditActivity.DELETE, entityIdParam = "id")
        public ResponseEntity<?> deleteWebhook(String id) {
            return ResponseEntity.noContent().build();
        }
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase falls back to getDefaultEntityName for JIRA_WEBHOOK when service throws")
    void logAudit_jiraWebhookType_serviceThrows_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(jiraWebhookService.getJiraWebhookNameById("wh-ex"))
                .thenThrow(new RuntimeException("webhook not found"));

        Method method = WebhookDeleteTargets.class.getMethod("deleteWebhook", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"wh-ex"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.noContent().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // exception → getDefaultEntityName → "Webhook - wh-ex"
        assertThat(captor.getValue().getEntityName()).isEqualTo("Webhook - wh-ex");
    }

    static class WebhookEventTargets {
        @AuditLoggable(entityType = EntityType.JIRA_WEBHOOK_EVENT, action = AuditActivity.EXECUTE, entityIdParam = "id")
        public ResponseEntity<?> executeEvent(String id) {
            return ResponseEntity.ok().build();
        }
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase falls back to getDefaultEntityName for JIRA_WEBHOOK_EVENT when service throws")
    void logAudit_jiraWebhookEventType_serviceThrows_usesDefaultName() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-ex"))
                .thenThrow(new RuntimeException("event not found"));

        Method method = WebhookEventTargets.class.getMethod("executeEvent", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"evt-ex"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // exception → yields "Webhook Event" (default in the JIRA_WEBHOOK_EVENT case)
        assertThat(captor.getValue().getEntityName()).isEqualTo("Webhook Event");
    }

    @Test
    @DisplayName("fetchEntityNameFromDatabase getDefaultEntityName for INTEGRATION_CONNECTION via IllegalArgumentException")
    void logAudit_integrationConnectionType_invalidUUID_usesDefaultName() throws Throwable {
        // Covers getDefaultEntityName: INTEGRATION_CONNECTION case via outer IllegalArgumentException catch
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(integrationConnectionService.getIntegrationConnectionNameById("not-a-uuid", "tenant-1"))
                .thenThrow(new IllegalArgumentException("invalid UUID"));

        Method method = ConnectionTargets.class.getMethod("updateConnection", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"not-a-uuid"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // IllegalArgumentException → getDefaultEntityName → "Connection - not-a-uuid"
        assertThat(captor.getValue().getEntityName()).isEqualTo("Connection - not-a-uuid");
    }

    @Test
    @DisplayName("extractFieldOrGetter resolves value from superclass field via inheritance chain")
    void logAudit_extractFieldOrGetter_superclassField_resolves() throws Throwable {
        // Covers extractFieldOrGetter: superclass traversal (cls = cls.getSuperclass())
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Use a subclass that inherits 'id' field from a parent class
        class BaseRequest {
            final String id;
            BaseRequest(String baseId) {
                this.id = baseId;
            }
        }
        class SubRequest extends BaseRequest {
            SubRequest(String subId) {
                super(subId);
            }
        }

        // For this test, we use Targets.create which looks for "id" parameter name
        Method method = Targets.class.getMethod("create", String.class, NameRequest.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        // Parameter names don't include "id" directly, forcing field extraction path
        when(signature.getParameterNames()).thenReturn(new String[]{"req", "name"});
        SubRequest subReq = new SubRequest("field-id-123");
        when(joinPoint.getArgs()).thenReturn(new Object[]{subReq, new NameRequest("Test Name")});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // extractFieldOrGetter traverses superclass to find "id"
        assertThat(captor.getValue().getEntityId()).isEqualTo("field-id-123");
    }

    @Test
    @DisplayName("populateRetryMetadata: webhookId is null in event, skips webhookName fetch")
    void logAudit_retryMetadata_nullWebhookId_skipsWebhookNameFetch() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        JiraWebhookEvent event = new JiraWebhookEvent();
        // webhookId is null => skip webhook name fetch
        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-null-wh"))
                .thenReturn(event);

        Method method = Targets.class.getMethod("retry", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"evt-null-wh"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // No webhook name added, but originalTriggerId present
        assertThat(captor.getValue().getMetadata()).containsKey("originalTriggerId");
        assertThat(captor.getValue().getMetadata()).doesNotContainKey("webhookName");
    }

    @Test
    @DisplayName("populateRetryMetadata: webhookName is null, skips metadata addition")
    void logAudit_retryMetadata_nullWebhookName_skipsWebhookNameMetadata() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        JiraWebhookEvent event = new JiraWebhookEvent();
        event.setWebhookId("wh-null-name");
        when(triggerHistoryService.findByOriginalEventIdOrderByRetryAttempt("evt-null-name"))
                .thenReturn(event);
        when(jiraWebhookService.getJiraWebhookNameById("wh-null-name")).thenReturn(null);

        Method method = Targets.class.getMethod("retry", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"evt-null-name"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMetadata()).containsKey("originalTriggerId");
        assertThat(captor.getValue().getMetadata()).doesNotContainKey("webhookName");
    }

    @Test
    @DisplayName("resolvePreDeleteContext: non-DELETE action returns empty context")
    void logAudit_nonDelete_resolvePreDeleteContext_returnsEmptyContext() throws Throwable {
        // Covers resolvePreDeleteContext: annotation.action() != DELETE => return new PreDeleteContext(null, null)
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = ConnectionTargets.class.getMethod("updateConnection", String.class);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"conn-update"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        aspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("conn-update");
    }

    @Test
    @DisplayName("logAudit extractIdFromResponseBody: body has getId returning null")
    void logAudit_responseBodyWithNullGetId_returnsNull() throws Throwable {
        // Covers extractIdFromResponseBody: val == null => don't return, fall through
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setAttribute(X_TENANT_ID, "tenant-1");
        request.setAttribute(X_USER_ID, "user-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        class NullIdBody {
            public String getId() {
                return null;
            }
        }

        Method method = Targets.class.getMethod("createFromResponse");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new NullIdBody()));

        aspect.logAudit(joinPoint);

        // entityId cannot be resolved (null from getId()) => audit is not saved (entityId is null/blank)
        verify(auditLogRepository, never()).save(any());
    }
}

