package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainee_workout_completions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"trainee_id", "plan_session_id", "completion_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraineeWorkoutCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_id")
    private Trainee trainee;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_session_id")
    private PlanSession planSession;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(nullable = false)
    private LocalDateTime completedAt;
}
