package com.example.app.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

// Entity class representing a question within a quiz
// Uses Lombok @Data annotation for automatic getter/setter generation
@Data
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    
    // Primary key for the quiz question
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the parent quiz (many-to-one relationship)
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // Reference to the actual question (many-to-one relationship)
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    // Order of this question in the quiz
    private Integer orderIndex;
    
    // User's answer to the question
    private String userAnswer;
    
    // Whether the user's answer is correct
    private Boolean correct;
    
    // Whether the question is flagged for review
    private Boolean flaggedForReview = false;
    
    // Points earned for this question
    private Integer pointsEarned;

    // Flag indicating if the question is marked
    @Column(name = "flagged")
    private Boolean flagged = false;

    // List of answers for sub-questions (if any)
    @Column(name = "sub_answers", columnDefinition = "text[]")
    private List<String> subAnswers = new ArrayList<>();
    
    // List of points earned for each sub-question
    @ElementCollection
    // Defines a collection of simple values (rather than entities)
    // More lightweight than a full @OneToMany relationship

    @CollectionTable(name = "sub_question_points", 
                    joinColumns = @JoinColumn(name = "quiz_question_id"))
    // Creates a separate table to store the collection values
    // joinColumns - specifies the foreign key column referencing the owner entity

    @Column(name = "points")
    // Specifies the column name in the collection table
    private List<Integer> subPointsEarned = new ArrayList<>();
    // Stores points earned for each sub-question
    // Using a List to maintain order corresponding to sub-questions
}