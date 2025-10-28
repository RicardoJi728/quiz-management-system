package com.example.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.model.QuestionBank;
import com.example.app.model.User;

// Repository interface for QuestionBank entity operations
@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    // Find all question banks owned by a specific user
    List<QuestionBank> findByOwner(User owner);

    // Find all published question banks
    List<QuestionBank> findByPublishedTrue();
} 