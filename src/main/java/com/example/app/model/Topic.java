// Package declaration for model classes
package com.example.app.model;

// Import required JPA and validation annotations
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

// Entity class representing a topic in the quiz system
@Entity
@Table(name = "topics")
public class Topic {
    
    // Primary key for the topic
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Name of the topic (must be unique and cannot be empty)
    @NotBlank(message = "Topic name cannot be empty")
    @Column(unique = true)
    private String name;

    // Reference to the question bank this topic belongs to
    @Column(name = "bank_id")
    private Long bankId;

    // Standard getters and setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }
} 