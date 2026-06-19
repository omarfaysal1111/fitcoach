package com.fitcoach.controller;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.SubscriptionPlan;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.SubscriptionStatusResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final SubscriptionService subscriptionService;

    /**
     * Body: { "coachEmail": "...", "plan": "BASIC|PREMIUM|ELITE|TRIAL" }
     * Auth: Basic omarfaysaladmin@admin.co:x6-4C37M
     */
    @PostMapping("/coaches/set-plan")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> setCoachPlan(
            @RequestBody Map<String, String> body) {

        String coachEmail = body.get("coachEmail");
        String planStr    = body.get("plan");

        if (coachEmail == null || coachEmail.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("coachEmail is required"));
        }
        if (planStr == null || planStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("plan is required"));
        }

        SubscriptionPlan plan;
        try {
            plan = SubscriptionPlan.valueOf(planStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid plan. Valid values: TRIAL, BASIC, PREMIUM, ELITE"));
        }

        User user = userRepository.findByEmail(coachEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + coachEmail));

        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No coach profile found for: " + coachEmail));

        subscriptionService.activatePlan(coach, plan);
        SubscriptionStatusResponse status = subscriptionService.getStatus(coach);

        return ResponseEntity.ok(ApiResponse.ok(
                "Plan updated to " + plan.name() + " for " + coachEmail, status));
    }

}
