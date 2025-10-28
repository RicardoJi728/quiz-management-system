// Package declaration for payload classes
package com.example.app.payload;

// Response class for JWT authentication
// Contains the JWT token and user information
public class JwtResponse {
    // The JWT token string
    private String token;
    
    // The token type, always "Bearer"
    private String type = "Bearer";
    
    // The username of the authenticated user
    private String username;

    // Constructor with token and username
    public JwtResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    // Getter for the JWT token
    public String getToken() {
        return token;
    }

    // Getter for the token type
    public String getType() {
        return type;
    }

    // Getter for the username
    public String getUsername() {
        return username;
    }
} 