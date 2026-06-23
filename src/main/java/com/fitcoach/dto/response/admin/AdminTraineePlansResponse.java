package com.fitcoach.dto.response.admin;

import com.fitcoach.domain.entity.PlanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin view of every plan (workout + nutrition) assigned to a single trainee,
 * looked up by the trainee's email address — including full prescription detail
 * (sessions/exercises/sets/reps for workouts, meals/macros/ingredients for nutrition).
 */
@Data
@Builder
public class AdminTraineePlansResponse {

    private Long traineeId;
    private String fullName;
    private String email;

    private List<WorkoutPlanItem> workoutPlans;
    private List<NutritionPlanItem> nutritionPlans;

    // ─────────────────────────────── Workout ─────────────────────────────────

    @Data
    @Builder
    public static class WorkoutPlanItem {
        private UUID planId;
        private String title;
        private String description;
        private String coachName;
        private LocalDate startDate;
        private PlanStatus status;
        private LocalDateTime createdAt;
        /** Workout days in {@code dayOrder}. */
        private List<SessionItem> sessions;
    }

    @Data
    @Builder
    public static class SessionItem {
        private UUID sessionId;
        private String title;
        private Integer dayOrder;
        /** Prescribed exercises in {@code orderIndex}. */
        private List<ExerciseItem> exercises;
    }

    @Data
    @Builder
    public static class ExerciseItem {
        private UUID id;
        private int orderIndex;
        private String name;
        private String targetedMuscle;
        private String videoLink;
        private String sectionType;
        private int sets;
        private String reps;
        private BigDecimal loadAmount;
        private Integer restSeconds;
    }

    // ────────────────────────────── Nutrition ────────────────────────────────

    @Data
    @Builder
    public static class NutritionPlanItem {
        private Long planId;
        private String title;
        private String description;
        private String coachName;
        private Double waterTargetLiters;
        private LocalDateTime createdAt;
        private List<MealItem> meals;
    }

    @Data
    @Builder
    public static class MealItem {
        private Long id;
        private String name;
        private Double calories;
        private Double protein;
        private Double carbs;
        private Double fat;
        private List<IngredientItem> ingredients;
    }

    @Data
    @Builder
    public static class IngredientItem {
        private Long id;
        private String name;
        private Double servingQuantityG;
        private Double calories;
        private Double protein;
        private Double carbohydrates;
        private Double fat;
    }
}
