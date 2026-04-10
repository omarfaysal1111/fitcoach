package com.fitcoach.dto.response.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for created/loaded {@link com.fitcoach.domain.entity.WorkoutPlan} (API: master plan).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutPlanResponse {

    private UUID id;
    private Long coachId;
    private String title;
    private String description;
    private LocalDateTime createdAt;
}
