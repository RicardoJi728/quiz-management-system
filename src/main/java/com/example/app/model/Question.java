package com.example.app.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

// Entity class representing a question in the quiz system
@Entity
@Table(name = "questions")
public class Question {

    // Primary key for the question
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The actual question text
    @Column(name = "question_text")
    private String questionText;

    // Marking scheme for the question
    @Column(name = "mark_scheme")
    private String markScheme;

    // Points awarded for this question
    @Column(name = "points")
    private Integer points;

    // Difficulty level of the question (required field)
    @NotNull(message = "Level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private Level level;

    // Reference to the question bank this question belongs to
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private QuestionBank bank;

    // User who created/owns this question
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // Flag indicating if the question is public
    @Column(name = "public_flag", nullable = false)
    private boolean publicFlag = false;

    // Date when the question was published
    @Column(name = "publication_date")
    private LocalDate publicationDate;

    // Additional information or notes about the question
    @Column(name = "additional_info")
    private String additionalInfo;

    // Type of question (e.g., standard, multiple choice)
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType = QuestionType.STANDARD;

    // Flag indicating if the question has sub-questions
    @Column(name = "has_sub_questions")
    private Boolean hasSubQuestions = false;

    // List of sub-questions associated with this question
    @OneToMany(mappedBy = "parentQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    // This annotation establishes a one-to-many relationship between Question and SubQuestion
    // mappedBy = "parentQuestion" - references the field in SubQuestion that owns the relationship
    // cascade = CascadeType.ALL - all operations (persist, remove, etc.) cascade to the children
    // orphanRemoval = true - removes SubQuestions when they're no longer referenced
    // fetch = FetchType.EAGER - loads all SubQuestions when the parent Question is retrieved
    private List<SubQuestion> subQuestions = new ArrayList<>();

    // List of multiple choice options for this question
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    // Defines a one-to-many relationship between Question and MultipleChoiceOption
    // mappedBy = "question" - references the field in MultipleChoiceOption that owns the relationship
    // cascade = CascadeType.ALL - operations on Question cascade to related options
    // orphanRemoval = true - removes options when they're no longer referenced by this Question
    private List<MultipleChoiceOption> options = new ArrayList<>();
    // Stores the list of multiple choice options for this question
    // Initialized as an empty ArrayList to avoid null pointer exceptions

    // Topic this question belongs to
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    // Correct option for multiple choice questions
    @Column(name = "correct_option")
    private String correctOption;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getMarkScheme() {
        return markScheme;
    }

    public void setMarkScheme(String markScheme) {
        this.markScheme = markScheme;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public QuestionBank getBank() {
        return bank;
    }

    public void setBank(QuestionBank bank) {
        this.bank = bank;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean getPublicFlag() {
        return publicFlag;
    }

    public void setPublicFlag(boolean publicFlag) {
        this.publicFlag = publicFlag;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public boolean isHasSubQuestions() {
        return hasSubQuestions;
    }

    public void setHasSubQuestions(boolean hasSubQuestions) {
        this.hasSubQuestions = hasSubQuestions;
    }

    public List<SubQuestion> getSubQuestions() {
        return subQuestions;
    }

    public void setSubQuestions(List<SubQuestion> subQuestions) {
        this.subQuestions.clear();
        if (subQuestions != null) {
            for (SubQuestion subQuestion : subQuestions) {
                addSubQuestion(subQuestion);
            }
        }
    }

    public void addSubQuestion(SubQuestion subQuestion) {
        subQuestions.add(subQuestion);
        subQuestion.setParentQuestion(this);
        this.hasSubQuestions = true;
    }

    public void removeSubQuestion(SubQuestion subQuestion) {
        subQuestions.remove(subQuestion);
        subQuestion.setParentQuestion(null);
        if (subQuestions.isEmpty()) {
            this.hasSubQuestions = false;
        }
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<MultipleChoiceOption> getOptions() {
        return options;
    }

    public void setOptions(List<MultipleChoiceOption> options) {
        this.options.clear();
        if (options != null) {
            for (MultipleChoiceOption option : options) {
                addOption(option);
            }
        }
    }

    public void addOption(MultipleChoiceOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void removeOption(MultipleChoiceOption option) {
        options.remove(option);
        option.setQuestion(null);
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }
}
