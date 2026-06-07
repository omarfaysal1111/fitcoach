package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TraineeProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String fitnessGoal;
    private String traineeLevel;
    private Long coachId;
    private String coachName;
    private LocalDateTime createdAt;
    private int workoutsCompletedToday;
    private int workoutsPlannedToday;
    private int mealsCompletedToday;
    private int mealsPlannedToday;
    private int workoutProgressPercent;
    private int nutritionProgressPercent;
    private int adherencePercent;
    private int missedWorkoutsCount;
    private int missedMealsCount;
    private String coachFeedback;
    private String cautionNotes;
    private String traineeNoteToCoach;
    private int currentStreak;

    private String avatarUrl;

    // onboarding fields
    private Double height;
    private Double weight;
    private LocalDate dateOfBirth;
    private String gender;
    private String healthHistory;
    private String medications;
    private boolean onboardingComplete;

    // extended onboarding fields
    private String dietaryPreferences;
    private String injuries;
    private String sleepHours;
    private String medicalConditions;
    private String additionalNotes;
}
