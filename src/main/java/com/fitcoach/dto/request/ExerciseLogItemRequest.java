package com.fitcoach.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ExerciseLogItemRequest {

    @NotNull
    @JsonAlias("workoutExerciseId")
    private UUID planSessionExerciseId;

    /**
     * Preferred: one entry per prescribed set, in order (index 0 = set 1).
     * When non-empty, per-set outcomes and reasons are stored; {@link #actualSetsCompleted} / {@link #skippedSets} are derived.
     */
    @Valid
    private List<ExerciseSetLogRequest> setOutcomes;

    /** Legacy aggregate logging when {@link #setOutcomes} is null or empty. */
    @Min(0)
    private int actualSetsCompleted;

    /** Legacy: intentionally skipped sets; use {@link #setOutcomes} with SKIPPED for per-set reasons. */
    @Min(0)
    private int skippedSets;

    /** Legacy: required when skippedSets &gt; 0 */
    private String excuse;
}
