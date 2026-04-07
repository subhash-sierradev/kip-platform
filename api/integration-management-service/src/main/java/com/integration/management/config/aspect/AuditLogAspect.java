package com.integration.management.config.aspect;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.EntityType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.AuditLog;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.service.ArcGISIntegrationService;
import com.integration.management.service.ConfluenceIntegrationService;
import com.integration.management.service.IntegrationConnectionService;
import com.integration.management.service.JiraWebhookEventService;
import com.integration.management.service.JiraWebhookService;
import com.integration.management.service.SettingsService;
import com.integration.management.util.ClientIpAddressResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.integration.management.constants.ReflectionConstants.METHOD_PREFIX_GET;
import static com.integration.management.constants.ReflectionConstants.METHOD_PREFIX_IS;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.execution.contract.model.enums.EntityType.JIRA_WEBHOOK_EVENT;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public final class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final JiraWebhookEventService triggerHistoryService;
    private final JiraWebhookService jiraWebhookService;
    private final IntegrationConnectionService integrationConnectionService;
    private final ArcGISIntegrationService arcGISIntegrationService;
    @Lazy
    private final ConfluenceIntegrationService confluenceIntegrationService;
    private final SettingsService settingsService;

    private record PreDeleteContext(String entityId, String entityName) {
    }

    @Around("@annotation(com.integration.management.config.aspect.AuditLoggable)")
    public Object logAudit(final ProceedingJoinPoint joinPoint) throws Throwable {
        final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        final AuditLoggable annotation = method.getAnnotation(AuditLoggable.class);

        // For DELETE operations, resolve entityId and entityName BEFORE execution
        // because the entity will be deleted and won't be available after
        final PreDeleteContext preDeleteContext = resolvePreDeleteContext(joinPoint, annotation);

        Object result = null;
        Throwable thrownException = null;
        AuditResult auditResult;
        AuditActivity finalAction;

        try {
            // Execute the actual method
            result = joinPoint.proceed();
            // Determine final action first, then result based on that action
            finalAction = resolveFinalAction(annotation, result);
            auditResult = determineResultFromSuccess(annotation, result, finalAction);
        } catch (Throwable e) {
            thrownException = e;
            // If method fails, use the original annotation action and set result to FAILED
            finalAction = annotation.action();
            auditResult = AuditResult.FAILED;
            log.debug("Method execution failed, audit result set to FAILED: {}", e.getMessage(), e);
            // Re-throw the exception after audit logging
        }

        // Extract context information
        String tenantId = extractHeaderValue(X_TENANT_ID);
        String userId = extractHeaderValue(X_USER_ID);
        String clientIpAddress = extractClientIpAddress();
        final String entityId = determineEntityId(preDeleteContext, joinPoint, annotation, result);
        final String entityName = determineEntityName(
                preDeleteContext, joinPoint, annotation, result, entityId);

        if (entityId == null || entityId.isBlank()) {
            log.warn("Skipping audit log creation: unable to resolve entityId for action {} on method {}",
                    annotation.action(), method.getName());
            if (thrownException != null) throw thrownException;
            return result;
        }

        if (entityName.isBlank()) {
            log.warn("Skipping audit log creation: unable to resolve entityName for action {} "
                    + "on method {} with entityId {}", annotation.action(), method.getName(), entityId);
            if (thrownException != null) throw thrownException;
            return result;
        }

        // Populate retry metadata only for webhook event RETRY operations
        final Map<String, Object> metadata = shouldPopulateRetryMetadata(annotation, method)
                ? populateRetryMetadata(joinPoint, entityId)
                : Collections.emptyMap();

        // Use the already determined finalAction (no need to resolve again)
        // Create audit log with result field
        final AuditLog logEntry = AuditLog.builder()
                .entityType(annotation.entityType())
                .entityId(entityId)
                .entityName(entityName)
                .action(finalAction)
                .result(auditResult)
                .performedBy(userId)
                .clientIpAddress(clientIpAddress)
                .tenantId(tenantId)
                .metadata(metadata)
                .build();

        try {
            auditLogRepository.save(logEntry);
            log.info("Saved audit log: {} with result: {}", logEntry, auditResult);
        } catch (RuntimeException e) {
            // Do not fail the business operation if audit logging fails due to schema mismatch or other DB issues
            log.warn("Audit logging failed (non-blocking). Skipping audit persistence. cause={} message={}",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        if (thrownException != null) throw thrownException;
        return result;
    }

    private String resolveEntityName(final ProceedingJoinPoint joinPoint,
                                     final AuditLoggable annotation,
                                     final Object result,
                                     final String entityId) {

        try {
            // Try to extract entity name from the request parameters first
            String nameFromParams = extractEntityNameFromParameters(joinPoint);
            if (nameFromParams != null && !nameFromParams.isBlank()) {
                log.debug("Entity name resolved from request parameters: {}", nameFromParams);
                return nameFromParams;
            }

            // Try to extract entity name from the response
            String nameFromResponse = extractEntityNameFromResponse(result);
            if (nameFromResponse != null && !nameFromResponse.isBlank()) {
                log.debug("Entity name resolved from response: {}", nameFromResponse);
                return nameFromResponse;
            }

            // Fallback: fetch entity name from database based on entity type and ID
            String nameFromDatabase = fetchEntityNameFromDatabase(annotation.entityType(), entityId);
            if (nameFromDatabase != null && !nameFromDatabase.isBlank()) {
                log.debug("Entity name resolved from database: {}", nameFromDatabase);
                return nameFromDatabase;
            }

            // This should not happen now since fetchEntityNameFromDatabase provides defaults
            log.warn("Could not resolve entity name for entityType: {}, entityId: {}",
                    annotation.entityType(), entityId);
            return getDefaultEntityName(annotation.entityType(), entityId);
        } catch (Exception e) {
            log.error("Error resolving entity name for entityType: {}, entityId: {}",
                    annotation.entityType(), entityId, e);
            return getDefaultEntityName(annotation.entityType(), entityId);
        }
    }

    private String extractEntityNameFromParameters(final ProceedingJoinPoint joinPoint) {
        final Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            // Try to extract name from common request objects
            Object nameValue = extractFieldOrGetter(arg, "integrationName");
            if (nameValue == null) {
                nameValue = extractFieldOrGetter(arg, "name");
            }
            if (nameValue == null) {
                nameValue = extractFieldOrGetter(arg, "webhookName");
            }

            if (nameValue != null) {
                return String.valueOf(nameValue);
            }
        }
        return null;
    }

    private String extractEntityNameFromResponse(final Object result) {
        if (result instanceof ResponseEntity<?> entity && entity.getBody() != null) {
            Object body = entity.getBody();

            // Try to extract name from response body
            Object nameValue = extractFieldOrGetter(body, "integrationName");
            if (nameValue == null) {
                nameValue = extractFieldOrGetter(body, "name");
            }
            if (nameValue == null) {
                nameValue = extractFieldOrGetter(body, "webhookName");
            }
            if (nameValue == null) {
                nameValue = extractFieldOrGetter(body, "configKey");
            }

            if (nameValue != null) {
                return String.valueOf(nameValue);
            }
        }
        return null;
    }

    private String fetchEntityNameFromDatabase(final EntityType entityType, final String entityId) {
        try {
            return switch (entityType) {
                case INTEGRATION_CONNECTION -> {
                    String tenantId = extractHeaderValue(X_TENANT_ID);
                    yield integrationConnectionService.getIntegrationConnectionNameById(entityId, tenantId);
                }
                case JIRA_WEBHOOK -> jiraWebhookService.getJiraWebhookNameById(entityId);
                case ARCGIS_INTEGRATION -> arcGISIntegrationService.getArcGISIntegrationNameById(entityId);
                case CONFLUENCE_INTEGRATION -> confluenceIntegrationService.getConfluenceIntegrationNameById(entityId);
                case JIRA_WEBHOOK_EVENT -> {
                    // For webhook events, try to getSecret the event and use its webhook name
                    try {
                        JiraWebhookEvent event = triggerHistoryService
                                .findByOriginalEventIdOrderByRetryAttempt(entityId);
                        if (event != null && event.getWebhookId() != null) {
                            String webhookName = jiraWebhookService.getJiraWebhookNameById(event.getWebhookId());
                            yield webhookName != null ? webhookName : "Webhook Event";
                        }
                    } catch (Exception e) {
                        log.debug("Could not resolve webhook event details for entityId: {}", entityId, e);
                    }
                    yield "Webhook Event";
                }
                case SITE_CONFIG -> {
                    String tenantId = extractHeaderValue(X_TENANT_ID);
                    yield resolveSiteConfigName(entityId, tenantId);
                }
                case CACHE -> {
                    if ("allCaches".equals(entityId)) {
                        yield "System Cache (All Caches)";
                    } else {
                        yield "System Cache (" + entityId + ")";
                    }
                }
                default -> "Unknown Entity";
            };
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for entityId: {}", entityId);
            return getDefaultEntityName(entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to fetch entity name from database for entityType: {}, entityId: {}",
                    entityType, entityId, e);
            return getDefaultEntityName(entityType, entityId);
        }
    }

    private String resolveSiteConfigName(final String entityId, final String tenantId) {
        try {
            return settingsService.getSiteConfigKeyById(UUID.fromString(entityId), tenantId);
        } catch (Exception e) {
            log.debug("Could not resolve site config key for entityId: {}, tenantId: {}", entityId, tenantId, e);
            return "Site Configuration - " + entityId;
        }
    }

    private String getDefaultEntityName(final EntityType entityType, final String entityId) {
        return switch (entityType) {
            case ARCGIS_INTEGRATION -> "ArcGIS Integration - " + entityId;
            case CONFLUENCE_INTEGRATION -> "Confluence Integration - " + entityId;
            case INTEGRATION -> "Integration - " + entityId;
            case INTEGRATION_CONNECTION -> "Connection - " + entityId;
            case JIRA_WEBHOOK -> "Webhook - " + entityId;
            case JIRA_WEBHOOK_EVENT -> "Webhook Event - " + entityId;
            case SITE_CONFIG -> "Site Config - " + entityId;
            case CACHE -> {
                if ("allCaches".equals(entityId)) {
                    yield "Cache (All Caches)";
                } else {
                    yield "Cache (" + entityId + ")";
                }
            }
        };
    }

    private boolean shouldPopulateRetryMetadata(AuditLoggable annotation, Method method) {
        // Strict: only JIRA_WEBHOOK_EVENT with RETRY action
        return annotation.entityType() == JIRA_WEBHOOK_EVENT && annotation.action() == AuditActivity.RETRY;
    }

    private String extractHeaderValue(final String header) {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(attrs -> attrs.getRequest().getAttribute(header))
                .map(String::valueOf)
                .orElseThrow(() -> new IllegalStateException("Missing request attribute: " + header));
    }

    private String extractClientIpAddress() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(ClientIpAddressResolver::resolveClientIpAddress)
                .orElse(null);
    }

    private String resolveEntityId(final ProceedingJoinPoint joinPoint,
                                   final AuditLoggable annotation,
                                   final Object result) {
        if (!annotation.entityIdValue().isEmpty()) {
            log.info("Audit entityId resolved from annotation value: {}", annotation.entityIdValue());
            return annotation.entityIdValue();
        }

        String entityId = resolveFromParameters(joinPoint, annotation);
        if (entityId != null) {
            return entityId;
        }

        return resolveFromResult(result, annotation);
    }

    private String resolveFromParameters(final ProceedingJoinPoint joinPoint,
                                         final AuditLoggable annotation) {
        final String targetParam = annotation.entityIdParam();
        if (targetParam.isBlank()) {
            return null;
        }

        final String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        final Object[] args = joinPoint.getArgs();

        // Try direct parameter match first
        String directId = IntStream.range(0, paramNames.length)
                .filter(i -> targetParam.equals(paramNames[i]) && args[i] != null)
                .mapToObj(i -> String.valueOf(args[i]))
                .findFirst()
                .orElse(null);
        if (directId != null) {
            log.debug("Audit entityId resolved from direct parameter '{}': {}", targetParam, directId);
            return directId;
        }

        // Try field/getter extraction
        for (Object arg : args) {
            if (arg == null)
                continue;
            Object extracted = extractFieldOrGetter(arg, targetParam);
            if (extracted != null) {
                log.debug("Audit entityId resolved from field/getter '{}' in argument {}: {}",
                        targetParam, arg.getClass().getSimpleName(), extracted);
                return String.valueOf(extracted);
            }
        }
        return null;
    }

    private String resolveFromResult(final Object result, final AuditLoggable annotation) {
        if (result instanceof ResponseEntity<?> entity
                && entity.getBody() instanceof CreationResponse creationResponse) {
            log.debug("Audit entityId resolved from CreationResponse body: {}", creationResponse.getId());
            return creationResponse.getId();
        }

        if (result instanceof ResponseEntity<?> genericEntity && genericEntity.getBody() != null) {
            return extractIdFromResponseBody(genericEntity.getBody());
        }

        log.warn("Audit entityId could not be resolved (entityType={}, action={}, targetParam='{}')",
                annotation.entityType(), annotation.action(), annotation.entityIdParam());
        return null;
    }

    private String extractIdFromResponseBody(final Object body) {
        try {
            Method getId = body.getClass().getMethod("getId");
            Object val = getId.invoke(body);
            if (val != null) {
                log.debug("Audit entityId resolved reflectively from ResponseEntity body getId(): {}", val);
                return String.valueOf(val);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Failed reflective getId() on response body {}: {}",
                    body.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    private Object extractFieldOrGetter(Object source, String name) {
        // Try getter first (getX / isX)
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
        for (String prefix : new String[]{METHOD_PREFIX_GET, METHOD_PREFIX_IS}) {
            try {
                Method m = source.getClass().getMethod(prefix + capitalized);
                if (m.getParameterCount() == 0) {
                    Object val = m.invoke(source);
                    if (val != null)
                        return val;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                log.debug("Failed invoking getter {}{} on {}: {}", prefix, capitalized,
                        source.getClass().getSimpleName(), e.getMessage());
            }
        }
        // Try field (including superclasses)
        Class<?> cls = source.getClass();
        while (cls != null && cls != Object.class) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                Object val = f.get(source);
                if (val != null)
                    return val;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                log.debug("Failed accessing field '{}' on {}: {}", name, cls.getSimpleName(), e.getMessage());
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private Map<String, Object> populateRetryMetadata(ProceedingJoinPoint joinPoint, String entityId) {
        Map<String, Object> metadata = new HashMap<>();
        if (entityId != null) {
            metadata.put("originalTriggerId", entityId);
            try {
                JiraWebhookEvent originalEvent = triggerHistoryService
                        .findByOriginalEventIdOrderByRetryAttempt(entityId);
                String webhookId = originalEvent.getWebhookId();
                if (webhookId != null) {
                    // Get webhook name using the cached service method
                    String webhookName = jiraWebhookService.getJiraWebhookNameById(webhookId);
                    if (webhookName != null) {
                        metadata.put("webhookName", webhookName);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch original webhook event details for retry audit: {}", e.getMessage());
                metadata.put("retryMetadataError", "Unable to fetch original event details: " + e.getMessage());
            }
        }
        // Add method-specific retry information
        String methodName = joinPoint.getSignature().getName();
        log.debug("Generated retry metadata: {}", metadata);
        return metadata;
    }

    private AuditActivity resolveFinalAction(final AuditLoggable annotation, final Object result) {
        if (!annotation.action().equals(AuditActivity.PENDING)) {
            return annotation.action();
        }

        // Handle ResponseEntity<Boolean> for toggle operations
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body instanceof Boolean booleanResult) {
                AuditActivity resolvedAction = booleanResult ? AuditActivity.ENABLED : AuditActivity.DISABLED;
                log.debug("Dynamically resolved audit action: {} (return value: {})", resolvedAction, booleanResult);
                return resolvedAction;
            }
        }

        // Handle direct boolean return
        if (result instanceof Boolean booleanResult) {
            AuditActivity resolvedAction = booleanResult ? AuditActivity.ENABLED : AuditActivity.DISABLED;
            log.debug("Dynamically resolved audit action: {} (return value: {})", resolvedAction, booleanResult);
            return resolvedAction;
        }
        return annotation.action();
    }

    /**
     * Determines the audit result based on successful method execution and final resolved action.
     * Maps actions to appropriate result states for successful operations.
     */
    private AuditResult determineResultFromSuccess(final AuditLoggable annotation, final Object result,
                                                   final AuditActivity finalAction) {
        // Use the final resolved action for result determination, not the original annotation action
        return switch (finalAction) {
            case PENDING -> AuditResult.IN_PROGRESS;
            case ENABLED, DISABLED -> {
                // Toggle actions should have explicit SUCCESS/FAILED results
                // ENABLED = successful enablement, DISABLED = successful disablement
                yield AuditResult.SUCCESS;
            }
            case EXECUTE, RETRY -> {
                // For operations that can have meaningful results, check the response
                if (result instanceof ResponseEntity<?> responseEntity) {
                    // Consider 2xx status codes as success
                    yield responseEntity.getStatusCode().is2xxSuccessful()
                            ? AuditResult.SUCCESS : AuditResult.FAILED;
                }
                yield AuditResult.SUCCESS;
            }
            default -> AuditResult.SUCCESS;
        };
    }

    private PreDeleteContext resolvePreDeleteContext(final ProceedingJoinPoint joinPoint,
                                                     final AuditLoggable annotation) {
        if (annotation.action() != AuditActivity.DELETE) {
            return new PreDeleteContext(null, null);
        }

        String entityId = resolveEntityId(joinPoint, annotation, null);
        String entityName = null;
        if (entityId != null && !entityId.isBlank()) {
            entityName = fetchEntityNameFromDatabase(annotation.entityType(), entityId);
        }
        return new PreDeleteContext(entityId, entityName);
    }

    private String determineEntityId(PreDeleteContext preDeleteContext,
                                     ProceedingJoinPoint joinPoint,
                                     AuditLoggable annotation,
                                     Object result) {
        if (preDeleteContext.entityId != null) {
            return preDeleteContext.entityId;
        }
        return resolveEntityId(joinPoint, annotation, result);
    }

    private String determineEntityName(PreDeleteContext preDeleteContext,
                                       ProceedingJoinPoint joinPoint,
                                       AuditLoggable annotation,
                                       Object result,
                                       String entityId) {
        return Objects.requireNonNullElseGet(preDeleteContext.entityName,
                () -> resolveEntityName(joinPoint, annotation, result, entityId));
    }
}
