package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single prescribed exercise slot within a {@link PlanSession} (sets, reps, load, rest).
 * References the master {@link Exercise} catalog (e.g. wger-seeded movements).
 */
@Entity
@Table(name = "plan_session_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanSessionExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_session_id", nullable = false)
    private PlanSession planSession;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 20)
    private SectionType sectionType;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private String reps;

    @Column(name = "load_amount", precision = 10, scale = 2)
    private BigDecimal loadAmount;

    @Column(name = "rest_seconds")
    private Integer restSeconds;
}
