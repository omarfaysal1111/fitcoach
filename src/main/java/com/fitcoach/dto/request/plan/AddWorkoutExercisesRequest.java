package com.fitcoach.dto.request.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper for POST /api/v1/workouts/{workoutId}/exercises so the body can carry a named list
 * (alternative: use {@code @RequestBody List<WorkoutExerciseLineRequest>} in the controller).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWorkoutExercisesRequest {

    @NotEmpty
    @Valid
    private List<WorkoutExerciseLineRequest> exercises;
}
