package com.example.app.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.dto.CustomQuizConfigDTO;
import com.example.app.dto.QuizConfigDTO;
import com.example.app.model.Level;
import com.example.app.model.Question;
import com.example.app.model.QuestionBank;
import com.example.app.model.QuestionType;
import com.example.app.model.Quiz;
import com.example.app.model.QuizQuestion;
import com.example.app.model.SubQuestion;
import com.example.app.model.User;
import com.example.app.repository.QuestionBankRepository;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.QuizRepository;

import lombok.extern.slf4j.Slf4j;

// Service class for handling quiz-related operations
// This service manages the creation, submission, and grading of quizzes
@Slf4j
@Service
public class QuizService {
    // Repository dependencies for data access
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuestionBankRepository bankRepository;

    // Constructor with dependency injection
    public QuizService(QuestionRepository questionRepository, 
                      QuizRepository quizRepository,
                      QuestionBankRepository bankRepository) {
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.bankRepository = bankRepository;
    }

    // Create a random quiz based on configuration
    // This method generates a quiz with randomly selected questions based on specified criteria
    @Transactional
    // Annotation that ensures the entire method executes within a database transaction
    // If any part fails, all database changes will be rolled back
    public Quiz createRandomQuiz(QuizConfigDTO config, User user) {
        log.info("Creating quiz with config: {}", config);
        
        // Validate config
        if (config == null) {
            throw new IllegalArgumentException("Quiz configuration cannot be null");
        }
        
        if (config.getLevels() == null || config.getLevels().isEmpty()) {
            throw new IllegalArgumentException("At least one difficulty level must be selected");
        }
        
        // Validate and convert levels - ensure case-sensitive matching
        List<Level> questionLevels;
        try {
            questionLevels = config.getLevels().stream()
                .map(level -> Level.valueOf(level.toUpperCase()))
                .collect(Collectors.toList());
            
            log.info("Selected difficulty levels: {}", questionLevels);
        } catch (IllegalArgumentException e) {
            log.error("Invalid level value received: {}", config.getLevels());
            throw new IllegalArgumentException("Invalid difficulty level. Must be STANDARD or HIGHER");
        }
        
        QuestionBank bank = bankRepository.findById(config.getBankId())
                .orElseThrow(() -> new RuntimeException("Question bank not found"));
        
        Quiz quiz = new Quiz();
        quiz.setUser(user);
        quiz.setBank(bank);
        quiz.setTimeLimit(config.getTimeLimit());
        quiz.setNumberOfQuestions(config.getNumberOfQuestions());
        quiz.setStartTime(LocalDateTime.now());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setPublished(false);
        quiz.setLevel(questionLevels.get(0));
        
        // Create Pageable for limiting results but ensure we get enough questions
        Pageable pageable = PageRequest.of(0, Math.max(config.getNumberOfQuestions() * 3, 100));
        
        // Get questions based on filters
        List<Question> questions;
        if (config.getTopicIds() == null || config.getTopicIds().isEmpty()) {
            // Get questions matching exactly the selected levels
            questions = questionRepository.findByBankIdAndLevelIn(
                config.getBankId(),
                questionLevels,  // Only use the explicitly selected levels
                pageable
            );
            log.info("Fetching questions for levels: {} without topic filter", questionLevels);
        } else {
            // Get questions with both level and topic filters
            questions = questionRepository.findByFilters(
                config.getBankId(),
                questionLevels,  // Only use the explicitly selected levels
                config.getTopicIds(),
                pageable
            );
            log.info("Fetching questions for levels: {} with topic filter: {}", questionLevels, config.getTopicIds());
        }
        
        log.info("Found {} questions matching criteria", questions.size());
        
        if (questions.size() < config.getNumberOfQuestions()) {
            throw new RuntimeException(String.format(
                "Not enough questions available matching the selected criteria. Found %d questions, but %d were requested. " +
                "Please select different difficulty levels or reduce the number of questions.",
                questions.size(), 
                config.getNumberOfQuestions()
            ));
        }
        
        // Randomize and create quiz questions
        Collections.shuffle(questions);
        List<QuizQuestion> quizQuestions = questions.stream()
            .limit(config.getNumberOfQuestions())
            .map(q -> createQuizQuestion(q, quiz))
            .collect(Collectors.toList());
            
        quiz.setQuizQuestions(quizQuestions);
        Quiz savedQuiz = quizRepository.save(quiz);
        log.info("Created quiz with ID: {} containing {} questions of levels: {}", 
            savedQuiz.getId(), savedQuiz.getQuizQuestions().size(), questionLevels);
        
        return savedQuiz;
    }

