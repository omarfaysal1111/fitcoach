package com.fitcoach.domain.entity;

import com.fitcoach.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Persisted in-app notification, backing the notification inbox (SCRUM-13/84).
 * A row is created at the same time a push is sent so the inbox survives app
 * restarts and the unread badge reflects real state.
 */
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationType type = NotificationType.GENERAL;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
