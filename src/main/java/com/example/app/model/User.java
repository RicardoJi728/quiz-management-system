package com.example.app.model;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

// Entity class representing a user in the system
@Entity
@Table(name = "users")
// Ignore certain properties during JSON serialization
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "authorities", 
    "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
public class User implements UserDetails {
    
    // Primary key for the user
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username field, must be unique and not null
    @Column(nullable = false, unique = true)
    private String username;

    // Password field, must not be null and will be write-only in JSON
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Email field, must be unique and not null
    @Column(nullable = false, unique = true)
    private String email;

    // Transient field for handling raw password during registration/update
    @Transient
    private String rawPassword;

    // Standard getters and setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getter and setter for raw password with automatic password update
    public String getRawPassword() {
        return rawPassword;
    }

    public void setRawPassword(String rawPassword) {
        this.rawPassword = rawPassword;
        if (rawPassword != null) {
            this.password = rawPassword;
        }
    }

    // Implementation of UserDetails interface methods
    @Override
    public boolean isAccountNonExpired() {
        // Return true to indicate the account never expires
        // This is part of the UserDetails interface implementation
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Return true to indicate the account is never locked
        // Another required method from the UserDetails interface
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Return true to indicate credentials never expire
        // Required for the UserDetails interface implementation
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
}
