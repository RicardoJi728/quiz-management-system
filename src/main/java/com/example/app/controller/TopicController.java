package com.example.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.Topic;
import com.example.app.repository.QuestionBankRepository;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.TopicRepository;
import com.example.app.service.TopicService;

// REST controller for handling topic-related operations
@RestController
@RequestMapping("/api/topics")
@CrossOrigin
public class TopicController {
    // Repository and service dependencies
    private final TopicRepository topicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final TopicService topicService;

    // Constructor with dependency injection
    public TopicController(TopicRepository topicRepository, 
                          QuestionBankRepository questionBankRepository, 
                          QuestionRepository questionRepository, 
                          TopicService topicService) {
        this.topicRepository = topicRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionRepository = questionRepository;
        this.topicService = topicService;
    }

    // Endpoint to get all topics with their question counts
    @GetMapping
    public List<TopicService.TopicWithQuestionCount> getAllTopics() {
        return topicService.getAllTopicsWithQuestionCount();
    }

    // Endpoint to create a new topic
    @PostMapping
    public ResponseEntity<?> createTopic(@RequestBody Map<String, Object> request) {
        try {
            // Extract topic name and bank ID from request
            String name = (String) request.get("name");
            Map<String, Object> bankMap = (Map<String, Object>) request.get("bank");
            Long bankId = Long.valueOf(bankMap.get("id").toString());
            
            // Check for duplicate topic names in the same bank
            Optional<Topic> existingTopic = topicRepository.findByNameIgnoreCaseAndBankId(name, bankId);
            if (existingTopic.isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Topic already exists in this question bank"));
            }
            
            // Create and save new topic
            Topic topic = new Topic();
            topic.setName(name);
            topic.setBankId(bankId);
            
            Topic savedTopic = topicRepository.save(topic);
            return ResponseEntity.ok(savedTopic);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Endpoint to get a specific topic by ID
    @GetMapping("/{id}")
    public ResponseEntity<Topic> getTopic(@PathVariable Long id) {
        return topicRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint to delete a topic
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTopic(@PathVariable Long id, @RequestParam(required = false) Long bankId) {
        try {
            topicService.deleteTopic(id, bankId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to get all topics for a specific question bank with question counts
    @GetMapping("/bank/{bankId}")
    public ResponseEntity<List<TopicService.TopicWithQuestionCount>> getTopicsByBank(@PathVariable Long bankId) {
        try {
            // Get topics associated with the bank
            List<Topic> topics = topicRepository.findByBankId(bankId);
            // Get question counts for each topic
            Map<Long, Long> questionCounts = questionRepository.countQuestionsByTopicId();
            
            // Combine topics with their question counts
            List<TopicService.TopicWithQuestionCount> topicsWithCount = topics.stream()
                .map(topic -> new TopicService.TopicWithQuestionCount(
                    topic.getId(),
                    topic.getName(),
                    questionCounts.getOrDefault(topic.getId(), 0L)))
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(topicsWithCount);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 