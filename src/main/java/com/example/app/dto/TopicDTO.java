package com.example.app.dto;

// Data Transfer Object for Topic information
// Used to transfer topic data between layers of the application
public class TopicDTO {
    // Unique identifier of the topic
    private Long id;
    
    // Name of the topic
    private String name;
    
    // Number of questions associated with this topic
    private Long questionCount;

    // Constructor with all fields
    public TopicDTO(Long id, String name, Long questionCount) {
        this.id = id;
        this.name = name;
        this.questionCount = questionCount;
    }

    // Getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getQuestionCount() { return questionCount; }
    public void setQuestionCount(Long questionCount) { this.questionCount = questionCount; }
} 