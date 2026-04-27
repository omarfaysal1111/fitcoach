package com.fitcoach.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fitcoach.domain.enums.SetLogOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExerciseSetLogRequest {

    @NotNull
    private SetLogOutcome outcome;

    /**
     * Required when outcome is SKIPPED or MISSED (what happened on that set).
     * Optional when COMPLETED.
     */
    private String reason;

    /**
     * Weight lifted for this set in kilograms. Optional — null means bodyweight or not recorded.
     * Accepts both "weightKg" and "weight" from client payloads.
     */
    @JsonAlias("weight")
    private Double weightKg;

    /** Actual reps performed for this set. Optional — null when not recorded. */
    private Integer reps;
}
