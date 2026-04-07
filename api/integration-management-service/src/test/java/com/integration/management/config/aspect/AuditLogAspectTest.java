package com.integration.management.config.aspect;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.EntityType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.AuditLog;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.service.ArcGISIntegrationService;
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
    private SettingsService settingsService;

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
}
