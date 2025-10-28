package com.example.app.dto;

import java.util.List;

import lombok.Data;

// Data Transfer Object for quiz configuration
// Used to create a random quiz with specified parameters
@Data
public class QuizConfigDTO {
    // ID of the question bank to use for the quiz
    private Long bankId;
    
    // Time limit for the quiz in minutes
    private Integer timeLimit;
    
    // Number of questions to include in the quiz
    private Integer numberOfQuestions;
    
    // List of difficulty levels to include in the quiz
    private List<String> levels;
    
    // List of topic IDs to filter questions by
    private List<Long> topicIds;
} 