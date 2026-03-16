package com.fitcoach.controller;

import com.fitcoach.domain.entity.ExercisePlan;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.TraineeDashboardTodayResponse;
import com.fitcoach.dto.response.TraineeExercisePlanDetailResponse;
import com.fitcoach.dto.response.TraineePlanSummaryResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.service.ExercisePlanService;
import com.fitcoach.service.NutritionPlanService;
import com.fitcoach.service.TraineeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trainees")
@RequiredArgsConstructor
public class TraineeController {

    private final TraineeService traineeService;
    private final NutritionPlanService nutritionPlanService;
    private final ExercisePlanService exercisePlanService;

    /** GET /api/trainees/me – authenticated trainee's own profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(traineeService.getMyProfile(principal.getUsername())));
    }

    /** GET /api/trainees/me/dashboard-today – aggregated view for trainee home */
    @GetMapping("/me/dashboard-today")
    public ResponseEntity<ApiResponse<TraineeDashboardTodayResponse>> getMyTodayDashboard(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(traineeService.getTodayDashboard(principal.getUsername())));
    }

    /** POST /api/trainees/me/workouts/{workoutId}/complete – mark a workout as completed today */
    @PostMapping("/me/workouts/{workoutId}/complete")
    public ResponseEntity<ApiResponse<String>> completeWorkout(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long workoutId) {
        traineeService.completeWorkout(principal.getUsername(), workoutId);
        return ResponseEntity.ok(ApiResponse.ok("Workout completed"));
    }

    /** POST /api/trainees/me/meals/{mealId}/complete – mark a meal as completed/logged today */
    @PostMapping("/me/meals/{mealId}/complete")
    public ResponseEntity<ApiResponse<String>> completeMeal(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long mealId) {
        traineeService.completeMeal(principal.getUsername(), mealId);
        return ResponseEntity.ok(ApiResponse.ok("Meal completed"));
    }

    /** PUT /api/trainees/me – update profile */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateTraineeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated", traineeService.updateMyProfile(principal.getUsername(), request)));
    }

    /** GET /api/trainees/coach – view the assigned coach's profile */
    @GetMapping("/coach")
    public ResponseEntity<ApiResponse<CoachProfileResponse>> getMyCoach(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(traineeService.getMyCoach(principal.getUsername())));
    }
    
    /** GET /api/trainees/me/nutrition-plans – get my assigned nutrition plans (summaries only) */
    @GetMapping("/me/nutrition-plans")
    public ResponseEntity<ApiResponse<List<TraineePlanSummaryResponse>>> getMyNutritionPlans(
            @AuthenticationPrincipal UserDetails principal) {
        Long traineeId = traineeService.getTraineeByEmail(principal.getUsername()).getId();
        List<NutritionPlan> plans = nutritionPlanService.getPlansByTrainee(traineeId);

        // Pick the latest plan by createdAt (if any), null-safe
        List<TraineePlanSummaryResponse> summaries = plans.stream()
                .max(java.util.Comparator.comparing(
                        NutritionPlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(p -> List.of(TraineePlanSummaryResponse.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .type("NUTRITION")
                        .build()))
                .orElseGet(List::of);

        return ResponseEntity.ok(
                ApiResponse.ok("Nutrition plans retrieved successfully", summaries));
    }

    /** GET /api/trainees/me/exercise-plans – get my assigned exercise plans (summaries only) */
    @GetMapping("/me/exercise-plans")
    public ResponseEntity<ApiResponse<List<TraineePlanSummaryResponse>>> getMyExercisePlans(
            @AuthenticationPrincipal UserDetails principal) {
        Long traineeId = traineeService.getTraineeByEmail(principal.getUsername()).getId();
        List<ExercisePlan> plans = exercisePlanService.getPlansByTrainee(traineeId);

        // Pick the latest plan by createdAt (if any), null-safe
        List<TraineePlanSummaryResponse> summaries = plans.stream()
                .max(java.util.Comparator.comparing(
                        ExercisePlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(p -> List.of(TraineePlanSummaryResponse.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .type("EXERCISE")
                        .build()))
                .orElseGet(List::of);

        return ResponseEntity.ok(
                ApiResponse.ok("Exercise plans retrieved successfully", summaries));
    }

    /** GET /api/trainees/me/exercise-plans/{planId} – detailed view of a specific plan for the trainee */
    @GetMapping("/me/exercise-plans/{planId}")
    public ResponseEntity<ApiResponse<TraineeExercisePlanDetailResponse>> getMyExercisePlanDetail(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long planId) {
        TraineeExercisePlanDetailResponse detail =
                traineeService.getExercisePlanDetail(principal.getUsername(), planId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }
}
