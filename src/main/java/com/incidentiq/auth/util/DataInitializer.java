package com.incidentiq.auth.util;

import com.incidentiq.auth.dto.AuthResponse;
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
        createIfNotFound("admin", "Admin", "User", "admin123", "admin@zapcg.com", "ROLE_ADMIN");
        createIfNotFound("manager", "Manager", "User", "manager123", "manager@zapcg.com", "ROLE_MANAGER");
        createIfNotFound("user", "Regular", "User", "user123", "user@zapcg.com", "ROLE_USER");
        createIfNotFound("tech_backend", "John", "Backend", "tech123", "backend@zapcg.com", "ROLE_USER");
        createIfNotFound("tech_frontend", "Jane", "Frontend", "tech123", "frontend@zapcg.com", "ROLE_USER");
        createIfNotFound("tech_database", "Bob", "Database", "tech123", "database@zapcg.com", "ROLE_USER");
        createIfNotFound("tech_infra", "Alice", "Infra", "tech123", "infra@zapcg.com", "ROLE_USER");
        createIfNotFound("tech_network", "Charlie", "Network", "tech123", "network@zapcg.com", "ROLE_USER");
    }

    private void createIfNotFound(String username, String firstName, String lastName, String password, String email, String role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            log.info("Creating default user: {}", username);
            AuthResponse response = authService.register(RegisterRequest.builder()
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .password(password)
                    .email(email)
                    .role(role)
                    .build());
            authService.markProfileCompleted(response.getUserId());
        }
    }
}
