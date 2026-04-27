package com.fitcoach.controller;

import com.fitcoach.dto.request.CompleteWorkoutRequest;
import com.fitcoach.dto.request.CreateMeasurementLogRequest;
import com.fitcoach.dto.request.CreateProgressPictureRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.MeasurementLogResponse;
import com.fitcoach.dto.response.ProgressPictureResponse;
import com.fitcoach.service.ExerciseLogService;
import com.fitcoach.service.MeasurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trainees")
@RequiredArgsConstructor
public class TraineeProgressController {

    private final ExerciseLogService exerciseLogService;
    private final MeasurementService measurementService;

    /**
     * POST /api/trainees/me/plan-sessions/{planSessionId}/complete-with-logs
     * Complete a plan session with per-exercise logging. Prefer {@code setOutcomes}: one entry per
     * prescribed set (COMPLETED, SKIPPED, MISSED) with a reason for SKIPPED/MISSED. Legacy aggregate
     * fields ({@code actualSetsCompleted}, {@code skippedSets}, {@code excuse}) still supported.
     */
    @PostMapping("/me/plan-sessions/{planSessionId}/complete-with-logs")
    public ResponseEntity<ApiResponse<String>> completePlanSessionWithLogs(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID planSessionId,
            @Valid @RequestBody CompleteWorkoutRequest request) {
        exerciseLogService.completeWorkoutWithLogs(principal.getUsername(), planSessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plan session logged successfully"));
    }

    /**
     * POST /api/trainees/me/measurements
     * Record a new measurement / inbody entry.
     */
    @PostMapping("/me/measurements")
    public ResponseEntity<ApiResponse<MeasurementLogResponse>> addMeasurement(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateMeasurementLogRequest request) {
        MeasurementLogResponse response =
                measurementService.addMeasurement(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Measurement recorded", response));
    }

    /**
     * GET /api/trainees/me/measurements
     * Get all measurements for the authenticated trainee, newest first.
     */
    @GetMapping("/me/measurements")
    public ResponseEntity<ApiResponse<List<MeasurementLogResponse>>> getMyMeasurements(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(measurementService.getMyMeasurements(principal.getUsername())));
    }

    /**
     * POST /api/trainees/me/progress-pictures
     * Upload progress picture URLs.
     */
    @PostMapping(value = "/me/progress-pictures", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProgressPictureResponse>> addProgressPicture(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateProgressPictureRequest request) {
        ProgressPictureResponse response =
                measurementService.addProgressPicture(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Progress picture saved", response));
    }

    /**
     * GET /api/trainees/me/progress-pictures
     * Get all progress pictures for the authenticated trainee, newest first.
     */
    @GetMapping("/me/progress-pictures")
    public ResponseEntity<ApiResponse<List<ProgressPictureResponse>>> getMyProgressPictures(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(measurementService.getMyProgressPictures(principal.getUsername())));
    }
}
