package com.incidentiq.auth.service;

import com.incidentiq.auth.dto.AuthRequest;
import com.incidentiq.auth.dto.AuthResponse;
import com.incidentiq.auth.dto.RegisterRequest;
import com.incidentiq.auth.model.User;
import com.incidentiq.auth.repository.UserRepository;
import com.incidentiq.auth.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public User register(RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : "ROLE_USER")
                .build();
        
        return userRepository.save(user);
    }
}
