package com.integration.management.config.aspect;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.EntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLoggable {
    EntityType entityType();
    AuditActivity action();
    String entityIdParam() default "id";
    String entityIdValue() default "";
}
