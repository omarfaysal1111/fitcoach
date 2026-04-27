package com.fitcoach.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTraineeByCoachRequest {

    @Size(max = 100, message = "Fitness goal must be at most 100 characters")
    private String fitnessGoal;

    @Size(max = 30, message = "Trainee level must be at most 30 characters")
    private String traineeLevel;
}
