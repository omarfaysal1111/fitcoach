package com.fitcoach.dto.request.plan;

import com.fitcoach.domain.entity.SectionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line in the bulk payload for
 * POST /api/v1/workouts/{workoutId}/exercises (persisted as {@link com.fitcoach.domain.entity.PlanSessionExercise}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutExerciseLineRequest {

    @NotNull
    private Long exerciseId;

    @NotNull
    private SectionType sectionType;

    @NotNull
    private Integer orderIndex;

    @NotNull
    private Integer sets;

    private String reps;

    private Double loadAmount;

    private Integer restSeconds;
}
