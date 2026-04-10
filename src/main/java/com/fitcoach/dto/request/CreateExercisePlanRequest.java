package com.fitcoach.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class CreateExercisePlanRequest {
    private String title;
    private String description;
    private List<Long> traineeIds;

    /** Plan days / sessions; {@code "workouts"} kept as JSON alias for older clients. */
    @JsonAlias("workouts")
    private List<CreatePlanSessionRequest> sessions;
}
