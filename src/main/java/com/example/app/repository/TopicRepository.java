package com.example.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.app.model.Topic;

// Repository interface for Topic entity operations
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    // Find all topics associated with a specific question bank
    @Query("SELECT t FROM Topic t WHERE t.bankId = :bankId")
    List<Topic> findByBankId(@Param("bankId") Long bankId);
    
    // Find a topic by name and bank ID (case-insensitive)
    // Returns an Optional to handle cases where the topic might not exist
    Optional<Topic> findByNameIgnoreCaseAndBankId(String name, Long bankId);
} 