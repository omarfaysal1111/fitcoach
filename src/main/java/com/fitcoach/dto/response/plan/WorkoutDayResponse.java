package com.fitcoach.dto.response.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One plan day/session (API path: /workouts/{workoutId}; entity: {@link com.fitcoach.domain.entity.PlanSession}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutDayResponse {

    private UUID id;
    private UUID planId;
    private String title;
    private Integer dayOrder;
}
