package com.incidentiq.auth.util;

import com.incidentiq.auth.dto.RegisterRequest;
import com.incidentiq.auth.model.User;
import com.incidentiq.auth.repository.UserRepository;
import com.incidentiq.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public void run(String... args) throws Exception {
        createIfNotFound("admin", "admin123", "admin@zapcg.com", "ROLE_ADMIN");
        createIfNotFound("manager", "manager123", "manager@zapcg.com", "ROLE_MANAGER");
        createIfNotFound("user", "user123", "user@zapcg.com", "ROLE_USER");
    }

    private void createIfNotFound(String username, String password, String email, String role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            log.info("Creating default user: {}", username);
            authService.register(RegisterRequest.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .build());
        }
    }
}
