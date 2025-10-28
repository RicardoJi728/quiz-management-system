package com.example.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.model.User;

// Repository interface for User entity operations
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Find a user by their username
    // Returns an Optional to handle cases where the user might not exist
    Optional<User> findByUsername(String username);

    // Find a user by their email address
    // Returns an Optional to handle cases where the user might not exist
    Optional<User> findByEmail(String email);

    // Check if a user with the given username exists
    // Returns true if a user with the username exists, false otherwise
    boolean existsByUsername(String username);

    // Check if a user with the given email exists
    // Returns true if a user with the email exists, false otherwise
    boolean existsByEmail(String email);
}
