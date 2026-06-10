package com.fitcoach.controller;

import com.fitcoach.dto.request.CompleteWorkoutRequest;
import com.fitcoach.dto.request.CreateMeasurementLogRequest;
import com.fitcoach.dto.request.CreateProgressPictureRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.MeasurementLogResponse;
import com.fitcoach.dto.response.ProgressPhotoResponse;
import com.fitcoach.dto.response.ProgressPictureResponse;
import com.fitcoach.service.ExerciseLogService;
import com.fitcoach.service.MeasurementService;
import com.fitcoach.service.ProgressPhotoService;
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
    private final ProgressPhotoService progressPhotoService;

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
     * Merges file-uploaded photos (progress_photos) and URL-based pictures (progress_pictures),
     * groups by date, and maps label→slot. Newest date first.
     */
    @GetMapping("/me/progress-pictures")
    public ResponseEntity<ApiResponse<List<ProgressPictureResponse>>> getMyProgressPictures(
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal.getUsername();

        // Group file-uploaded photos by date, mapping label → front/side/back slot
        java.util.Map<java.time.LocalDate, ProgressPictureResponse.ProgressPictureResponseBuilder> byDate =
                new java.util.LinkedHashMap<>();

        for (ProgressPhotoResponse p : progressPhotoService.getMyPhotos(email)) {
            java.time.LocalDate date = p.getPhotoDate() != null ? p.getPhotoDate() : java.time.LocalDate.now();
            var builder = byDate.computeIfAbsent(date, d ->
                    ProgressPictureResponse.builder().id(p.getId()).date(d).uploadedAt(p.getUploadedAt()));
            String label = p.getLabel() != null ? p.getLabel().toLowerCase() : "front";
            if (label.contains("side"))       builder.sidePictureUrl(p.getFileUrl());
            else if (label.contains("back"))  builder.backPictureUrl(p.getFileUrl());
            else                              builder.frontPictureUrl(p.getFileUrl());
        }

        // Merge URL-based pictures (progress_pictures table) — fill empty slots per date
        for (ProgressPictureResponse pic : measurementService.getMyProgressPictures(email)) {
            java.time.LocalDate date = pic.getDate() != null ? pic.getDate() : java.time.LocalDate.now();
            var builder = byDate.computeIfAbsent(date, d ->
                    ProgressPictureResponse.builder().id(pic.getId()).date(d).uploadedAt(pic.getUploadedAt()));
            if (pic.getFrontPictureUrl() != null) builder.frontPictureUrl(pic.getFrontPictureUrl());
            if (pic.getSidePictureUrl()  != null) builder.sidePictureUrl(pic.getSidePictureUrl());
            if (pic.getBackPictureUrl()  != null) builder.backPictureUrl(pic.getBackPictureUrl());
            if (pic.getNotes() != null)           builder.notes(pic.getNotes());
        }

        List<ProgressPictureResponse> result = byDate.values().stream()
                .map(ProgressPictureResponse.ProgressPictureResponseBuilder::build)
                .sorted(java.util.Comparator.comparing(ProgressPictureResponse::getDate).reversed())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
