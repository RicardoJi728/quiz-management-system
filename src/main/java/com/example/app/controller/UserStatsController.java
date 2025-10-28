package com.example.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.User;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.UserRepository;

// REST controller for handling user statistics operations
@RestController
@RequestMapping("/api/stats")
public class UserStatsController {
    // Repository dependencies for data access
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    // Constructor with dependency injection
    public UserStatsController(QuestionRepository questionRepository, UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    // Endpoint to get statistics for the currently authenticated user
    @GetMapping("/my-stats")
    public Map<String, Object> getMyStats(@AuthenticationPrincipal UserDetails userDetails) {
        // Find the user by username
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Create response map with user statistics
        Map<String, Object> stats = new HashMap<>();
        // Count total questions created by the user
        stats.put("totalQuestions", questionRepository.countByOwner(user));
        // Count public questions created by the user
        stats.put("publicQuestions", questionRepository.countByOwnerAndPublicFlagTrue(user));
        
        return stats;
    }
} 