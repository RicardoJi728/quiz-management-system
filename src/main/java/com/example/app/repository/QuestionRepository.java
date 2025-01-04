package com.example.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.model.Question;
import com.example.app.model.Subject;
import com.example.app.model.User;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubjectAndLevel(Subject subject, String level);
    List<Question> findBySubject(Subject subject);
    List<Question> findByLevel(String level);
    List<Question> findByOwner(User user);
    List<Question> findByIsPublicTrue();
    List<Question> findByQuestionContainingIgnoreCase(String keyword);
    long countByOwner(User user);
    long countByOwnerAndIsPublicTrue(User user);
}
