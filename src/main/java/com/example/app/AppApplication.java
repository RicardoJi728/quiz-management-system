// Package declaration for the main application class
package com.example.app;

// Import required Spring Boot classes
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

// Main Spring Boot application annotation that enables auto-configuration
@SpringBootApplication
// Specifies the package where JPA entities are located
@EntityScan("com.example.app.model")
public class AppApplication {

    // Main method that serves as the entry point of the application
    public static void main(String[] args) {
        // Bootstrap the Spring Boot application
        SpringApplication.run(AppApplication.class, args);
    }
}
