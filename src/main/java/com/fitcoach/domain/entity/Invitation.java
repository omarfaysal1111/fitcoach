package com.fitcoach.domain.entity;

import com.fitcoach.domain.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    /**
     * The UUID IS the invitation token – coaches share this with the trainee.
     * Stored as a native UUID column in PostgreSQL for efficiency.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    /** Email address the invitation is intended for. */
    @Column(nullable = false, length = 150)
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── Domain helpers ───────────────────────────────────────────────────────
    public boolean isValid() {
        return InvitationStatus.PENDING.equals(this.status)
                && LocalDateTime.now().isBefore(this.expiresAt);
    }
}
