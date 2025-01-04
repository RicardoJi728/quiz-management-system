package com.example.app;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.model.Subject;
import com.example.app.repository.SubjectRepository;

@Component
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final SubjectRepository subjectRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(SubjectRepository subjectRepository, JdbcTemplate jdbcTemplate) {
        this.subjectRepository = subjectRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        logger.info("Starting data seeding...");
        
        // Check if table exists
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subject", Integer.class);
            logger.info("Found {} subjects in database", count);
        } catch (Exception e) {
            logger.error("Error checking subjects table: {}", e.getMessage());
            // Create table if it doesn't exist
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS subject (" +
                               "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                               "subject_name VARCHAR(255) UNIQUE)");
            logger.info("Created subjects table");
        }
        
        Arrays.asList(
            "Computer Science",
            "Physics",
            "Mathematics",
            "Chemistry"
        ).forEach(this::createSubjectIfNotExists);

        // Verify subjects after creation
        logger.info("Data seeding completed. Subjects in database:");
        jdbcTemplate.query("SELECT * FROM subject", 
            (rs, rowNum) -> String.format("ID: %d, Name: %s", rs.getLong("id"), rs.getString("subject_name")))
            .forEach(logger::info);
    }

    @Transactional
    protected void createSubjectIfNotExists(String subjectName) {
        try {
            if (subjectRepository.findBySubjectNameIgnoreCase(subjectName).isEmpty()) {
                Subject subject = new Subject(subjectName);
                subjectRepository.save(subject);
                logger.info("Created subject: {}", subjectName);
            } else {
                logger.info("Subject already exists: {}", subjectName);
            }
        } catch (Exception e) {
            logger.error("Error creating subject {}: {}", subjectName, e.getMessage());
        }
    }
}
