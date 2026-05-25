package com.fitcoach.dto.request.plan;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /api/v1/plans — create the master {@link com.fitcoach.domain.entity.WorkoutPlan}.
 *
 * coachId is intentionally omitted — it is resolved server-side from the
 * authenticated JWT so the client never needs to know or send it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    @NotBlank
    private String title;

    private String description;
}
