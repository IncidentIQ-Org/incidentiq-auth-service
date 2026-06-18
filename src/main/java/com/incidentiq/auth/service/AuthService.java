package com.incidentiq.auth.service;

import com.incidentiq.auth.dto.AuthRequest;
import com.incidentiq.auth.dto.AuthResponse;
import com.incidentiq.auth.dto.RegisterRequest;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login credentials
     * @return the authentication response with token
     * @throws com.incidentiq.auth.exception.UserNotFoundException if user not found
     * @throws org.springframework.security.authentication.BadCredentialsException if password invalid
     */
    AuthResponse login(AuthRequest request);

    /**
     * Registers a new user and returns a JWT token.
     *
     * @param request the registration details
     * @return the authentication response with token
     * @throws RuntimeException if username already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Marks a user's profile as completed in the auth-service database.
     *
     * @param userId the ID of the user
     */
    void markProfileCompleted(Long userId);
}
