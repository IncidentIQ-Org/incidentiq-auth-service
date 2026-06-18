package com.incidentiq.auth.controller;

import com.incidentiq.auth.dto.AuthRequest;
import com.incidentiq.auth.dto.AuthResponse;
import com.incidentiq.auth.dto.RegisterRequest;
import com.incidentiq.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PatchMapping("/users/{id}/profile-completed")
    public ResponseEntity<Void> markProfileCompleted(@PathVariable Long id) {
        authService.markProfileCompleted(id);
        return ResponseEntity.ok().build();
    }
}
