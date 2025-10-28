package com.example.app.security;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.app.repository.UserRepository;

// Custom implementation of UserDetailsService for Spring Security
@Service
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    // Repository for user data access
    private final UserRepository userRepository;

    // Constructor with dependency injection
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Load user details by username for authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
} 