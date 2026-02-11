package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.features.db.EntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audit {
    AuditLog.Action action();
    EntityType resourceType();

    String resourceIdParam() default ""; // nach Parameter
    String resourceIdExpression() default ""; // Ausdruck
}

