package com.example.app.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filter for processing JWT authentication
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Logger for debugging and error tracking
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Dependencies for JWT operations and user details
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    // Constructor with dependency injection
    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    // Extract JWT token from Authorization header
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Process each request to validate JWT token and set authentication
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            // Extract JWT token from the HTTP request
            String jwt = getJwtFromRequest(request);
            logger.debug("JWT Token: {}", jwt);

            if (jwt != null) {
                // Extract username from the JWT token
                String username = jwtUtils.extractUsername(jwt);
                logger.debug("Username from token: {}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Load user details from the database
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtUtils.isTokenValid(jwt, userDetails)) {
                        // Create authentication object if token is valid
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        // Add request details to the authentication
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Set authentication in the Spring Security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            // Log any errors during the authentication process
            logger.error("Cannot set user authentication", e);
        }

        chain.doFilter(request, response);
    }
} 