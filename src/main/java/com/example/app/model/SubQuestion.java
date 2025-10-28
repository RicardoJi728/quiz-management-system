package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Entity class representing a sub-question within a main question
@Entity
@Table(name = "sub_questions")
@Data
public class SubQuestion {
    
    // Primary key for the sub-question
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifier for the sub-question (e.g., "a", "b", "c")
    @Column(name = "sub_identifier")
    private String subIdentifier;

    // The actual sub-question text (required field)
    @NotBlank(message = "Sub-question text cannot be empty")
    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    // Marking scheme for the sub-question
    @Column(name = "mark_scheme", columnDefinition = "TEXT")
    private String markScheme;

    // Points awarded for this sub-question
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    // Establishes many-to-one relationship with the parent Question
    // fetch = FetchType.LAZY - parent Question is only loaded when explicitly accessed

    @JoinColumn(name = "question_id")
    // Specifies the foreign key column in the database table

    @JsonBackReference
    // Prevents infinite recursion during JSON serialization by omitting this property
    private Question parentQuestion;
    // References the parent Question this SubQuestion belongs to

    // Standard getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubIdentifier() { return subIdentifier; }
    public void setSubIdentifier(String subIdentifier) { this.subIdentifier = subIdentifier; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getMarkScheme() { return markScheme; }
    public void setMarkScheme(String markScheme) { this.markScheme = markScheme; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Question getParentQuestion() { return parentQuestion; }
    public void setParentQuestion(Question parentQuestion) { this.parentQuestion = parentQuestion; }

    // Custom toString method for better logging
    @Override
    public String toString() {
        return "SubQuestion{" +
            "id=" + id +
            ", subIdentifier='" + subIdentifier + '\'' +
            ", questionText='" + questionText + '\'' +
            ", points=" + points +
            '}';
    }
}