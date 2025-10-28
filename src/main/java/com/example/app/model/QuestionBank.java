package com.example.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

// Entity class representing a collection of questions
// Uses Lombok @Data annotation for automatic getter/setter generation
@Data
@Entity
@Table(name = "question_banks")
public class QuestionBank {
    
    // Primary key for the question bank
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Title of the question bank (required field)
    @Column(nullable = false)
    private String title;

    // Optional description of the question bank
    private String description;

    // User who owns this question bank (required field)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // List of questions in this bank (one-to-many relationship)
    @JsonManagedReference
    @OneToMany(mappedBy = "bank", cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();

    // Timestamp when the question bank was created
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Publication status of the question bank (required field)
    @Column(name = "published", nullable = false)
    private boolean published = false;

    // Lifecycle callback to set creation timestamp before persisting
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Custom getter and setter for published status
    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    // Custom getter and setter for creation timestamp
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}