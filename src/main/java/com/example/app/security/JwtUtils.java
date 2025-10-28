package com.example.app.security;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// Utility class for JWT token operations
@Component
public class JwtUtils {
    // Logger for debugging and error tracking
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // JWT secret key from application properties
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    // JWT token expiration time from application properties
    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // Get the signing key for JWT operations
    private Key getSigningKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
            if (keyBytes.length * 8 < 512) {
                throw new IllegalStateException("JWT secret key must be at least 512 bits");
            }
            return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid JWT secret key", e);
        }
    }

    // Extract username from JWT token
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }

    // Validate JWT token against user details
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            // Extract username from the token
            final String username = extractUsername(token);
            
            // Check if username matches and token is not expired
            return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            // Log any errors during validation
            logger.error("Error validating token", e);
            return false;
        }
    }

    // Check if token has expired
    private boolean isTokenExpired(String token) {
        try {
            // Extract expiration date from token and check if it's before current time
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            // Log any errors during expiration check
            logger.error("Error checking token expiration", e);
            return true;  // Assume token is expired if there's an error
        }
    }

    // Extract expiration date from token
    private Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    // Generate new JWT token for username
    public String generateToken(String username) {
        try {
            if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
                throw new IllegalStateException("JWT secret is not configured");
            }
            
            Date now = new Date();
            Date expiry = new Date(now.getTime() + jwtExpirationMs);
            
            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiry)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            logger.error("Error generating token: {}", e.getMessage());
            throw new RuntimeException("Could not generate token: " + e.getMessage());
        }
    }
} 