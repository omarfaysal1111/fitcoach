package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "coach_transformations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachTransformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    @Column(name = "client_label", nullable = false, length = 200)
    private String clientLabel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "stats_summary", length = 300)
    private String statsSummary;

    @Column(name = "before_photo_url")
    private String beforePhotoUrl;

    @Column(name = "after_photo_url")
    private String afterPhotoUrl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
