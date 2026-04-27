package com.fitcoach.dto.response;

import com.fitcoach.domain.enums.PaymentMethod;
import com.fitcoach.domain.enums.PaymentStatus;
import com.fitcoach.domain.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private SubscriptionPlan desiredPlan;
    private PaymentMethod paymentMethod;
    private BigDecimal claimedAmount;
    private BigDecimal ocrExtractedAmount;
    private PaymentStatus status;
    private String reviewNote;
    private LocalDateTime createdAt;
}
