package com.fitcoach.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTraineeRequest {

    @Size(max = 80, message = "Full name must be at most 80 characters")
    private String fullName;

    @Size(max = 30, message = "Fitness goal must be at most 30 characters")
    private String fitnessGoal;
}
