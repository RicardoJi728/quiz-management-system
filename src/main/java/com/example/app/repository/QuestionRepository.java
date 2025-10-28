package com.example.app.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.app.dto.TopicDTO;
import com.example.app.model.Level;
import com.example.app.model.Question;
import com.example.app.model.QuestionBank;
import com.example.app.model.User;

// Repository interface for Question entity operations
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // Find all questions in a specific question bank
    List<Question> findByBankId(Long bankId);

    // Find all questions owned by a specific user
    List<Question> findByOwner(User owner);

    // Search questions by text content (case-insensitive)
    List<Question> findByQuestionTextContainingIgnoreCase(String keyword);

    // Find questions by difficulty level
    List<Question> findByLevel(String level);

    // Find questions by bank and difficulty level
    List<Question> findByBankAndLevel(QuestionBank bank, String level);

    // Count total questions owned by a user
    Long countByOwner(User owner);

    // Count public questions owned by a user
    Long countByOwnerAndPublicFlagTrue(User owner);

    // Find topics with their question counts for a specific bank
    // Returns a list of TopicDTO objects containing topic details and question counts
    @Query("SELECT new com.example.app.dto.TopicDTO(t.id, t.name, COUNT(q)) " +
           "FROM Question q " +
           "JOIN q.topic t " +
           "WHERE q.bank.id = :bankId " +
           "GROUP BY t.id, t.name")
    // Custom JPQL query that:
    // - Creates TopicDTO objects directly from the query results
    // - Joins Question and Topic tables
    // - Filters by the question bank ID
    // - Groups results by topic ID and name
    // - Counts questions in each topic

    List<TopicDTO> findTopicsWithQuestionCount(@Param("bankId") Long bankId);
    // Returns a list of TopicDTO objects with question counts
    // @Param annotation binds the method parameter to the named parameter in the query

    // Find questions by bank ID and topic ID
    @Query("SELECT q FROM Question q WHERE q.bank.id = :bankId AND q.topic.id = :topicId")
    List<Question> findByBankIdAndTopicId(@Param("bankId") Long bankId, @Param("topicId") Long topicId);

    // Find questions based on multiple filters (bank, levels, topics)
    // Supports pagination for large result sets
    @Query("SELECT q FROM Question q " +
           "WHERE q.bank.id = :bankId " +
           "AND q.level IN :levels " +
           "AND q.topic.id IN :topicIds")
    // Custom JPQL query that:
    // - Selects Question entities
    // - Filters by question bank ID
    // - Filters by multiple difficulty levels
    // - Filters by multiple topic IDs

    List<Question> findByFilters(
        @Param("bankId") Long bankId,      // Question bank ID parameter
        @Param("levels") List<Level> levels,   // List of difficulty levels to include
        @Param("topicIds") List<Long> topicIds,  // List of topic IDs to include
        Pageable pageable                       // Pagination information
    );
    // Returns filtered questions with pagination support

    // Find questions by bank ID and multiple difficulty levels
    // Supports pagination for large result sets
    List<Question> findByBankIdAndLevelIn(Long bankId, List<Level> levels, Pageable pageable);

    // Check if any questions exist for a specific topic
    boolean existsByTopicId(Long topicId);

    // Check if any questions exist for a specific topic (using JPA naming convention)
    boolean existsByTopic_Id(Long topicId);
    
    // Get question counts grouped by topic
    @Query("SELECT q.topic.id as topicId, COUNT(q) as count FROM Question q GROUP BY q.topic.id")
    List<Object[]> countQuestionsGroupByTopic();
    
    // Convert question counts to a map of topic ID to count
    default Map<Long, Long> countQuestionsByTopicId() {
        // Default method implementation in the repository interface
        // Transforms raw query results into a Map for easier consumption

        return countQuestionsGroupByTopic().stream()
            // Get the stream of Object[] results from the query method
            .collect(Collectors.toMap(
                row -> (Long) row[0],    // Use topic ID (first array element) as map key
                row -> (Long) row[1]     // Use count (second array element) as map value
            ));
        // Collects results into a Map<TopicId, QuestionCount>
    }

    // Check if a topic is used in any other question bank
    boolean existsByTopic_IdAndBank_IdNot(Long topicId, Long bankId);

    // Delete all questions with a specific topic in a specific bank
    void deleteByTopic_IdAndBank_Id(Long topicId, Long bankId);
}
