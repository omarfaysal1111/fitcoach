package com.fitcoach.controller;

import com.fitcoach.dto.request.PaymentSubmissionRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.PaymentResponse;
import com.fitcoach.dto.response.SubscriptionStatusResponse;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.PaymentService;
import com.fitcoach.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/coaches/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;

    /**
     * POST /api/coaches/payment/submit
     *
     * Multipart request:
     *   - screenshot  : the transfer receipt image (Vodafone Cash / InstaPay)
     *   - desiredPlan : BASIC | PREMIUM | ELITE
     *   - paymentMethod : VODAFONE_CASH | INSTAPAY
     *   - transferredAmount : the amount the coach claims to have sent
     */
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaymentResponse>> submitPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("screenshot") MultipartFile screenshot,
            @RequestPart("data") @Valid PaymentSubmissionRequest request) {

        PaymentResponse response = paymentService.submitPayment(
                userDetails.getUsername(), screenshot, request);

        HttpStatus status = switch (response.getStatus()) {
            case APPROVED -> HttpStatus.OK;
            case REJECTED -> HttpStatus.PAYMENT_REQUIRED;
            default       -> HttpStatus.ACCEPTED;
        };

        return ResponseEntity.status(status)
                .body(ApiResponse.ok(response));
    }

    /**
     * GET /api/coaches/payment/subscription
     *
     * Returns the coach's current subscription status and client-slot usage.
     */
    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getSubscription(
            @AuthenticationPrincipal UserDetails userDetails) {

        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();
        var coach = coachRepository.findByUserId(user.getId())
                .orElseThrow();

        SubscriptionStatusResponse status = subscriptionService.getStatus(coach);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * GET /api/coaches/payment/history
     *
     * Returns a list of all payment submissions by this coach.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<PaymentResponse> history = paymentService.getPaymentHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
