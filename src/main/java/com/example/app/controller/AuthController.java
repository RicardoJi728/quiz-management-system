package com.example.app.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.User;
import com.example.app.payload.LoginRequest;
import com.example.app.security.JwtUtils;
import com.example.app.service.UserService;

import jakarta.validation.Valid;

// REST controller for handling authentication-related requests
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    // Logger instance for this class
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Required dependencies for authentication and user management
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // Constructor with dependency injection
    public AuthController(AuthenticationManager authenticationManager,
                         UserService userService,
                         PasswordEncoder passwordEncoder,
                         JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    // Endpoint for user registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        logger.info("Received registration request for username: {}", user.getUsername());
        
        try {
            // Log registration attempt details
            logger.debug("Registration data - username: {}, email: {}", 
                user.getUsername(), user.getEmail());

            String password = user.getPassword();
            logger.debug("Password present: {}", (password != null && !password.isEmpty()));

            // Validate password
            if (password == null || password.trim().isEmpty()) {
                logger.error("Password is null or empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password cannot be empty"));
            }

            // Check if username already exists
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                logger.error("Username already exists: {}", user.getUsername());
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username already exists"));
            }

            // Check if email already exists
            if (userService.findByEmail(user.getEmail()).isPresent()) {
                logger.error("Email already exists: {}", user.getEmail());
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already exists"));
            }

            // Encode password and save user
            user.setPassword(passwordEncoder.encode(password.trim()));
            User savedUser = userService.save(user);
            
            // Generate JWT token
            String token = jwtUtils.generateToken(savedUser.getUsername());
            
            logger.info("Successfully registered user: {}", savedUser.getUsername());
            
            // Return success response with token and user details
            return ResponseEntity.ok()
                .body(Map.of(
                    "token", token,
                    "user", savedUser,
                    "message", "Registration successful!"
                ));
        } catch (Exception e) {
            logger.error("Error registering user", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Error registering user: " + e.getMessage()));
        }
    }

    // Endpoint for user login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for user: {}", loginRequest.getUsername());
            
            // Find user and verify existence
            User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Verify password matches
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Invalid password");
            }
            
            // Generate JWT token
            String token = jwtUtils.generateToken(user.getUsername());
            
            logger.info("Login successful for user: {}", user.getUsername());
            
            // Return success response with token and user details
            return ResponseEntity.ok()
                .body(Map.of(
                    "token", token,
                    "user", user,
                    "message", "Login successful!"
                ));
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid username or password"));
        }
    }
}