    // Create a custom quiz with specific questions
    // This method creates a quiz with user-selected questions from a question bank
    @Transactional
    public Quiz createCustomQuiz(CustomQuizConfigDTO config, User user) {
        if (config == null) {
            throw new IllegalArgumentException("Quiz configuration cannot be null");
        }

        if (config.getQuestionIds() == null || config.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("At least one question must be selected");
        }

        QuestionBank bank = bankRepository.findById(config.getBankId())
                .orElseThrow(() -> new RuntimeException("Question bank not found"));

        // Allow quiz creation if user is owner or bank is public
        if (!bank.getOwner().getId().equals(user.getId()) && !bank.isPublished()) {
            throw new RuntimeException("Not authorized to create quiz from this bank");
        }

        List<Question> selectedQuestions = questionRepository.findAllById(config.getQuestionIds());
        
        if (selectedQuestions.size() != config.getQuestionIds().size()) {
            throw new RuntimeException("Some selected questions were not found");
        }

        Quiz quiz = new Quiz();
        quiz.setUser(user);
        quiz.setBank(bank);
        quiz.setTimeLimit(config.getTimeLimit());
        quiz.setNumberOfQuestions(selectedQuestions.size());
        quiz.setStartTime(LocalDateTime.now());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setPublished(false);
        quiz.setLevel(selectedQuestions.get(0).getLevel()); // Set to first question's level

        List<QuizQuestion> quizQuestions = selectedQuestions.stream()
            .map(q -> createQuizQuestion(q, quiz))
            .collect(Collectors.toList());

        quiz.setQuizQuestions(quizQuestions);
        return quizRepository.save(quiz);
    }

    // Helper method to create a quiz question
    // Creates a new QuizQuestion entity linking a Question to a Quiz
    private QuizQuestion createQuizQuestion(Question question, Quiz quiz) {
        QuizQuestion quizQuestion = new QuizQuestion();
        quizQuestion.setQuestion(question);
        quizQuestion.setQuiz(quiz);
        return quizQuestion;
    }

