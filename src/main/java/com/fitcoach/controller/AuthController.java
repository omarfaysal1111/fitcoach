package com.fitcoach.controller;

import com.fitcoach.dto.request.AppleAuthRequest;
import com.fitcoach.dto.request.GoogleAuthRequest;
import com.fitcoach.dto.request.LoginRequest;
import com.fitcoach.dto.request.RegisterCoachRequest;
import com.fitcoach.dto.request.RegisterTraineeRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.AuthResponse;
import com.fitcoach.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/coaches/register
     * Public – allows a coach to self-register.
     */
    @PostMapping("/coaches/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCoach(
            @Valid @RequestBody RegisterCoachRequest request) {
        AuthResponse response = authService.registerCoach(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Coach registered successfully", response));
    }

    /**
     * POST /api/auth/trainees/register
     * Public – but requires a valid invitationToken in the request body.
     */
    @PostMapping("/trainees/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerTrainee(
            @Valid @RequestBody RegisterTraineeRequest request) {
        AuthResponse response = authService.registerTrainee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Trainee registered successfully", response));
    }

    /**
     * POST /api/auth/login
     * Public – works for both roles; role is included in the response.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /**
     * POST /api/auth/google
     * Public – verifies a Google ID Token and returns an app JWT.
     * Creates an account on first sign-in (coaches only; trainees also need invitationToken).
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.ok("Google sign-in successful", response));
    }

    /**
     * POST /api/auth/apple
     * Public – verifies an Apple identity token and returns an app JWT.
     * Creates an account on first sign-in (coaches only; trainees also need invitationToken).
     */
    @PostMapping("/apple")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithApple(
            @Valid @RequestBody AppleAuthRequest request) {
        AuthResponse response = authService.loginWithApple(request);
        return ResponseEntity.ok(ApiResponse.ok("Apple sign-in successful", response));
    }
}
