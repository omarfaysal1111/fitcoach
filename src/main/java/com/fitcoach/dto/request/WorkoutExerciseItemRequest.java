package com.fitcoach.dto.request;

import lombok.Data;

@Data
public class WorkoutExerciseItemRequest {
    private Long exerciseId;
    private int order;
    private int sets;
    private String reps;
    private String load;
    private String rest;
}

