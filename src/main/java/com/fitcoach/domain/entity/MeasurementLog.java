package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "measurement_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_id")
    private Trainee trainee;

    @Column(nullable = false)
    private LocalDate date;

    private Double weight;

    @Column(name = "body_fat_percentage")
    private Double bodyFatPercentage;

    @Column(name = "muscle_mass")
    private Double muscleMass;

    @Column(name = "water_percentage")
    private Double waterPercentage;

    private Double chest;
    private Double waist;
    private Double arms;
    private Double hips;
    private Double thighs;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
