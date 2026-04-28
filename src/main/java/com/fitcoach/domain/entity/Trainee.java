package com.fitcoach.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitcoach.domain.enums.TraineeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "trainees")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trainee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    @JsonIgnore
    private Coach coach;

    @Column(length = 100)
    private String fitnessGoal;

    @Column(length = 30)
    private String traineeLevel;

    @Column(columnDefinition = "TEXT")
    private String coachFeedback;

    @Column(columnDefinition = "TEXT")
    private String cautionNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TraineeStatus status = TraineeStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
