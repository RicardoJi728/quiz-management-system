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

// Entity class representing a multiple choice option for a question
@Entity
@Table(name = "multiple_choice_options")
public class MultipleChoiceOption {
    
    // Primary key for the option
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The text of the option (required field)
    @NotBlank(message = "Option text cannot be empty")
    @Column(name = "option_text", columnDefinition = "TEXT")
    private String optionText;

    // Flag indicating if this is the correct option
    @Column(name = "is_correct")
    private boolean correct;

    // Reference to the parent question (many-to-one relationship)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    @JsonBackReference
    private Question question;

    // Default constructor
    public MultipleChoiceOption() {}

    // Constructor with option text and correctness
    public MultipleChoiceOption(String optionText, boolean correct) {
        this.optionText = optionText;
        this.correct = correct;
    }

    // Standard getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
} 