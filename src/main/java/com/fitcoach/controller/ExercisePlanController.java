package com.fitcoach.controller;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.entity.WorkoutPlan;
import com.fitcoach.dto.request.AssignPlanRequest;
import com.fitcoach.dto.request.CreateExercisePlanRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.ExercisePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/coaches/exercise-plans")
@RequiredArgsConstructor
public class ExercisePlanController {

    private final ExercisePlanService exercisePlanService;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutPlan>> createExercisePlan(
            @RequestBody CreateExercisePlanRequest request,
            Authentication authentication) {

        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can create exercise plans"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        WorkoutPlan plan = exercisePlanService.createExercisePlan(coach.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Exercise plan created successfully", plan));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutPlan>>> getMyPlans(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        List<WorkoutPlan> plans = exercisePlanService.getPlansByCoach(coach.getId());
        return ResponseEntity.ok(ApiResponse.ok("Exercise plans retrieved successfully", plans));
    }

    @PostMapping("/{planId}/assign")
    public ResponseEntity<ApiResponse<String>> assignPlanToTrainees(
            @PathVariable UUID planId,
            @RequestBody AssignPlanRequest request,
            Authentication authentication) {

        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can assign plans"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        try {
            exercisePlanService.assignPlanToTrainees(planId, coach.getId(), request.getTraineeIds());
            return ResponseEntity.ok(ApiResponse.ok("Plan assigned successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }
}
