// Package declaration for configuration classes
package com.example.app.config;

// Import required Spring MVC configuration classes
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Configuration class for web-related settings including CORS
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    // Override method to configure CORS (Cross-Origin Resource Sharing) settings
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Configure CORS for all paths
        registry.addMapping("/**")
                // Allow requests from localhost:8080
                .allowedOrigins("http://localhost:8080")
                // Allow specific HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Allow all headers
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                .allowCredentials(true);
    }

    // Override method to configure view controllers for URL mapping
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map root URL to index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        // Map /register URL to register.html
        registry.addViewController("/register").setViewName("forward:/register.html");
    }
} 