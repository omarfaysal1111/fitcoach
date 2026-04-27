package com.fitcoach.controller;

import com.fitcoach.dto.request.CoachGoalRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachGoalResponse;
import com.fitcoach.service.CoachGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachGoalController {

    private final CoachGoalService coachGoalService;

    /** GET /api/coaches/trainees/{id}/goals – list all goals set for a trainee */
    @GetMapping("/trainees/{id}/goals")
    public ResponseEntity<ApiResponse<List<CoachGoalResponse>>> getGoals(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(coachGoalService.getGoals(principal.getUsername(), id)));
    }

    /** POST /api/coaches/trainees/{id}/goals – create a new goal for a trainee */
    @PostMapping("/trainees/{id}/goals")
    public ResponseEntity<ApiResponse<CoachGoalResponse>> createGoal(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody CoachGoalRequest request) {
        CoachGoalResponse response = coachGoalService.createGoal(principal.getUsername(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Goal created", response));
    }

    /** PATCH /api/coaches/goals/{goalId} – update an existing goal */
    @PatchMapping("/goals/{goalId}")
    public ResponseEntity<ApiResponse<CoachGoalResponse>> updateGoal(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long goalId,
            @RequestBody CoachGoalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Goal updated",
                coachGoalService.updateGoal(principal.getUsername(), goalId, request)));
    }

    /** DELETE /api/coaches/goals/{goalId} – delete a goal */
    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long goalId) {
        coachGoalService.deleteGoal(principal.getUsername(), goalId);
        return ResponseEntity.ok(ApiResponse.ok("Goal deleted", null));
    }
}
