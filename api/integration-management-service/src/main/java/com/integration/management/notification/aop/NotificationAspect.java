package com.integration.management.notification.aop;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Intercepts methods annotated with {@link PublishNotification} and publishes a
 * {@link NotificationEvent} after the method returns successfully.
 *
 * <p>
 * Metadata resolution strategies:
 * <ul>
 * <li><b>Provider</b> (pre-fetched): called <em>before</em>
 * {@code joinPoint.proceed()} so the
 * entity is still present in the database (supports void/delete methods).</li>
 * <li><b>Inline SpEL</b> (post-return): evaluated after proceed with
 * {@code #result} and all
 * method parameters bound by name.</li>
 * </ul>
 *
 * <p>
 * Notification failures are caught and logged — they never propagate to the
 * caller.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotificationAspect {

    private final NotificationEventPublisher notificationEventPublisher;
    private final ApplicationContext applicationContext;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Around("@annotation(publishNotification)")
    public Object around(final ProceedingJoinPoint joinPoint,
            final PublishNotification publishNotification) throws Throwable {

        // Pre-fetch metadata BEFORE proceed so deleted/modified entities are still in
        // the DB
        Map<String, Object> prefetchedMetadata = prefetchMetadata(joinPoint, publishNotification);
        Object result = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // Log and re-throw exceptions from the main operation; we don't want to swallow
            // them
            log.error("Exception in method annotated with @PublishNotification for eventKey='{}': {}",
                    publishNotification.eventKey().name(), ex.getMessage(), ex);
            throw ex;
        }

        try {
            EvaluationContext ctx = buildContext(joinPoint, result);
            String tenantId = evaluate(publishNotification.tenantId(), ctx, String.class);
            String userId = evaluate(publishNotification.userId(), ctx, String.class);
            Map<String, Object> metadata = resolveMetadata(publishNotification, ctx, prefetchedMetadata);

            NotificationEvent event = NotificationEvent.builder()
                    .eventKey(publishNotification.eventKey().name())
                    .tenantId(tenantId)
                    .triggeredByUserId(userId)
                    .metadata(metadata)
                    .build();

            notificationEventPublisher.publishAfterCommit(event);
        } catch (Exception ex) {
            // Never let notification failure propagate to the calling operation
            log.error("NotificationAspect failed for eventKey='{}': {}",
                    publishNotification.eventKey().name(), ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * If a provider bean is configured, resolves the entity ID via SpEL and calls
     * the provider
     * before the method proceeds. Returns {@code null} when inline SpEL is used
     * instead.
     */
    private Map<String, Object> prefetchMetadata(final ProceedingJoinPoint joinPoint,
            final PublishNotification annotation) {
        if (annotation.metadataProvider().isEmpty()) {
            return null;
        }
        try {
            EvaluationContext ctx = buildContext(joinPoint, null);
            Object entityId = spelParser.parseExpression(annotation.entityId()).getValue(ctx);
            String tenantId = evaluate(annotation.tenantId(), ctx, String.class);
            NotificationMetadataProvider provider = applicationContext.getBean(annotation.metadataProvider(),
                    NotificationMetadataProvider.class);
            return provider.resolve(entityId, tenantId);
        } catch (Exception ex) {
            log.warn("Failed to prefetch notification metadata for eventKey='{}': {}",
                    annotation.eventKey().name(), ex.getMessage(), ex);
            return Map.of();
        }
    }

    /**
     * Uses prefetched provider data when available; otherwise evaluates the inline
     * SpEL expression
     * with {@code #result} and all method parameters bound.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveMetadata(final PublishNotification annotation,
            final EvaluationContext ctx,
            final Map<String, Object> prefetched) {
        if (prefetched != null) {
            return prefetched;
        }
        if (!annotation.metadata().isEmpty()) {
            return spelParser.parseExpression(annotation.metadata()).getValue(ctx, Map.class);
        }
        return null;
    }

    private EvaluationContext buildContext(final ProceedingJoinPoint joinPoint,
            final Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        String[] parameterNames = signature.getParameterNames();

        StandardEvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < parameters.length; i++) {
            String parameterName = null;
            if (parameterNames != null && i < parameterNames.length) {
                parameterName = parameterNames[i];
            }
            if (parameterName == null || parameterName.isBlank()) {
                parameterName = parameters[i].getName();
            }

            ctx.setVariable(parameterName, args[i]);
            ctx.setVariable("p" + i, args[i]);
            ctx.setVariable("a" + i, args[i]);
        }
        ctx.setVariable("result", result);
        return ctx;
    }

    private <T> T evaluate(final String expression,
            final EvaluationContext ctx,
            final Class<T> type) {
        return spelParser.parseExpression(expression).getValue(ctx, type);
    }
}
