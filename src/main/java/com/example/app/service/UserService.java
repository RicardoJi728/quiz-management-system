package com.example.app.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.app.model.User;
import com.example.app.repository.UserRepository;

// Service class for handling user-related operations
// Implements UserDetailsService for Spring Security integration
@Service
public class UserService implements UserDetailsService {
    // Repository dependency for user data access
    private final UserRepository userRepository;

    // Constructor with dependency injection
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Find a user by their username
    // Returns an Optional to handle cases where the user might not exist
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Find a user by their email address
    // Returns an Optional to handle cases where the user might not exist
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Implementation of UserDetailsService interface method
    // Loads user details for Spring Security authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // Save or update a user in the database
    // Returns the saved user entity
    public User save(User user) {
        return userRepository.save(user);
    }
}
