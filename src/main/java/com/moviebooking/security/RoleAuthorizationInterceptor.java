package com.moviebooking.security;

import java.util.Set;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.moviebooking.domain.Role;
import com.moviebooking.domain.User;
import com.moviebooking.exception.ForbiddenException;
import com.moviebooking.exception.UnauthorizedException;
import com.moviebooking.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authorizes requests by resolving the caller from the {@code X-User-Id} header (the
 * principal an upstream auth layer would supply) and checking their role against the
 * handler's {@link RequiresRole}. Handlers without the annotation are public.
 *
 * <p>On success the resolved user id is stashed as a request attribute so controllers can
 * act as that principal without trusting a client-supplied id in the body.
 */
@RequiredArgsConstructor
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE = "authenticatedUserId";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // not a controller method (e.g. static resource)
        }

        RequiresRole requiresRole = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (requiresRole == null) {
            requiresRole = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        }
        if (requiresRole == null) {
            return true; // public endpoint
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Missing " + USER_ID_HEADER + " header");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Unknown principal: " + userId));

        Set<Role> allowed = Set.of(requiresRole.value());
        if (!allowed.contains(user.getRole())) {
            throw new ForbiddenException("Requires one of roles " + allowed);
        }

        request.setAttribute(PRINCIPAL_ATTRIBUTE, user.getId());
        return true;
    }
}
