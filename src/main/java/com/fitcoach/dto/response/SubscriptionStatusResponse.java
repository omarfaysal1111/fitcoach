package com.fitcoach.dto.response;

import com.fitcoach.domain.enums.SubscriptionPlan;
import com.fitcoach.domain.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SubscriptionStatusResponse {
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private int maxClients;
    private int currentClientCount;
    private int remainingSlots;
    private boolean unlimited;
}
