package com.fitcoach.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CompleteWorkoutRequest {

    @NotNull
    @NotEmpty
    @Valid
    private List<ExerciseLogItemRequest> exerciseLogs;
}
