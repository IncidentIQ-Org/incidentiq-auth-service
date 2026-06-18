package com.incidentiq.auth.service.impl;

import com.incidentiq.auth.dto.AuthRequest;
import com.incidentiq.auth.dto.AuthResponse;
import com.incidentiq.auth.dto.RegisterRequest;
import com.incidentiq.auth.exception.UserNotFoundException;
import com.incidentiq.auth.model.User;
import com.incidentiq.auth.repository.UserRepository;
import com.incidentiq.auth.service.AuthService;
import com.incidentiq.auth.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://user-service:8081}")
    private String userServiceUrl;

    @Override
    public AuthResponse login(AuthRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException(request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getUsername());
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getRole(), user.isApproved());
        log.info("Login successful for user: {} (approved: {})", request.getUsername(), user.isApproved());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .approved(user.isApproved())
                .profileCompleted(user.isProfileCompleted())
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new RuntimeException("Username '" + request.getUsername() + "' is already taken");
        }

        String requestedRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "ROLE_USER";
        boolean isManager = "ROLE_MANAGER".equals(requestedRole);
        // Managers start unapproved; users and admins auto-approved
        boolean approved = !isManager;

        User user = User.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .approved(approved)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered successfully: {} (role: {}, approved: {})", request.getUsername(), requestedRole, approved);

        // Sync user to user-service
        try {
            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("id", saved.getId());
            userPayload.put("username", saved.getUsername());
            userPayload.put("firstName", saved.getFirstName());
            userPayload.put("lastName", saved.getLastName());
            userPayload.put("email", saved.getEmail());
            userPayload.put("role", saved.getRole());
            userPayload.put("profileCompleted", saved.isProfileCompleted());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(userPayload, headers);

            // Direct service-to-service call (bypasses the gateway), so use the controller's
            // real path "/sync" — user-service has no /api/users context-path.
            restTemplate.postForObject(userServiceUrl + "/sync", requestEntity, String.class);
            log.info("User synced to user-service: {}", saved.getUsername());
        } catch (Exception e) {
            log.error("Failed to sync user to user-service: {}", e.getMessage());
            // Don't fail registration if sync fails
        }

        // Notify admins about new user registration
        try {
            String notificationUrl = "http://api-gateway/api/notifications/user-registration";
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("newUserId", saved.getId());
            notificationPayload.put("username", saved.getUsername());
            notificationPayload.put("role", saved.getRole());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> notificationRequest = new HttpEntity<>(notificationPayload, headers);

            restTemplate.postForObject(notificationUrl, notificationRequest, String.class);
            log.info("Admins notified of new user registration: {}", saved.getUsername());
        } catch (Exception e) {
            log.error("Failed to notify admins of user registration: {}", e.getMessage());
            // Don't fail registration if notification fails
        }

        String token = jwtUtils.generateToken(saved.getUsername(), saved.getId(), saved.getRole(), saved.isApproved());
        return AuthResponse.builder()
                .token(token)
                .userId(saved.getId())
                .username(saved.getUsername())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .approved(saved.isApproved())
                .profileCompleted(saved.isProfileCompleted())
                .build();
    }

    @Override
    @Transactional
    public void markProfileCompleted(Long userId) {
        log.info("Marking profile completed for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.setProfileCompleted(true);
        userRepository.save(user);
        log.info("Profile marked as completed for userId: {}", userId);
    }
}
