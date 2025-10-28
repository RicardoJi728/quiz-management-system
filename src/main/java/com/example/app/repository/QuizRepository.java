package com.example.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.model.Quiz;
import com.example.app.model.User;

// Repository interface for Quiz entity operations
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // Find all quizzes created by a specific user
    List<Quiz> findByUser(User user);

    // Find all quizzes created by a specific user, ordered by start time (newest first)
    List<Quiz> findByUserOrderByStartTimeDesc(User user);

    // Find all quizzes associated with a specific question bank
    List<Quiz> findByBankId(Long bankId);
} 