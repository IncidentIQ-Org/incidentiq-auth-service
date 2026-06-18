package com.incidentiq.auth.exception;

/**
 * Exception thrown when a user is not found during authentication.
 * This allows specific handling of user lookup failures while
 * maintaining security by not revealing whether username exists.
 */
public class UserNotFoundException extends RuntimeException {
    
    private final String attemptedUsername;
    
    public UserNotFoundException(String username) {
        super("User not found: " + username);
        this.attemptedUsername = username;
    }
    
    public String getAttemptedUsername() {
        return attemptedUsername;
    }
}
