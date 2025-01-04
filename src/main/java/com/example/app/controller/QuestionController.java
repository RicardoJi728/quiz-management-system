package com.example.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.Question;
import com.example.app.model.Subject;
import com.example.app.model.User;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.SubjectRepository;
import com.example.app.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public QuestionController(QuestionRepository questionRepository, 
                            UserRepository userRepository,
                            SubjectRepository subjectRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@Valid @RequestBody Question question,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        question.setOwner(user);
        
        if (question.getSubject() != null && question.getSubject().getId() != null) {
            Subject subject = subjectRepository.findById(question.getSubject().getId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
            question.setSubject(subject);
        }
        
        Question savedQuestion = questionRepository.save(question);
        return ResponseEntity.ok(savedQuestion);
    }

    // Get questions by subject and level
    @GetMapping("/{subjectId}/{level}")
    public List<Question> getQuestionsBySubjectAndLevel(@PathVariable Long subjectId, @PathVariable String level) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new RuntimeException("Subject not found"));
        return questionRepository.findBySubjectAndLevel(subject, level);
    }

    // Update a question
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, 
                                                 @RequestBody Question question,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        Question existingQuestion = questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Question not found"));
            
        // Check if the user is the owner of the question
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!existingQuestion.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Update the question while preserving the owner
        question.setId(id);
        question.setOwner(existingQuestion.getOwner());
        
        return ResponseEntity.ok(questionRepository.save(question));
    }

    // Delete a question
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (!questionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        questionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-questions")
    public List<Question> getMyQuestions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        return questionRepository.findByOwner(user);
    }

    @GetMapping("/public")
    public List<Question> getPublicQuestions() {
        return questionRepository.findByIsPublicTrue();
    }

    @GetMapping("/search")
    public List<Question> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String level) {
        
        if (keyword != null) {
            return questionRepository.findByQuestionContainingIgnoreCase(keyword);
        } else if (subject != null && level != null) {
            List<Subject> subjects = subjectRepository.findBySubjectNameIgnoreCase(subject);
            if (subjects.isEmpty()) {
                throw new RuntimeException("Subject not found: " + subject);
            }
            return questionRepository.findBySubjectAndLevel(subjects.get(0), level);
        } else if (subject != null) {
            List<Subject> subjects = subjectRepository.findBySubjectNameIgnoreCase(subject);
            if (subjects.isEmpty()) {
                throw new RuntimeException("Subject not found: " + subject);
            }
            return questionRepository.findBySubject(subjects.get(0));
        } else if (level != null) {
            return questionRepository.findByLevel(level);
        }
        return questionRepository.findAll();
    }
}
