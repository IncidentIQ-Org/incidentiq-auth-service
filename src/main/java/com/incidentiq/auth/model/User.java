package com.incidentiq.auth.model;

import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN, ROLE_MANAGER, ROLE_USER

    /**
     * Whether the account is approved. Regular users and admins are approved
     * automatically. Managers start as unapproved (false) and must be approved
     * by an admin before they gain manager-level privileges.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean approved = true;

    /**
     * Whether the user has completed their professional profile.
     * New users start with this as false and must complete their profile
     * before accessing full system features.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean profileCompleted = false;
}
