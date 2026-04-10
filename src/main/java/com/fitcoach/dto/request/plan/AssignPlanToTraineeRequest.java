package com.fitcoach.dto.request.plan;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * POST /api/v1/plans/{planId}/assign — one trainee and start date.
 * Distinct from bulk {@link com.fitcoach.dto.request.AssignPlanRequest} (multiple trainee IDs, no start date).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPlanToTraineeRequest {

    @NotNull
    private Long traineeId;

    @NotNull
    private LocalDate startDate;
}
