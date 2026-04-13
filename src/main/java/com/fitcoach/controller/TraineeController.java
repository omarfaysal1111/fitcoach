package com.fitcoach.controller;

import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.WorkoutPlan;
import com.fitcoach.dto.request.MealCompletionRequest;
import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.IngredientResponse;
import com.fitcoach.dto.response.MealDetailedResponse;
import com.fitcoach.dto.response.NutritionPlanDetailedResponse;
import com.fitcoach.dto.response.TraineeDashboardTodayResponse;
import com.fitcoach.dto.response.TraineeExercisePlanDetailResponse;
import com.fitcoach.dto.response.TraineePlanSummaryResponse;
import com.fitcoach.dto.response.TraineeWorkoutPlanSessionsResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    /** POST /api/trainees/me/plan-sessions/{planSessionId}/complete – mark a plan session (plan day) completed today */
    @PostMapping("/me/plan-sessions/{planSessionId}/complete")
    public ResponseEntity<ApiResponse<String>> completePlanSession(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID planSessionId) {
        traineeService.completePlanSession(principal.getUsername(), planSessionId);
        return ResponseEntity.ok(ApiResponse.ok("Plan session completed"));
    }

    /** POST /api/trainees/me/meals/{mealId}/complete – mark a meal as completed/logged today */
/** POST /api/trainees/me/meals/{mealId}/complete – mark a meal as completed, skipped, or modified today */
    @PostMapping("/me/meals/{mealId}/complete")
    public ResponseEntity<ApiResponse<String>> completeMeal(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long mealId,
            @RequestBody(required = false) MealCompletionRequest request) {
        
        // If the request is null (no body sent), initialize a default one (perfect completion)
        if (request == null) {
            request = new MealCompletionRequest();
        }

        // Pass the request down to the service layer to handle the business logic
        traineeService.completeMeal(principal.getUsername(), mealId, request);
        
        String message = request.isSkipMeal() ? "Meal skipped successfully" : "Meal logged successfully";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }    /** PUT /api/trainees/me – update profile */
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
@Transactional(readOnly = true) // Ensures lazy-loaded meals and ingredients can be accessed within the request
public ResponseEntity<ApiResponse<List<NutritionPlanDetailedResponse>>> getMyNutritionPlans(
        @AuthenticationPrincipal UserDetails principal) {
    Long traineeId = traineeService.getTraineeByEmail(principal.getUsername()).getId();
    List<NutritionPlan> plans = nutritionPlanService.getPlansByTrainee(traineeId);

    // Pick the latest plan by createdAt (if any), null-safe
    List<NutritionPlanDetailedResponse> detailedPlans = plans.stream()
            .max(java.util.Comparator.comparing(
                    NutritionPlan::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .map(p -> List.of(NutritionPlanDetailedResponse.builder()
                    .id(String.valueOf(p.getId()))
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .type("NUTRITION")
                    .meals(p.getMeals().stream()
                            .map(meal -> MealDetailedResponse.builder()
                                    .id(String.valueOf(meal.getId()))
                                    .name(meal.getName())
                                    .calories(meal.getCalories())
                                    .protein(meal.getProtein())
                                    .carbs(meal.getCarbs())
                                    .fat(meal.getFat())
                                    .ingredients(meal.getIngredients().stream()
                                            .map(ingredient -> IngredientResponse.builder()
                                                    .id(String.valueOf(ingredient.getId()))
                                                    // Assuming your Ingredient entity has a 'getName()' method
                                                    .name(ingredient.getName()) 
                                                    .build())
                                            .toList())
                                    .build())
                            .toList())
                    .build()))
            .orElseGet(List::of);

    return ResponseEntity.ok(
            ApiResponse.ok("Nutrition plan retrieved successfully", detailedPlans));
}
    /** GET /api/trainees/me/exercise-plans – get my assigned exercise plans (summaries only) */
    @GetMapping("/me/exercise-plans")
    public ResponseEntity<ApiResponse<List<TraineePlanSummaryResponse>>> getMyExercisePlans(
            @AuthenticationPrincipal UserDetails principal) {
        Long traineeId = traineeService.getTraineeByEmail(principal.getUsername()).getId();
        List<WorkoutPlan> plans = exercisePlanService.getPlansByTrainee(traineeId);

        List<TraineePlanSummaryResponse> summaries = plans.stream()
                .max(java.util.Comparator.comparing(
                        WorkoutPlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(p -> List.of(TraineePlanSummaryResponse.builder()
                        .id(p.getId().toString())
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
            @PathVariable UUID planId) {
        TraineeExercisePlanDetailResponse detail =
                traineeService.getExercisePlanDetail(principal.getUsername(), planId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /** GET /api/trainees/me/exercise-plans/{planId}/sessions – plan sessions only (no exercise lines) */
    @GetMapping("/me/exercise-plans/{planId}/sessions")
    public ResponseEntity<ApiResponse<TraineeWorkoutPlanSessionsResponse>> getMyWorkoutPlanSessions(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID planId) {
        TraineeWorkoutPlanSessionsResponse body =
                traineeService.getWorkoutPlanSessions(principal.getUsername(), planId);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
