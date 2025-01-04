package com.example.app.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.app.model.User;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.UserRepository;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/stats")
public class UserStatsController {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public UserStatsController(QuestionRepository questionRepository, UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-stats")
    public Map<String, Object> getMyStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", questionRepository.countByOwner(user));
        stats.put("publicQuestions", questionRepository.countByOwnerAndIsPublicTrue(user));
        
        return stats;
    }
} 