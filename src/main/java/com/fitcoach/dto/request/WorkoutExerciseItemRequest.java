package com.fitcoach.dto.request;

import lombok.Data;

/**
 * One prescribed exercise line when building a {@link com.fitcoach.domain.entity.PlanSession}
 * (not a wger catalog row by itself—the {@link #exerciseId} points at the catalog).
 */
@Data
public class WorkoutExerciseItemRequest {
    private Long exerciseId;
    /** Optional: WARM_UP, MAIN, COOL_DOWN */
    private String sectionType;
    private int order;
    private int sets;
    private String reps;
    private String load;
    private String rest;
}

