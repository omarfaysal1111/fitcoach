package com.fitcoach.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CompleteWorkoutRequest {

    @NotNull
    @Valid
    private List<ExerciseLogItemRequest> exerciseLogs;
}
