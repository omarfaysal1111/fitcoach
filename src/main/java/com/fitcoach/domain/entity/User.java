package com.fitcoach.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitcoach.domain.enums.AuthProvider;
import com.fitcoach.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false, length = 80)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * How this account was created / authenticates.
     * LOCAL  = classic password account.
     * GOOGLE = created via Google Sign-In.
     * APPLE  = created via Sign in with Apple.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Stable user ID from the identity provider (Google {@code sub} / Apple {@code sub}).
     * NULL for LOCAL accounts.
     */
    @Column(length = 255)
    private String providerSubject;

    /**
     * Epoch seconds of the {@code iat} claim for the latest issued JWT. Tokens with an earlier
     * {@code iat} are rejected after a new login or registration issues a replacement token.
     */
    private Long jwtIssuedEpochSec;

    /** Firebase Cloud Messaging device token — used to send push notifications. */
    @Column(length = 512)
    private String fcmToken;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ── Convenience helpers ──────────────────────────────────────────────────
    public boolean isCoach() {
        return Role.COACH.equals(this.role);
    }

    public boolean isTrainee() {
        return Role.TRAINEE.equals(this.role);
    }
}
