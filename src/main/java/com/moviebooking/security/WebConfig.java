package com.moviebooking.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.moviebooking.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Registers the role-authorization interceptor across the API. Endpoints opt in to a role
 * with {@link RequiresRole}; everything else (health, ping, user bootstrap) is public.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleAuthorizationInterceptor(userRepository))
                .addPathPatterns("/api/**");
    }
}
