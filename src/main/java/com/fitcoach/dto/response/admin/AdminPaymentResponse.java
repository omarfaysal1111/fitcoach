package com.fitcoach.dto.response.admin;

import com.fitcoach.domain.enums.PaymentMethod;
import com.fitcoach.domain.enums.PaymentStatus;
import com.fitcoach.domain.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment record enriched with coach identity – used on the admin
 * "All Payments" screen.
 */
@Data
@Builder
public class AdminPaymentResponse {
    private Long paymentId;
    private Long coachId;
    private String coachName;
    private String coachEmail;
    private SubscriptionPlan desiredPlan;
    private PaymentMethod paymentMethod;
    private BigDecimal claimedAmount;
    private BigDecimal ocrExtractedAmount;
    private PaymentStatus status;
    private String reviewNote;
    private String screenshotPath;
    private LocalDateTime submittedAt;
}
