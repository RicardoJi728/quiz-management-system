package com.example.app.config;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    
    @Bean
    public void cleanupDatabase(TestEntityManager entityManager) {
        entityManager.getEntityManager().createQuery("DELETE FROM Question").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();
    }
} 