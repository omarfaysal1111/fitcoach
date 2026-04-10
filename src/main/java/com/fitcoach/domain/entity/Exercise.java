package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Master exercise dictionary (e.g. seeded from wger {@code /exerciseinfo/}). Plan prescriptions
 * reference this via {@link PlanSessionExercise}.
 */
@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String videoLink;

    private String targetedMuscle;
}
