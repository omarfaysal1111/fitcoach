package com.fitcoach.controller;

import com.fitcoach.dto.request.ReviewExerciseLogRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.MeasurementLogResponse;
import com.fitcoach.dto.response.ProgressPictureResponse;
import com.fitcoach.dto.response.WorkoutLogResponse;
import com.fitcoach.service.ExerciseLogService;
import com.fitcoach.service.MeasurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachTraineeProgressController {

    private final ExerciseLogService exerciseLogService;
    private final MeasurementService measurementService;

    /**
     * GET /api/coaches/trainees/{traineeId}/workout-logs
     * View all workout logs and exercise-level excuses for a trainee.
     */
    @GetMapping("/trainees/{traineeId}/workout-logs")
    public ResponseEntity<ApiResponse<List<WorkoutLogResponse>>> getTraineeWorkoutLogs(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long traineeId) {
        List<WorkoutLogResponse> logs =
                exerciseLogService.getWorkoutLogsForTrainee(principal.getUsername(), traineeId);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    /**
     * POST /api/coaches/workout-logs/{logId}/review
     * Review a specific exercise log and add coach notes.
     */
    @PostMapping("/workout-logs/{logId}/review")
    public ResponseEntity<ApiResponse<WorkoutLogResponse.ExerciseLogItem>> reviewExerciseLog(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long logId,
            @RequestBody ReviewExerciseLogRequest request) {
        WorkoutLogResponse.ExerciseLogItem result =
                exerciseLogService.reviewExerciseLog(principal.getUsername(), logId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review submitted", result));
    }

    /**
     * GET /api/coaches/trainees/{traineeId}/measurements
     * View all measurement logs for a specific trainee.
     */
    @GetMapping("/trainees/{traineeId}/measurements")
    public ResponseEntity<ApiResponse<List<MeasurementLogResponse>>> getTraineeMeasurements(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long traineeId) {
        return ResponseEntity.ok(
                ApiResponse.ok(measurementService.getTraineeMeasurements(
                        principal.getUsername(), traineeId)));
    }

    /**
     * GET /api/coaches/trainees/{traineeId}/progress-pictures
     * View all progress pictures for a specific trainee.
     */
    @GetMapping("/trainees/{traineeId}/progress-pictures")
    public ResponseEntity<ApiResponse<List<ProgressPictureResponse>>> getTraineeProgressPictures(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long traineeId) {
        return ResponseEntity.ok(
                ApiResponse.ok(measurementService.getTraineeProgressPictures(
                        principal.getUsername(), traineeId)));
    }
}
