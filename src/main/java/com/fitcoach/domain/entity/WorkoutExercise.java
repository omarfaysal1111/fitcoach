package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_exercise_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private Workout workout;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Column(name = "order_index")
    private int orderIndex;

    private int sets;

    private String reps;

    private String load;

    private String rest;
}

