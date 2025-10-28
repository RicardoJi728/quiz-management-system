package com.example.app.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.QuestionBank;
import com.example.app.model.User;
import com.example.app.repository.QuestionBankRepository;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

// REST controller for handling question bank operations
@Slf4j
@RestController
@RequestMapping("/api/questionbanks")
@CrossOrigin
public class QuestionBankController {
    // Repository dependency for question bank data access
    private final QuestionBankRepository bankRepository;

    // Constructor with dependency injection
    public QuestionBankController(QuestionBankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    // Endpoint to get all question banks owned by the current user
    @GetMapping
    public ResponseEntity<List<QuestionBank>> getBanks(@AuthenticationPrincipal User user) {
        try {
            List<QuestionBank> banks = bankRepository.findByOwner(user);
            log.info("Found {} banks for user {}", banks.size(), user.getUsername());
            return ResponseEntity.ok(banks);
        } catch (Exception e) {
            log.error("Error fetching banks for user {}", user.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint to create a new question bank
    @PostMapping
    public ResponseEntity<?> createBank(@RequestBody QuestionBank bank, @AuthenticationPrincipal User user) {
        try {
            log.info("Creating bank with title: {} for user: {}", bank.getTitle(), user.getUsername());
            
            // Verify user is authenticated
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            // Set bank ownership and creation time
            bank.setOwner(user);
            bank.setCreatedAt(LocalDateTime.now());
            QuestionBank savedBank = bankRepository.save(bank);
            log.info("Successfully created bank with ID: {}", savedBank.getId());
            
            return ResponseEntity.ok(savedBank);
        } catch (Exception e) {
            log.error("Error creating bank", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating bank: " + e.getMessage());
        }
    }

    // Endpoint to get a specific question bank by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBank(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            QuestionBank bank = bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found"));
                
            // Check if user has access to the bank
            if (!bank.getOwner().getId().equals(user.getId()) && !bank.isPublished()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to access this bank");
            }
            
            return ResponseEntity.ok(bank);
        } catch (Exception e) {
            log.error("Error fetching bank {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching bank: " + e.getMessage());
        }
    }

    // Endpoint to get all public question banks
    @GetMapping("/public")
    public ResponseEntity<List<QuestionBank>> getPublicQuestionBanks() {
        return ResponseEntity.ok(bankRepository.findByPublishedTrue());
    }

    // Endpoint to update an existing question bank
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestionBank(@PathVariable Long id,
                                              @Valid @RequestBody QuestionBank questionBank,
                                              @AuthenticationPrincipal User user) {
        return bankRepository.findById(id)
            .map(existing -> {
                // Verify user has permission to update the bank
                if (!existing.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).body("Not authorized");
                }
                // Update bank properties
                existing.setTitle(questionBank.getTitle());
                existing.setDescription(questionBank.getDescription());
                existing.setPublished(questionBank.isPublished());
                return ResponseEntity.ok(bankRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint to delete a question bank
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestionBank(@PathVariable Long id,
                                              @AuthenticationPrincipal User user) {
        return bankRepository.findById(id)
            .map(bank -> {
                // Verify user has permission to delete the bank
                if (!bank.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).body("Not authorized");
                }
                bankRepository.delete(bank);
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint to toggle the published status of a question bank
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> togglePublishQuestionBank(@PathVariable Long id,
                                               @AuthenticationPrincipal User user) {
        return bankRepository.findById(id)
            .map(bank -> {
                // Verify user has permission to publish/unpublish the bank
                if (!bank.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).body("Not authorized");
                }
                bank.setPublished(!bank.isPublished());
                return ResponseEntity.ok(bankRepository.save(bank));
            })
            .orElse(ResponseEntity.notFound().build());
    }
} 