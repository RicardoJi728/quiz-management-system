// Package declaration for controllers
package com.example.app.controller;

// Import required Spring and application classes
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.dto.TopicDTO;
import com.example.app.model.Question;
import com.example.app.model.QuestionBank;
import com.example.app.model.QuestionType;
import com.example.app.model.User;
import com.example.app.repository.QuestionBankRepository;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.QuizQuestionRepository;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

// REST controller for handling question-related operations
@Slf4j
@RestController
@RequestMapping("/api/questions")
@CrossOrigin
public class QuestionController {
    // Repository dependencies for data access
    private final QuestionRepository questionRepository;
    private final QuestionBankRepository bankRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    // Constructor with dependency injection
    public QuestionController(QuestionRepository questionRepository, 
                            QuestionBankRepository bankRepository,
                            QuizQuestionRepository quizQuestionRepository) {
        this.questionRepository = questionRepository;
        this.bankRepository = bankRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    // Endpoint to search questions by keyword or level
    @GetMapping("/search")
    public List<Question> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level) {
        
        if (keyword != null) {
            return questionRepository.findByQuestionTextContainingIgnoreCase(keyword);
        } else if (level != null) {
            return questionRepository.findByLevel(level);
        }
        return questionRepository.findAll();
    }

    // Endpoint to get all questions created by the current user
    @GetMapping("/my-questions")
    public List<Question> getMyQuestions(@AuthenticationPrincipal User user) {
        return questionRepository.findByOwner(user);
    }

    // Endpoint to create a new question
    @PostMapping
    public ResponseEntity<?> createQuestion(@Valid @RequestBody Question question, @AuthenticationPrincipal User user) {
        try {
            // Verify question bank exists and user has permission
            QuestionBank bank = bankRepository.findById(question.getBank().getId())
                .orElseThrow(() -> new RuntimeException("Bank not found"));
                
            if (!bank.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to add questions to this bank");
            }

            Question savedQuestion = questionRepository.save(question);
            return ResponseEntity.ok(savedQuestion);
        } catch (Exception e) {
            log.error("Error creating question", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating question: " + e.getMessage());
        }
    }

    // Endpoint to get all questions in a specific bank
    @GetMapping("/bank/{bankId}")
    public ResponseEntity<List<Question>> getQuestionsByBank(@PathVariable Long bankId) {
        try {
            List<Question> questions = questionRepository.findByBankId(bankId);
            log.info("Found {} questions for bank {}", questions.size(), bankId);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            log.error("Error fetching questions for bank {}", bankId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint to delete a question and its related data
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
                
            // Verify user has permission to delete the question
            if (question.getBank() != null && 
                question.getBank().getOwner() != null && 
                question.getBank().getOwner().getId().equals(user.getId())) {
                
                // Delete related quiz questions
                quizQuestionRepository.deleteByQuestionId(id);
                
                // Clear multiple choice options
                if (question.getOptions() != null) {
                    question.getOptions().clear();
                }
                
                // Clear sub-questions
                if (question.getSubQuestions() != null) {
                    question.getSubQuestions().clear();
                }
                
                // Delete the question
                questionRepository.delete(question);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to delete this question");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Error deleting question: " + e.getMessage());
        }
    }

    // Endpoint to update an existing question
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id,
                                          @Valid @RequestBody Question question,
                                          @AuthenticationPrincipal User user) {
        try {
            log.info("Updating question {} with data: {}", id, question);
            return questionRepository.findById(id)
                .map(existingQuestion -> {
                    // Verify user has permission to update the question
                    if (!existingQuestion.getBank().getOwner().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body("Not authorized");
                    }
                    
                    // Preserve existing relationships
                    question.setOwner(existingQuestion.getOwner());
                    question.setBank(existingQuestion.getBank());
                    
                    // Update basic question fields
                    existingQuestion.setQuestionText(question.getQuestionText());
                    existingQuestion.setMarkScheme(question.getMarkScheme());
                    existingQuestion.setPoints(question.getPoints());
                    existingQuestion.setLevel(question.getLevel());
                    existingQuestion.setPublicationDate(question.getPublicationDate());
                    existingQuestion.setAdditionalInfo(question.getAdditionalInfo());
                    existingQuestion.setTopic(question.getTopic());
                    existingQuestion.setQuestionType(question.getQuestionType());
                    
                    // Handle subsections for questions with subsections
                    if (question.getQuestionType() == QuestionType.WITH_SUBSECTIONS) {
                        existingQuestion.setHasSubQuestions(true);
                        existingQuestion.getSubQuestions().clear();
                        if (question.getSubQuestions() != null) {
                            question.getSubQuestions().forEach(subQuestion -> {
                                subQuestion.setParentQuestion(existingQuestion);
                                existingQuestion.getSubQuestions().add(subQuestion);
                            });
                        }
                    } else {
                        existingQuestion.setHasSubQuestions(false);
                        existingQuestion.getSubQuestions().clear();
                    }
                    
                    // Handle multiple choice options
                    if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                        existingQuestion.getOptions().clear();
                        if (question.getOptions() != null) {
                            question.getOptions().forEach(option -> {
                                option.setQuestion(existingQuestion);
                                existingQuestion.getOptions().add(option);
                            });
                        }
                    }
                    
                    Question updatedQuestion = questionRepository.save(existingQuestion);
                    log.info("Question updated successfully: {}", updatedQuestion);
                    return ResponseEntity.ok(updatedQuestion);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating question", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Endpoint to get all topics in a question bank with question counts
    @GetMapping("/bank/{bankId}/topics")
    public List<TopicDTO> getTopicsInBank(@PathVariable Long bankId) {
        return questionRepository.findTopicsWithQuestionCount(bankId);
    }

    // Endpoint to get all questions for a specific topic in a bank
    @GetMapping("/bank/{bankId}/topic/{topicId}")
    public List<Question> getQuestionsForTopic(
        @PathVariable Long bankId,
        @PathVariable Long topicId
    ) {
        return questionRepository.findByBankIdAndTopicId(bankId, topicId);
    }

    // Endpoint to get all questions in a bank with authorization check
    @GetMapping("/bank/{bankId}/all")
    public ResponseEntity<?> getAllQuestionsInBank(@PathVariable Long bankId, @AuthenticationPrincipal User user) {
        try {
            QuestionBank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Bank not found"));
                
            // Check if user has access to the bank
            if (!bank.getOwner().getId().equals(user.getId()) && !bank.isPublished()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to access this bank");
            }

            List<Question> questions = questionRepository.findByBankId(bankId);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            log.error("Error fetching questions for bank {}", bankId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching questions: " + e.getMessage());
        }
    }
}
