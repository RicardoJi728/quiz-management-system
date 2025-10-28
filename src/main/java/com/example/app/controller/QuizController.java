package com.example.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.dto.CustomQuizConfigDTO;
import com.example.app.dto.QuizConfigDTO;
import com.example.app.model.Quiz;
import com.example.app.model.QuizQuestion;
import com.example.app.model.User;
import com.example.app.service.QuizService;

import lombok.extern.slf4j.Slf4j;

// REST controller for handling quiz-related operations
@Slf4j
@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin
public class QuizController {
    // Service layer dependency for quiz operations
    private final QuizService quizService;

    // Constructor with dependency injection
    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // Endpoint to create a random quiz based on configuration
    @PostMapping("/create")
    public ResponseEntity<?> createRandomQuiz(
            @RequestBody QuizConfigDTO config,
            @AuthenticationPrincipal User user) {
        log.info("Creating random quiz with config: {}", config);
        try {
            Quiz quiz = quizService.createRandomQuiz(config, user);
            return ResponseEntity.ok().body(quiz);
        } catch (IllegalArgumentException e) {
            log.error("Invalid quiz configuration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating quiz", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create quiz: " + e.getMessage()));
        }
    }

    // Endpoint to create a custom quiz with specific questions
    @PostMapping("/create/custom")
    public ResponseEntity<?> createCustomQuiz(
            @RequestBody CustomQuizConfigDTO config,
            @AuthenticationPrincipal User user) {
        log.info("Creating custom quiz with config: {}", config);
        try {
            Quiz quiz = quizService.createCustomQuiz(config, user);
            return ResponseEntity.ok().body(quiz);
        } catch (Exception e) {
            log.error("Error creating custom quiz", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to create a practice quiz from a question bank
    @PostMapping("/practice")
    public ResponseEntity<?> createPracticeQuiz(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User user) {
        try {
            // Extract parameters from request
            Long bankId = Long.parseLong(request.get("bankId").toString());
            Integer timeLimit = Integer.parseInt(request.get("timeLimit").toString());
            @SuppressWarnings("unchecked")
            List<String> levels = (List<String>) request.get("levels");
            
            Quiz quiz = quizService.createPracticeQuiz(bankId, timeLimit, levels, user);
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("Error creating practice quiz", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to submit answers for a quiz
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<?> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody List<QuizQuestion> answers,
            @AuthenticationPrincipal User user) {
        try {
            log.info("Submitting quiz {} with {} answers", quizId, answers.size());
            Quiz submittedQuiz = quizService.submitQuiz(quizId, answers, user);
            return ResponseEntity.ok(submittedQuiz);
        } catch (Exception e) {
            log.error("Error submitting quiz", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to retrieve user's quiz results
    @GetMapping("/my-results")
    public ResponseEntity<?> getMyQuizResults(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(quizService.getUserQuizResults(user));
    }

    // Endpoint to flag a question in a quiz for review
    @PostMapping("/{quizId}/flag/{questionId}")
    public void flagQuestion(@PathVariable Long quizId,
                           @PathVariable Long questionId) {
        quizService.flagQuestion(quizId, questionId);
    }

    // Endpoint to retrieve a specific quiz by ID
    @GetMapping("/{quizId}")
    public ResponseEntity<?> getQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizService.getQuiz(quizId);
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("Error loading quiz", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to get detailed results for a specific quiz
    @GetMapping("/{quizId}/results")
    public ResponseEntity<?> getQuizResults(@PathVariable Long quizId, @AuthenticationPrincipal User user) {
        try {
            Quiz quiz = quizService.getQuiz(quizId);
            
            // Verify user authorization
            if (!quiz.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to view these results");
            }
            
            // Calculate total possible score
            int totalPossibleScore = quiz.getQuizQuestions().stream()
                .mapToInt(q -> q.getQuestion().getPoints() != null ? q.getQuestion().getPoints() : 0)
                .sum();
                
            // Prepare results response
            Map<String, Object> results = new HashMap<>();
            results.put("score", quiz.getScore());
            results.put("totalPossibleScore", totalPossibleScore);
            results.put("startTime", quiz.getStartTime());
            results.put("endTime", quiz.getEndTime());
            results.put("timeLimit", quiz.getTimeLimit());
            results.put("questions", quiz.getQuizQuestions());
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error getting quiz results", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to grade a quiz with custom points per question
    @PostMapping("/{quizId}/grade")
    public ResponseEntity<?> gradeQuiz(
            @PathVariable Long quizId,
            @RequestBody Map<Long, List<Integer>> questionPoints,
            @AuthenticationPrincipal User user) {
        try {
            Quiz gradedQuiz = quizService.gradeQuiz(quizId, questionPoints, user);
            return ResponseEntity.ok(gradedQuiz);
        } catch (Exception e) {
            log.error("Error grading quiz", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to delete a quiz
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long quizId, @AuthenticationPrincipal User user) {
        quizService.deleteQuiz(quizId, user);
        return ResponseEntity.ok().build();
    }

    // Endpoint to create a full quiz from a question bank
    @PostMapping("/create/full")
    public ResponseEntity<?> createFullQuiz(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User user) {
        try {
            // Extract parameters from request
            Long bankId = Long.parseLong(request.get("bankId").toString());
            Integer timeLimit = Integer.parseInt(request.get("timeLimit").toString());
            @SuppressWarnings("unchecked")
            List<String> levels = (List<String>) request.get("levels");
            
            Quiz quiz = quizService.createFullQuiz(bankId, timeLimit, levels, user);
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("Error creating full quiz", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}