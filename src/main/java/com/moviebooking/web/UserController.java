package com.moviebooking.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.domain.Role;
import com.moviebooking.security.RequiresRole;
import com.moviebooking.service.UserService;
import com.moviebooking.web.dto.CreateUserRequest;
import com.moviebooking.web.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Public bootstrap endpoint: creates a user with the requested role (default CUSTOMER). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.create(request));
    }

    @GetMapping("/{id}")
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public UserResponse get(@PathVariable String id) {
        return UserResponse.from(userService.get(id));
    }
}
