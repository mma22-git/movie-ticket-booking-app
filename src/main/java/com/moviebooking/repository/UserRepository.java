package com.moviebooking.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
}