    // Submit answers for a quiz
    // This method processes the submitted answers, calculates the score, and marks the quiz as completed
    @Transactional
    // Ensures atomicity - either all database changes are committed or none are
    public Quiz submitQuiz(Long quizId, List<QuizQuestion> answers, User user) {
        Quiz quiz = getQuiz(quizId);
        
        if (!quiz.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to submit this quiz");
        }
        
        if (quiz.getCompleted()) {
            throw new RuntimeException("Quiz already submitted");
        }
        
        try {
            int totalScore = 0;
            for (QuizQuestion answer : answers) {
                QuizQuestion question = quiz.getQuizQuestions().stream()
                    .filter(q -> q.getId().equals(answer.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Invalid question"));
                    
                question.setUserAnswer(answer.getUserAnswer());
                
                // Handle multiple choice questions
                if (question.getQuestion().getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    // Find the correct option
                    String correctOption = question.getQuestion().getOptions().stream()
                        .filter(opt -> opt.isCorrect())
                        .map(opt -> opt.getOptionText())
                        .findFirst()
                        .orElse(null);
                        
                    boolean isCorrect = question.getUserAnswer() != null && 
                                      question.getUserAnswer().equals(correctOption);
                    question.setCorrect(isCorrect);
                    if (isCorrect) {
                        totalScore += question.getQuestion().getPoints();
                    }
                }
                
                // Store sub-answers if they exist
                if (answer.getSubAnswers() != null && !answer.getSubAnswers().isEmpty()) {
                    question.setSubAnswers(answer.getSubAnswers());
                }
            }
            
            quiz.setScore(totalScore);
            quiz.setCompleted(true);
            quiz.setEndTime(LocalDateTime.now());
            
            // Calculate and set total possible score
            quiz.setTotalPossibleScore(quiz.calculateTotalPossibleScore());
            
            return quizRepository.save(quiz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit quiz: " + e.getMessage());
        }
    }

    // Grade a quiz with custom points per question
    // This method allows for manual grading of quiz questions with custom point allocations
    @Transactional
    // Transaction boundary for the grading process
    // Prevents partial updates to the database
    public Quiz gradeQuiz(Long quizId, Map<Long, List<Integer>> questionPoints, User user) {
        Quiz quiz = getQuiz(quizId);
            
        if (!quiz.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to grade this quiz");
        }
        
        int totalScore = 0;
        
        // First add multiple choice points
        for (QuizQuestion question : quiz.getQuizQuestions()) {
            if (question.getQuestion().getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                if (Boolean.TRUE.equals(question.getCorrect())) {
                    totalScore += question.getQuestion().getPoints();
                }
            }
        }
        
        // Then add manually graded points
        for (QuizQuestion question : quiz.getQuizQuestions()) {
            if (question.getQuestion().getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
                List<Integer> points = questionPoints.get(question.getId());
                if (points != null) {
                    question.setSubPointsEarned(points);
                    totalScore += points.stream().mapToInt(Integer::intValue).sum();
                }
            }
        }
        
        quiz.setScore(totalScore);
        
        // Ensure total possible score is set
        if (quiz.getTotalPossibleScore() == null) {
            quiz.setTotalPossibleScore(quiz.calculateTotalPossibleScore());
        }
        
        return quizRepository.save(quiz);
    }

    // Get all quiz results for a user
    public List<Quiz> getUserQuizResults(User user) {
        return quizRepository.findByUserOrderByStartTimeDesc(user);
    }

    // Flag a question in a quiz for review
    public void flagQuestion(Long quizId, Long questionId) {
        QuizQuestion quizQuestion = quizRepository.findById(quizId)
            .map(quiz -> quiz.getQuizQuestions().stream()
                .filter(qq -> qq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Question not found")))
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
            
        quizQuestion.setFlagged(true);
    }

    // Get a specific quiz by ID
    public Quiz getQuiz(Long quizId) {
        return quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + quizId));
    }

    // Delete a quiz
    // Removes a quiz and its associated data from the system
    @Transactional
    public void deleteQuiz(Long quizId, User user) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
            
        // Check if the user owns the quiz
        if (!quiz.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this quiz");
        }
        
        try {
            quizRepository.delete(quiz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete quiz: " + e.getMessage());
        }
    }

    // Create a practice quiz focusing on questions the user needs to improve
    public Quiz createPracticeQuiz(Long bankId, Integer timeLimit, List<String> levels, User user) {
        if (timeLimit == null || timeLimit < 1) {
            throw new IllegalArgumentException("Time limit must be at least 1 minute");
        }

        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("At least one difficulty level must be selected");
        }

        List<Level> questionLevels = levels.stream()
            .map(level -> Level.valueOf(level.toUpperCase()))
            .collect(Collectors.toList());

        QuestionBank bank = bankRepository.findById(bankId)
            .orElseThrow(() -> new RuntimeException("Question bank not found"));
        
        // Get questions that were answered incorrectly in past quizzes
        List<Question> incorrectQuestions = quizRepository.findByUserOrderByStartTimeDesc(user).stream()
            .flatMap(q -> q.getQuizQuestions().stream())
            .filter(qq -> {
                if (qq.getQuestion() == null) return false;
                
                switch (qq.getQuestion().getQuestionType()) {
                    case MULTIPLE_CHOICE:
                        return !hasCorrectAttempt(qq.getQuestion(), user);
                    case STANDARD:
                        return !hasFullPoints(qq.getQuestion(), user);
                    case WITH_SUBSECTIONS:
                        return !hasAllSubsectionsCorrect(qq.getQuestion(), user);
                    default:
                        return false;
                }
            })
            .filter(qq -> qq.getQuestion().getBank().getId().equals(bankId))
            .filter(qq -> questionLevels.contains(qq.getQuestion().getLevel()))
            .map(QuizQuestion::getQuestion)
            .distinct()
            .collect(Collectors.toList());

        if (incorrectQuestions.isEmpty()) {
            throw new RuntimeException("No incorrect questions found to practice for the selected levels");
        }

        log.info("Found {} questions needing practice", incorrectQuestions.size());

        // Create new quiz with these questions
        Quiz quiz = new Quiz();
        quiz.setUser(user);
        quiz.setBank(bank);
        quiz.setTimeLimit(timeLimit);
        quiz.setStartTime(LocalDateTime.now());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setLevel(questionLevels.get(0));
        quiz.setNumberOfQuestions(incorrectQuestions.size());
        quiz.setPublished(false);

        List<QuizQuestion> quizQuestions = incorrectQuestions.stream()
            .map(q -> createQuizQuestion(q, quiz))
            .collect(Collectors.toList());

        quiz.setQuizQuestions(quizQuestions);
        
        log.info("Created practice quiz with {} questions", quizQuestions.size());
        return quizRepository.save(quiz);
    }

    // Helper method to check if user has a correct attempt for a question
    private boolean hasCorrectAttempt(Question question, User user) {
        return quizRepository.findByUserOrderByStartTimeDesc(user).stream()
            .flatMap(q -> q.getQuizQuestions().stream())
            .filter(qq -> qq.getQuestion().getId().equals(question.getId()))
            .anyMatch(qq -> Boolean.TRUE.equals(qq.getCorrect()));
    }

    // Helper method to check if user has received full points for a question
    private boolean hasFullPoints(Question question, User user) {
        return quizRepository.findByUserOrderByStartTimeDesc(user).stream()
            .flatMap(q -> q.getQuizQuestions().stream())
            .filter(qq -> qq.getQuestion().getId().equals(question.getId()))
            .anyMatch(qq -> {
                if (qq.getSubPointsEarned() == null || qq.getSubPointsEarned().isEmpty()) {
                    return false;
                }
                int earnedPoints = qq.getSubPointsEarned().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
                return earnedPoints == question.getPoints();
            });
    }

    // Helper method to check if user has correct answers for all subsections
    private boolean hasAllSubsectionsCorrect(Question question, User user) {
        return quizRepository.findByUserOrderByStartTimeDesc(user).stream()
            .flatMap(q -> q.getQuizQuestions().stream())
            .filter(qq -> qq.getQuestion().getId().equals(question.getId()))
            .anyMatch(qq -> {
                if (qq.getSubPointsEarned() == null || qq.getSubPointsEarned().isEmpty()) {
                    return false;
                }
                // Check if all subsections have full points
                return qq.getSubPointsEarned().stream()
                    .mapToInt(Integer::intValue)
                    .sum() == question.getSubQuestions().stream()
                        .mapToInt(SubQuestion::getPoints)
                        .sum();
            });
    }

    // Create a full quiz with all questions from a bank
    @Transactional
    public Quiz createFullQuiz(Long bankId, Integer timeLimit, List<String> levels, User user) {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("At least one difficulty level must be selected");
        }

        List<Level> questionLevels = levels.stream()
            .map(level -> Level.valueOf(level.toUpperCase()))
            .collect(Collectors.toList());

        QuestionBank bank = bankRepository.findById(bankId)
            .orElseThrow(() -> new RuntimeException("Question bank not found"));

        List<Question> allQuestions = questionRepository.findByBankIdAndLevelIn(
            bankId,
            questionLevels,
            Pageable.unpaged()
        );

        if (allQuestions.isEmpty()) {
            throw new RuntimeException("No questions found matching the selected criteria");
        }

        Quiz quiz = new Quiz();
        quiz.setUser(user);
        quiz.setBank(bank);
        quiz.setTimeLimit(timeLimit);
        quiz.setStartTime(LocalDateTime.now());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setLevel(questionLevels.get(0));
        quiz.setNumberOfQuestions(allQuestions.size());

        List<QuizQuestion> quizQuestions = allQuestions.stream()
            .map(q -> {
                QuizQuestion qq = new QuizQuestion();
                qq.setQuestion(q);
                qq.setQuiz(quiz);
                return qq;
            })
            .collect(Collectors.toList());

        quiz.setQuizQuestions(quizQuestions);
        return quizRepository.save(quiz);
    }
} 