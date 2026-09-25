package com.moviebooking.web.dto;

import com.moviebooking.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code role} is optional and defaults to CUSTOMER when omitted. */
public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        Role role) {
}
