package com.fitcoach.domain.entity;

import com.fitcoach.domain.enums.FeedbackCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A note the trainee sends to their coach, kept as history (one row per
 * submission) so the coach can see every message rather than just the latest.
 * Tagged with a {@link FeedbackCategory} so nutrition vs. workout feedback can
 * be distinguished on the coach's progress tab.
 */
@Entity
@Table(name = "trainee_feedback")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraineeFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    @Builder.Default
    private FeedbackCategory category = FeedbackCategory.GENERAL;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
