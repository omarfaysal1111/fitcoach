package com.fitcoach.controller;

import com.fitcoach.dto.request.InvitationRequest;
import com.fitcoach.dto.request.UpdateCoachRequest;
import com.fitcoach.dto.response.ApiResponse;
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
        return ResponseEntity.ok(ApiResponse.ok(coachService.getMyProfile(principal.getUsername())));
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
}
