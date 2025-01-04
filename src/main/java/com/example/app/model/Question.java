package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question cannot be empty")
    @Size(min = 10, max = 1000, message = "Question must be between 10 and 1000 characters")
    @Column(nullable = false)
    private String question;

    @NotBlank(message = "Mark scheme cannot be empty")
    @Size(min = 10, max = 1000, message = "Mark scheme must be between 10 and 1000 characters")
    @Column(nullable = false)
    private String markScheme;

    @Min(value = 1, message = "Points must be at least 1")
    @Max(value = 100, message = "Points cannot exceed 100")
    private Integer points;

    @NotBlank(message = "Level cannot be empty")
    private String level; // 'Standard' or 'Higher'

    @NotNull(message = "Subject is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User owner;

    @JsonProperty("public")
    private boolean isPublic; // to control visibility to other users

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getMarkScheme() {
        return markScheme;
    }

    public void setMarkScheme(String markScheme) {
        this.markScheme = markScheme;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    @JsonProperty("public")
    public boolean isPublic() {
        return isPublic;
    }

    @JsonProperty("public")
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
