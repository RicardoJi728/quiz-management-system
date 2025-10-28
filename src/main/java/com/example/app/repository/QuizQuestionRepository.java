package com.example.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.model.QuizQuestion;

// Repository interface for QuizQuestion entity operations
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    // Delete all quiz questions associated with a specific question
    // Uses a custom JPQL query for efficient deletion
    @Modifying
    @Transactional
    @Query("DELETE FROM QuizQuestion qq WHERE qq.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
} 