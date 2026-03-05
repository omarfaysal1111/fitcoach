package com.fitcoach.controller;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.User;
import com.fitcoach.dto.request.CreateNutritionPlanRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.NutritionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coaches/nutrition-plans")
@RequiredArgsConstructor
public class NutritionPlanController {

    private final NutritionPlanService nutritionPlanService;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<NutritionPlan>> createNutritionPlan(
            @RequestBody CreateNutritionPlanRequest request,
            Authentication authentication) {
        
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can create nutrition plans"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        
        NutritionPlan plan = nutritionPlanService.createNutritionPlan(coach.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Nutrition plan created successfully", plan));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NutritionPlan>>> getMyPlans(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        List<NutritionPlan> plans = nutritionPlanService.getPlansByCoach(coach.getId());
        return ResponseEntity.ok(ApiResponse.ok("Plans retrieved successfully", plans));
    }
}
