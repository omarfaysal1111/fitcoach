package com.fitcoach.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One day/session inside a coach-authored {@link WorkoutPlan} (e.g. "Leg Day").
 * Not to be confused with catalog {@link Exercise} rows from the external API, nor a future
 * third-party "workout" template entity.
 */
@Entity
@Table(name = "plan_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    @JsonIgnore
    private WorkoutPlan plan;

    @Column(nullable = false)
    private String title;

    @Column(name = "day_order", nullable = false)
    private Integer dayOrder;

    @OneToMany(mappedBy = "planSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlanSessionExercise> sessionExercises = new ArrayList<>();
}
