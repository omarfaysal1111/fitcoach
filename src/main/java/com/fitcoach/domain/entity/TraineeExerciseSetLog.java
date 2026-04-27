package com.fitcoach.domain.entity;

import com.fitcoach.domain.enums.SetLogOutcome;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trainee_exercise_set_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"trainee_exercise_log_id", "set_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraineeExerciseSetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_exercise_log_id", nullable = false)
    private TraineeExerciseLog exerciseLog;

    /** 1-based index within the prescribed exercise (matches plan order). */
    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private SetLogOutcome outcome;

    /** Required for {@link SetLogOutcome#SKIPPED} and {@link SetLogOutcome#MISSED}; optional for COMPLETED. */
    @Column(length = 500)
    private String reason;

    /** Weight lifted in kilograms for this set. Null means bodyweight or not recorded. */
    @Column(name = "weight_kg")
    private Double weightKg;

    /** Actual reps performed for this set. Null when not recorded. */
    @Column(name = "reps")
    private Integer reps;
}
