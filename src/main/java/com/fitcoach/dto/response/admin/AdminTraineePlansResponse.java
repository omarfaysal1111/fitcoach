package com.fitcoach.dto.response.admin;

import com.fitcoach.domain.entity.PlanStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin view of every plan (workout + nutrition) assigned to a single trainee,
 * looked up by the trainee's email address.
 */
@Data
@Builder
public class AdminTraineePlansResponse {

    private Long traineeId;
    private String fullName;
    private String email;

    private List<WorkoutPlanItem> workoutPlans;
    private List<NutritionPlanItem> nutritionPlans;

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
    }

    @Data
    @Builder
    public static class NutritionPlanItem {
        private Long planId;
        private String title;
        private String description;
        private String coachName;
        private Double waterTargetLiters;
        private LocalDateTime createdAt;
    }
}
