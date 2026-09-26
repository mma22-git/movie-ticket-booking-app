package com.moviebooking.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.moviebooking.domain.Role;

/**
 * Marks a controller method (or class) as requiring an authenticated principal with one
 * of the given roles. Enforced by {@link RoleAuthorizationInterceptor}. A handler with no
 * such annotation is public.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRole {

    Role[] value();
}
