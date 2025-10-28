// Package declaration for services
package com.example.app.service;

// Import required Spring and application classes
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.model.Topic;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.TopicRepository;

// Service class for handling topic-related operations
@Service
public class TopicService {
    // Repository dependencies for data access
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    // Constructor with dependency injection
    public TopicService(TopicRepository topicRepository, QuestionRepository questionRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
    }

    // Get all topics with their associated question counts
    // Returns a list of TopicWithQuestionCount records containing topic details and usage statistics
    public List<TopicWithQuestionCount> getAllTopicsWithQuestionCount() {
        // Fetch all topics
        List<Topic> topics = topicRepository.findAll();
        // Get question counts for each topic
        Map<Long, Long> questionCounts = questionRepository.countQuestionsByTopicId();
        
        // Map topics to records containing topic details and question counts
        return topics.stream()
            .map(topic -> new TopicWithQuestionCount(
                topic.getId(),
                topic.getName(),
                questionCounts.getOrDefault(topic.getId(), 0L)
            ))
            .collect(Collectors.toList());
    }

    // Delete a topic and its associated questions
    // If bankId is provided, only deletes questions in that specific bank
    @Transactional
    public void deleteTopic(Long id, Long bankId) {
        // Handle bank-specific topic deletion
        if (bankId != null) {
            // Check if topic is used in other banks
            if (questionRepository.existsByTopic_IdAndBank_IdNot(id, bankId)) {
                throw new RuntimeException("Cannot delete topic: It is being used in other question banks");
            }
            // Delete questions with this topic in the specified bank
            questionRepository.deleteByTopic_IdAndBank_Id(id, bankId);
        } else {
            // Handle global topic deletion
            if (questionRepository.existsByTopic_Id(id)) {
                throw new RuntimeException("Cannot delete topic: It is being used by one or more questions");
            }
        }
        // Delete the topic
        topicRepository.deleteById(id);
    }

    // Record class to represent a topic with its question count
    public record TopicWithQuestionCount(Long id, String name, Long questionCount) {}
} 