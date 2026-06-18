package com.incidentiq.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token with username, userId, and role claims.
     */
    public String generateToken(String username, Long userId, String role) {
        return generateToken(username, userId, role, true);
    }

    /**
     * Generates a JWT token where the effective role (used for backend authorization)
     * is downgraded to ROLE_USER if a manager account is not yet approved.
     * The actual requested role and the approval flag are also embedded so the
     * frontend can display the correct state.
     */
    public String generateToken(String username, Long userId, String role, boolean approved) {
        String normalized = role == null ? "ROLE_USER" : role.trim().toUpperCase();
        boolean isManager = normalized.equals("ROLE_MANAGER");
        // Pending managers operate with USER-level privileges until approved.
        String effectiveRole = (isManager && !approved) ? "ROLE_USER" : normalized;

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", effectiveRole);
        claims.put("actualRole", normalized);
        claims.put("approved", approved);
        claims.put("userId", userId);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
}
