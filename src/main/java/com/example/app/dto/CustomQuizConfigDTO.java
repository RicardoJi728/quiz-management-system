package com.example.app.dto;

import java.util.List;

import lombok.Data;

// Data Transfer Object for custom quiz configuration
// Used to create a quiz with specific questions selected by the user
@Data
public class CustomQuizConfigDTO {
    // ID of the question bank containing the selected questions
    private Long bankId;
    
    // Time limit for the quiz in minutes
    private Integer timeLimit;
    
    // List of specific question IDs to include in the quiz
    private List<Long> questionIds;
} 