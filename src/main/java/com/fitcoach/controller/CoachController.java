package com.fitcoach.controller;

import com.fitcoach.dto.request.InvitationRequest;
import com.fitcoach.dto.request.UpdateCoachRequest;
import com.fitcoach.dto.request.UpdateTraineeByCoachRequest;
import com.fitcoach.dto.request.UpdateTraineeNotesRequest;
import com.fitcoach.dto.response.CoachAlertResponse;
import com.fitcoach.dto.response.CoachTraineeDetailResponse;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachHomeResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.InvitationResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.service.CoachService;
import com.fitcoach.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;
    private final InvitationService invitationService;

    /** GET /api/coaches/me – authenticated coach's own profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CoachProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthenticated");
        }
        return ResponseEntity.ok(ApiResponse.ok(coachService.getMyProfile(principal.getUsername())));
    }

    /** GET /api/coaches/home – aggregated data for coach home dashboard */
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<CoachHomeResponse>> getHome(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthenticated");
        }
        CoachHomeResponse home = coachService.getHome(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(home));
    }

    /** PUT /api/coaches/me – update profile */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CoachProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateCoachRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated", coachService.updateMyProfile(principal.getUsername(), request)));
    }

    /** POST /api/coaches/invitations – generate invitation UUID for an email */
    @PostMapping("/invitations")
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody InvitationRequest request) {
        InvitationResponse response = invitationService.createInvitation(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invitation created", response));
    }

    /** GET /api/coaches/invitations – list all sent invitations */
    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getMyInvitations(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(invitationService.getInvitationsForCoach(principal.getUsername())));
    }

    /** GET /api/coaches/trainees – list enrolled trainees */
    @GetMapping("/trainees")
    public ResponseEntity<ApiResponse<List<TraineeProfileResponse>>> getMyTrainees(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(coachService.getMyTrainees(principal.getUsername())));
    }

    /** GET /api/coaches/trainees/{id} – profile plus workout completion history (per-set completed/skipped/missed). */
    @GetMapping("/trainees/{id}")
    public ResponseEntity<ApiResponse<CoachTraineeDetailResponse>> getTraineeDetails(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(coachService.getTraineeDetails(principal.getUsername(), id)));
    }

    /** PATCH /api/coaches/trainees/{id} – coach updates a trainee's goal and/or level */
    @PatchMapping("/trainees/{id}")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> updateTraineeProfile(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTraineeByCoachRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Trainee updated",
                coachService.updateTraineeByCoach(principal.getUsername(), id, request)));
    }

    /** PUT /api/coaches/trainees/{id}/notes – save coach feedback and caution notes for a trainee */
    @PutMapping("/trainees/{id}/notes")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> updateTraineeNotes(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody UpdateTraineeNotesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Notes saved",
                coachService.updateTraineeNotes(principal.getUsername(), id, request)));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<CoachAlertResponse>>> getAlerts(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(coachService.getAlerts(principal.getUsername())));
    }

    /** PATCH /api/coaches/trainees/{id}/status – archive a trainee (body: { "status": "ARCHIVED" }) */
    @PatchMapping("/trainees/{id}/status")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> updateTraineeStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String status = body.getOrDefault("status", "");
        if ("ARCHIVED".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(ApiResponse.ok("Trainee archived",
                    coachService.archiveTrainee(principal.getUsername(), id)));
        }
        throw new IllegalArgumentException("Unsupported status: " + status);
    }

    /** DELETE /api/coaches/trainees/{id} – soft-delete a trainee */
    @DeleteMapping("/trainees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrainee(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        coachService.deleteTrainee(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Trainee deleted", null));
    }
}
