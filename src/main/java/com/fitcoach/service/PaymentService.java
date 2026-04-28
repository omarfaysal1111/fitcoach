package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.CoachSubscription;
import com.fitcoach.domain.entity.PaymentRecord;
import com.fitcoach.domain.enums.PaymentMethod;
import com.fitcoach.domain.enums.PaymentStatus;
import com.fitcoach.domain.enums.SubscriptionPlan;
import com.fitcoach.dto.request.PaymentSubmissionRequest;
import com.fitcoach.dto.response.PaymentResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.exception.SubscriptionException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.PaymentRecordRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final BigDecimal TOLERANCE_EGP = new BigDecimal("5");

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OcrService ocrService;
    private final SubscriptionService subscriptionService;
    private final FileStorageService fileStorageService;

    // ── Submit Payment ────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse submitPayment(String coachEmail,
                                         MultipartFile screenshot,
                                         PaymentSubmissionRequest request) {

        Coach coach = resolveCoach(coachEmail);
        SubscriptionPlan desiredPlan = request.getDesiredPlan();

        if (desiredPlan == SubscriptionPlan.TRIAL) {
            throw new SubscriptionException("The trial plan cannot be purchased.");
        }

        BigDecimal expectedPrice = desiredPlan.getPriceEgp();
        BigDecimal claimedAmount  = request.getTransferredAmount();

        // Claimed amount must be at least the plan price (within tolerance)
        if (claimedAmount.compareTo(expectedPrice.subtract(TOLERANCE_EGP)) < 0) {
            throw new SubscriptionException(String.format(
                    "Claimed amount %.2f EGP is less than the required %.2f EGP for the %s plan.",
                    claimedAmount, expectedPrice, desiredPlan.name()));
        }

        // Persist screenshot first — OCR uses transferTo / streams and must not run before store,
        // or the multipart payload is exhausted and Files.copy in store fails on Tomcat's .tmp.
        String screenshotPath = null;
        try {
            screenshotPath = fileStorageService.store(screenshot, "payment_receipts");
        } catch (Exception e) {
            log.warn("Could not save payment screenshot: {}", e.getMessage());
        }

        BigDecimal ocrAmount = screenshotPath != null
                ? ocrService.extractAmount(fileStorageService.resolveStoredPath(screenshotPath))
                : null;
        log.info("Payment submitted: coach={}, plan={}, claimed={}, ocr={}",
                coachEmail, desiredPlan, claimedAmount, ocrAmount);

        // Verify OCR amount
        boolean ocrVerified = isOcrAmountSufficient(ocrAmount, expectedPrice);
        PaymentStatus paymentStatus;
        String reviewNote;

        if (ocrVerified) {
            paymentStatus = PaymentStatus.APPROVED;
            reviewNote = "OCR verification passed.";
        } else {
            paymentStatus = PaymentStatus.REJECTED;
            reviewNote = ocrAmount != null
                    ? String.format("OCR extracted %.2f EGP but expected %.2f EGP for %s plan.",
                            ocrAmount, expectedPrice, desiredPlan.name())
                    : "OCR could not extract a valid amount from the screenshot.";
        }

        PaymentRecord record = PaymentRecord.builder()
                .coach(coach)
                .desiredPlan(desiredPlan)
                .claimedAmount(claimedAmount)
                .ocrExtractedAmount(ocrAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(paymentStatus)
                .screenshotPath(screenshotPath)
                .reviewNote(reviewNote)
                .build();

        paymentRecordRepository.save(record);

        if (paymentStatus == PaymentStatus.APPROVED) {
            CoachSubscription activated = subscriptionService.activatePlan(coach, desiredPlan);
            log.info("Plan activated: coach={}, plan={}, until={}", coachEmail, desiredPlan, activated.getEndDate());
        }

        return toResponse(record);
    }

    // ── Payment History ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(String coachEmail) {
        Coach coach = resolveCoach(coachEmail);
        return paymentRecordRepository.findAllByCoachIdOrderByCreatedAtDesc(coach.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isOcrAmountSufficient(BigDecimal ocrAmount, BigDecimal expectedPrice) {
        if (ocrAmount == null) return false;
        return ocrAmount.compareTo(expectedPrice.subtract(TOLERANCE_EGP)) >= 0;
    }

    private Coach resolveCoach(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
    }

    private PaymentResponse toResponse(PaymentRecord r) {
        return PaymentResponse.builder()
                .paymentId(r.getId())
                .desiredPlan(r.getDesiredPlan())
                .paymentMethod(r.getPaymentMethod())
                .claimedAmount(r.getClaimedAmount())
                .ocrExtractedAmount(r.getOcrExtractedAmount())
                .status(r.getStatus())
                .reviewNote(r.getReviewNote())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
