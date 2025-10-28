// Package declaration for model classes
package com.example.app.model;

// Enum representing different types of questions in the quiz system
public enum QuestionType {
    // Standard question type with a single answer
    STANDARD,
    
    // Multiple choice question with predefined options
    MULTIPLE_CHOICE,
    
    // Question with multiple subsections, each with its own answer
    WITH_SUBSECTIONS
}