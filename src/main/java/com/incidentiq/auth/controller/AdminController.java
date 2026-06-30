package com.incidentiq.auth.controller;

import com.incidentiq.auth.dto.UserApprovalResponse;
import com.incidentiq.auth.model.User;
import com.incidentiq.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://user-service:8081}")
    private String userServiceUrl;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending users", description = "Get all users awaiting approval (unapproved managers)")
    public ResponseEntity<List<UserApprovalResponse>> getPendingUsers() {
        List<User> pendingUsers = userRepository.findByApprovedFalse();
        List<UserApprovalResponse> response = pendingUsers.stream()
                .map(this::toApprovalResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve user", description = "Approve a pending user account")
    public ResponseEntity<UserApprovalResponse> approveUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        user.setApproved(true);
        User saved = userRepository.save(user);

        // Sync the approved role to user-service so role-based queries (e.g. notifyManagers) work
        syncRoleToUserService(saved.getId(), saved.getRole(), saved.getUsername(),
                saved.getFirstName(), saved.getLastName(), saved.getEmail());

        return ResponseEntity.ok(toApprovalResponse(saved));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject user", description = "Reject and delete a pending user account")
    public ResponseEntity<Void> rejectUser(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        userRepository.delete(user);
        deleteFromUserService(id, authHeader);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Get all users in the system")
    public ResponseEntity<List<UserApprovalResponse>> getAllUsers() {
        List<User> allUsers = userRepository.findAll();
        List<UserApprovalResponse> response = allUsers.stream()
                .map(this::toApprovalResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Permanently delete a user account from all databases")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Delete from auth-service database
        userRepository.deleteById(id);
        log.info("Deleted user {} from auth-service database", id);

        // Also delete from user-service database — forward admin JWT so user-service accepts the call
        deleteFromUserService(id, authHeader);

        return ResponseEntity.noContent().build();
    }

    /**
     * Syncs an approved manager's role to user-service so notification queries work correctly.
     */
    private void syncRoleToUserService(Long userId, String role, String username,
                                        String firstName, String lastName, String email) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", userId);
            payload.put("username", username);
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            payload.put("email", email);
            payload.put("role", role);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.exchange(userServiceUrl + "/" + userId + "/role", HttpMethod.PUT, request, Void.class);
            log.info("Synced role {} to user-service for user {}", role, userId);
        } catch (Exception e) {
            log.error("Failed to sync role to user-service for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Cascade delete to user-service so the user is fully removed from both databases.
     * Forwards the admin's JWT so user-service accepts the authenticated call.
     */
    private void deleteFromUserService(Long userId, String authHeader) {
        try {
            String url = userServiceUrl + "/" + userId;
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            log.info("Deleted user {} from user-service database", userId);
        } catch (Exception e) {
            log.error("Failed to delete user {} from user-service: {}", userId, e.getMessage());
        }
    }

    private UserApprovalResponse toApprovalResponse(User user) {
        return UserApprovalResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .approved(user.isApproved())
                .build();
    }
}
