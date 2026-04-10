package com.fitcoach.controller;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.User;
import com.fitcoach.dto.request.plan.AddWorkoutDayRequest;
import com.fitcoach.dto.request.plan.AssignPlanToTraineeRequest;
import com.fitcoach.dto.request.plan.CreatePlanRequest;
import com.fitcoach.dto.request.plan.WorkoutExerciseLineRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.plan.PlanAssignmentResponse;
import com.fitcoach.dto.response.plan.WorkoutDayResponse;
import com.fitcoach.dto.response.plan.WorkoutExerciseLineResponse;
import com.fitcoach.dto.response.plan.WorkoutPlanResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.WorkoutPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API from architecture refactor (Steps 4–5). With {@code server.servlet.context-path=/api},
 * full URLs are {@code /api/v1/plans}, {@code /api/v1/workouts/...}.
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createPlan(
            @Valid @RequestBody CreatePlanRequest request,
            Authentication authentication) {
        Coach coach = resolveCoach(authentication);
        WorkoutPlanResponse body = workoutPlanService.createPlan(request, coach.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plan created", body));
    }

    @PostMapping("/plans/{planId}/workouts")
    public ResponseEntity<ApiResponse<WorkoutDayResponse>> addWorkoutDay(
            @PathVariable UUID planId,
            @Valid @RequestBody AddWorkoutDayRequest request,
            Authentication authentication) {
        Coach coach = resolveCoach(authentication);
        WorkoutDayResponse body = workoutPlanService.addWorkoutDay(planId, coach.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workout day added", body));
    }

    /**
     * Request body is a JSON array of exercise lines, per architecture doc.
     */
    @PostMapping("/workouts/{workoutId}/exercises")
    public ResponseEntity<ApiResponse<List<WorkoutExerciseLineResponse>>> replaceWorkoutExercises(
            @PathVariable UUID workoutId,
            @RequestBody List<@Valid WorkoutExerciseLineRequest> exercises,
            Authentication authentication) {
        Coach coach = resolveCoach(authentication);
        List<WorkoutExerciseLineResponse> body =
                workoutPlanService.replaceWorkoutExercises(workoutId, coach.getId(), exercises);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workout exercises saved", body));
    }

    @PostMapping("/plans/{planId}/assign")
    public ResponseEntity<ApiResponse<PlanAssignmentResponse>> assignPlan(
            @PathVariable UUID planId,
            @Valid @RequestBody AssignPlanToTraineeRequest request,
            Authentication authentication) {
        Coach coach = resolveCoach(authentication);
        PlanAssignmentResponse body =
                workoutPlanService.assignPlanToTrainee(planId, coach.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plan assigned", body));
    }

    private Coach resolveCoach(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
    }
}
