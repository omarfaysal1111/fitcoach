package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed meal-completion record returned to the coach for a single trainee.
 * Mirrors the structure of {@link WorkoutLogResponse} so the mobile app can
 * render both histories consistently.
 */
@Data
@Builder
public class MealCompletionLogResponse {

    private Long completionId;
    private Long mealId;
    private String mealName;
    /** Null when the meal's parent nutrition plan cannot be determined. */
    private Long nutritionPlanId;
    private String nutritionPlanTitle;
    /** ISO date string (yyyy-MM-dd). */
    private String completionDate;
    private LocalDateTime completedAt;
    /** True when the trainee pressed "skip meal" instead of eating it. */
    private boolean skipped;
    /** True when at least one ingredient was skipped or swapped. */
    private boolean hasDeviations;
    /** Per-ingredient deviation detail; empty when the meal was eaten as-is or fully skipped. */
    private List<IngredientDeviationItem> ingredientDeviations;

    @Data
    @Builder
    public static class IngredientDeviationItem {
        private Long originalIngredientId;
        private String originalIngredientName;
        /** Null when the ingredient was skipped rather than swapped. */
        private Long replacementIngredientId;
        private String replacementIngredientName;
        /** Quantity of the replacement ingredient used; null when skipped. */
        private Double newQuantity;
        /** "SKIPPED" or "SWAPPED". */
        private String type;
    }
}
