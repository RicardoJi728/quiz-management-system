// Package declaration for payload classes
package com.example.app.payload;

// Request class for user login
// Contains the credentials needed for authentication
public class LoginRequest {
    // The username for login
    private String username;
    
    // The password for login
    private String password;

    // Getter for the username
    public String getUsername() {
        return username;
    }

    // Setter for the username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter for the password
    public String getPassword() {
        return password;
    }

    // Setter for the password
    public void setPassword(String password) {
        this.password = password;
    }
} 