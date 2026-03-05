package com.fitcoach.controller;

import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.service.TraineeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trainees")
@RequiredArgsConstructor
public class TraineeController {

    private final TraineeService traineeService;

    /** GET /api/trainees/me – authenticated trainee's own profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(traineeService.getMyProfile(principal.getUsername())));
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
}
