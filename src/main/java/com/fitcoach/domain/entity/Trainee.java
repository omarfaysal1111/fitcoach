package com.fitcoach.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitcoach.domain.enums.TraineeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainees")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trainee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    @JsonIgnore
    private Coach coach;

    @Column(length = 100)
    private String fitnessGoal;

    @Column(length = 30)
    private String traineeLevel;

    @Column(columnDefinition = "TEXT")
    private String coachFeedback;

    @Column(columnDefinition = "TEXT")
    private String cautionNotes;

    /** Latest note submitted by the trainee to their coach. */
    @Column(columnDefinition = "TEXT", name = "trainee_note_to_coach")
    private String traineeNoteToCoach;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TraineeStatus status = TraineeStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    // ── Onboarding profile fields ─────────────────────────────────────────────

    /** Height in centimetres (e.g. 175). */
    private Double height;

    /** Weight in kilograms at the time of onboarding. */
    private Double weight;

    /** Date of birth. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** "male" | "female" | "other" */
    @Column(length = 20)
    private String gender;

    /** Free-text medical / health history (injuries, chronic conditions, etc.) */
    @Column(name = "health_history", columnDefinition = "TEXT")
    private String healthHistory;

    /** Comma-separated list of current medications, or free text. */
    @Column(columnDefinition = "TEXT")
    private String medications;

    // ── Extended onboarding fields ────────────────────────────────────────────

    /** e.g. "no_restrictions" | "halal" | "vegetarian" | "keto" */
    @Column(name = "dietary_preferences", length = 50)
    private String dietaryPreferences;

    /** Injuries described by the trainee. */
    @Column(name = "injuries", columnDefinition = "TEXT")
    private String injuries;

    /** e.g. "under_5h" | "5_6h" | "7_8h" | "9plus" */
    @Column(name = "sleep_hours", length = 20)
    private String sleepHours;

    /** Medical conditions (diabetes, hypertension, etc.) */
    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    /** Additional notes for the coach from the trainee. */
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    /** True once the trainee completes the onboarding form in-app. */
    @Column(name = "onboarding_complete", nullable = false)
    @Builder.Default
    private boolean onboardingComplete = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
