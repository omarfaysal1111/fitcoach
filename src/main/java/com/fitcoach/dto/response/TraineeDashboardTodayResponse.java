package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TraineeDashboardTodayResponse {

    private Profile profile;
    private CoachSummary coach;
    private Streak streak;
    private List<CoachGoal> coachGoals;
    private TodayWorkoutSummary todayWorkoutSummary;
    private TodayNutritionSummary todayNutritionSummary;
    private WeeklyGoals weeklyGoals;
    private List<Achievement> achievements;

    @Data
    @Builder
    public static class Profile {
        private Long id;
        private String fullName;
        private String fitnessGoal;
        private String avatarUrl;
    }

    @Data
    @Builder
    public static class CoachSummary {
        private Long id;
        private String fullName;
    }

    @Data
    @Builder
    public static class Streak {
        private int currentDays;
        private int nextBadgeInDays;
        private String nextBadgeName;
    }

    @Data
    @Builder
    public static class CoachGoal {
        private Long id;
        private String label;
        private boolean completed;
    }

    @Data
    @Builder
    public static class TodayWorkoutSummary {
        private Long planId;
        private String title;
        private String difficulty;
        private int exercisesTotal;
        private int exercisesDone;
        private int durationMinutes;
        private int estimatedCalories;
    }

    @Data
    @Builder
    public static class TodayNutritionSummary {
        private Long planId;
        private String title;
        private int caloriesConsumed;
        private int caloriesTarget;
        private int proteinGrams;
        private int proteinTarget;
        private int carbsGrams;
        private int carbsTarget;
        private int fatGrams;
        private int fatTarget;
    }

    @Data
    @Builder
    public static class WeeklyGoals {
        private int workoutsCompleted;
        private int workoutsTarget;
        private int mealsLogged;
        private int mealsTarget;
        private double waterLiters;
        private double waterTargetLiters;
        private double weightStart;
        private double weightCurrent;
        private double weightTarget;
    }

    @Data
    @Builder
    public static class Achievement {
        private String code;
        private String label;
        private String level;
        private boolean unlocked;
    }
}

