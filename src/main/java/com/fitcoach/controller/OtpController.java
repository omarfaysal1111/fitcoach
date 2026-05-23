package com.fitcoach.controller;

import com.fitcoach.dto.request.SendOtpRequest;
import com.fitcoach.dto.request.VerifyOtpRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public OTP endpoints – no JWT required.
 *
 * POST /api/v1/auth/otp/send    – request a 6-digit code
 * POST /api/v1/auth/otp/verify  – validate the code
 */
@RestController
@RequestMapping("/v1/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    /**
     * Send a one-time password to the provided email address.
     * The code is valid for 5 minutes.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        otpService.sendOtp(request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.ok("OTP sent successfully. Check your email.", null));
    }

    /**
     * Verify the OTP code submitted by the user.
     * Returns 200 on success, 400 on invalid/expired code.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        boolean valid = otpService.verifyOtp(request.getEmail(), request.getCode());
        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired OTP code."));
        }
        return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully.", null));
    }
}
