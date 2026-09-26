package com.moviebooking.service;

import org.springframework.stereotype.Service;

import com.moviebooking.domain.Role;
import com.moviebooking.domain.User;
import com.moviebooking.exception.ConflictException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.web.dto.CreateUserRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User create(CreateUserRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new ConflictException("User already exists with email: " + request.email());
        });
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .role(request.role() != null ? request.role() : Role.CUSTOMER)
                .build();
        return userRepository.save(user);
    }

    public User get(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
