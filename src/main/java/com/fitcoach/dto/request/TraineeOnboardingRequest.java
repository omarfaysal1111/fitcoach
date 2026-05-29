package com.fitcoach.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TraineeOnboardingRequest {

    // ── Personal info ─────────────────────────────────────────────────────────

    @Size(max = 80)
    private String fullName;

    @Past
    private LocalDate dateOfBirth;

    /** "male" | "female" | "other" */
    @Size(max = 20)
    private String gender;

    /** Height in centimetres. */
    @DecimalMin("50.0")
    @DecimalMax("300.0")
    private Double height;

    /** Weight in kilograms. */
    @DecimalMin("20.0")
    @DecimalMax("500.0")
    private Double weight;

    // ── Fitness profile ───────────────────────────────────────────────────────

    /** e.g. "lose_weight" | "build_muscle" | "improve_endurance" | "general_fitness" */
    @Size(max = 100)
    private String fitnessGoal;

    /** e.g. "beginner" | "intermediate" | "advanced" */
    @Size(max = 30)
    private String traineeLevel;

    // ── Health history ────────────────────────────────────────────────────────

    /** Past injuries, chronic conditions, surgeries, etc. */
    private String healthHistory;

    /** Current medications or "none". */
    private String medications;
}
