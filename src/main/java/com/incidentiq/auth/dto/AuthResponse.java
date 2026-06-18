package com.incidentiq.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean approved;
    private boolean profileCompleted;
}
