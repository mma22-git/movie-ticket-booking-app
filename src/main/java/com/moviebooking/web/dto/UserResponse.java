package com.moviebooking.web.dto;

import com.moviebooking.domain.User;

public record UserResponse(String id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
