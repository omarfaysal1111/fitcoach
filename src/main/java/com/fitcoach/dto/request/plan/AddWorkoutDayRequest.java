package com.fitcoach.dto.request.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /api/v1/plans/{planId}/workouts — add one day/session to a plan
 * (persisted as {@link com.fitcoach.domain.entity.PlanSession}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWorkoutDayRequest {

    @NotBlank
    private String title;

    @NotNull
    private Integer dayOrder;
}
