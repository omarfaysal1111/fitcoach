package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trainee_exercise_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraineeExerciseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_completion_id")
    private TraineeWorkoutCompletion workoutCompletion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_session_exercise_id")
    private PlanSessionExercise planSessionExercise;

    @Column(name = "actual_sets_completed", nullable = false)
    private int actualSetsCompleted;

    @Column(name = "skipped_sets", nullable = false)
    private int skippedSets;

    /** Sets attempted but not finished (fatigue, failure, etc.); see per-set rows in {@link #setLogs}. */
    @Column(name = "missed_sets", nullable = false)
    @Builder.Default
    private int missedSets = 0;

    /** Mandatory when skippedSets > 0 */
    @Column(length = 1000)
    private String excuse;

    /** Coach's response / feedback note */
    @Column(name = "coach_notes", length = 1000)
    private String coachNotes;

    @Column(name = "is_reviewed_by_coach", nullable = false)
    @Builder.Default
    private Boolean isReviewedByCoach = false;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @OneToMany(mappedBy = "exerciseLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TraineeExerciseSetLog> setLogs = new ArrayList<>();
}
