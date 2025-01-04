package com.example.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.Subject;
import com.example.app.repository.SubjectRepository;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubjectController.class);
    private final SubjectRepository subjectRepository;

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        List<Subject> subjects = subjectRepository.findAll();
        logger.info("Found {} subjects", subjects.size());
        subjects.forEach(s -> logger.info("Subject: {}", s));
        return ResponseEntity.ok(subjects);
    }

    @PostMapping
    public ResponseEntity<Subject> createSubject(@RequestBody Subject subject) {
        Subject saved = subjectRepository.save(subject);
        logger.info("Created subject: {}", saved);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable Long id) {
        return subjectRepository.findById(id)
            .map(subject -> {
                logger.info("Found subject: {}", subject);
                return ResponseEntity.ok(subject);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllSubjects() {
        subjectRepository.deleteAll();
        logger.info("Deleted all subjects");
        return ResponseEntity.ok().build();
    }
} 