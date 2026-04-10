package com.fitcoach.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * Payload for adding a {@link com.fitcoach.domain.entity.PlanSession} (one plan day) to a workout plan.
 * JSON may still send {@code "name"} for the session title (e.g. Leg Day).
 */
@Data
public class CreatePlanSessionRequest {

    /** Incoming JSON may use {@code "name"} (legacy) or {@code "title"}. */
    @JsonAlias({"name", "title"})
    private String title;

    private String notes;

    private List<WorkoutExerciseItemRequest> items;

    /** Backward compatibility for older clients */
    private List<Long> exerciseIds;
}
