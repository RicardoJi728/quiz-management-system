package com.example.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

// Entity class representing a quiz in the system
// Uses Lombok @Data annotation for automatic getter/setter generation
@Data
@Entity
@Table(name = "quizzes")
// Include only non-null fields in JSON serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Quiz {
    
    // Primary key for the quiz
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Time limit for completing the quiz (in minutes)
    private Integer timeLimit;
    
    // Number of questions in the quiz
    private Integer numberOfQuestions;
    
    // Topics covered in this quiz (many-to-many relationship)
    @JsonIgnore
    @ManyToMany
    @JoinTable(name = "quiz_topics")
    private List<Topic> selectedTopics;
    
    // Quiz timing information
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Quiz results
    private Integer score;
    private Boolean completed = false;
    
    // Questions in this quiz (one-to-many relationship)
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    // Defines a one-to-many relationship between Quiz and QuizQuestion
    // mappedBy = "quiz" - references the field in QuizQuestion that owns the relationship
    // cascade = CascadeType.ALL - operations on Quiz cascade to related QuizQuestions
    // fetch = FetchType.EAGER - loads all QuizQuestions whenever a Quiz is retrieved
    private List<QuizQuestion> quizQuestions = new ArrayList<>();
    // Stores the list of questions in this quiz
    // Initialized as an empty ArrayList to avoid null pointer exceptions

    // Quiz metadata
    private LocalDateTime createdAt;

    // Difficulty level of the quiz (required field)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Level level;

    // Publication status of the quiz (required field)
    @Column(nullable = false)
    private Boolean published = false;

    // User who created/takes this quiz
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    // Question bank this quiz belongs to
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private QuestionBank bank;

    // Total possible score for the quiz
    private Integer totalPossibleScore;

    // Custom getter for quiz ID with JSON property name
    @JsonProperty("quizId")
    public Long getQuizId() {
        return id;
    }

    // Getter for question count with JSON property
    @JsonProperty
    public int getQuestionCount() {
        return quizQuestions != null ? quizQuestions.size() : 0;
    }

    // Calculate the total possible score for the quiz
    public Integer calculateTotalPossibleScore() {
        if (quizQuestions == null) return 0;
        
        return quizQuestions.stream()
            .map(qq -> {
                // Handle questions with subsections
                if (qq.getQuestion().getQuestionType() == QuestionType.WITH_SUBSECTIONS) {
                    // Sum up points from all subsections
                    return qq.getQuestion().getSubQuestions().stream()
                        .mapToInt(SubQuestion::getPoints)
                        .sum();
                } else {
                    // Handle standard and multiple choice questions
                    return qq.getQuestion().getPoints();
                }
            })
            .mapToInt(Integer::intValue)
            .sum();
    }

    // Standard getter and setter for total possible score
    public Integer getTotalPossibleScore() {
        return totalPossibleScore;
    }

    public void setTotalPossibleScore(Integer totalPossibleScore) {
        this.totalPossibleScore = totalPossibleScore;
    }
} 