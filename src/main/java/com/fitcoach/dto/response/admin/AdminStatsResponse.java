package com.fitcoach.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * High-level platform stats shown on the admin dashboard home screen.
 */
@Data
@Builder
public class AdminStatsResponse {
    private long totalCoaches;
    private long totalTrainees;
    private long activeTrainees;
    private long totalPayments;
    private long pendingPayments;
    private long approvedPayments;
    private long rejectedPayments;
    private BigDecimal totalApprovedRevenue;
}
