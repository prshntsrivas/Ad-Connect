package com.adconnect.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the {@link CurrentUser} into a controller method parameter.
 * Responds 401 when the gateway did not forward an identity, unless {@code required = false}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
    boolean required() default true;
}
