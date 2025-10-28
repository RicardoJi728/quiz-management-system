package com.example.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Configuration class for Spring Security settings
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // JWT secret key from application properties
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    // JWT authentication filter for processing JWT tokens
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Constructor with dependency injection
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Configure security filter chain with authentication and authorization rules
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            // Configure URL authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow public access to static resources and auth endpoints
                .requestMatchers("/", "/index.html", "/register.html", "/dashboard.html", "/questionbank.html", "/topic.html",
                               "/quiz.html", "/take-quiz.html", "/quiz-results.html",
                               "/style.css", "/script.js", "/dashboard.js", "/questionbank.js", "/topic.js",
                               "/quiz.js", "/take-quiz.js", "/quiz-results.js",
                               "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Require authentication for API endpoints
                .requestMatchers("/api/quizzes/**", "/api/questions/**", "/api/questionbanks/**").authenticated()
                .anyRequest().authenticated()
            )
            // Configure stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Add JWT filter before username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    // Bean for password encryption
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean for authentication manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
