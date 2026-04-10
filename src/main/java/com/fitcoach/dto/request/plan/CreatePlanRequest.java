package com.fitcoach.dto.request.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /api/v1/plans — create the master {@link com.fitcoach.domain.entity.WorkoutPlan}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long coachId;
}
