package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainee_water_intakes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trainee_id", "intake_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraineeWaterIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    /** Calendar day this total applies to (trainee-local date). */
    @Column(name = "intake_date", nullable = false)
    private LocalDate intakeDate;

    /** Total water recorded for that day, in liters. */
    @Column(nullable = false)
    private Double liters;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
