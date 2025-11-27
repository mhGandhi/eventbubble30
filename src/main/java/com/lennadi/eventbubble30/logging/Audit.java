package com.lennadi.eventbubble30.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audit {
    AuditLog.Action action();

    String resourceType();

    String resourceIdParam() default "";
}
