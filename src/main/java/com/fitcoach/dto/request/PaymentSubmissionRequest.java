package com.fitcoach.dto.request;

import com.fitcoach.domain.enums.PaymentMethod;
import com.fitcoach.domain.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentSubmissionRequest {

    @NotNull(message = "desiredPlan is required")
    private SubscriptionPlan desiredPlan;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "transferredAmount is required")
    @Positive(message = "transferredAmount must be positive")
    private BigDecimal transferredAmount;
}
