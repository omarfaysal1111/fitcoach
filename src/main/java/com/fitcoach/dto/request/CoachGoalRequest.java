package com.fitcoach.dto.request;

import com.fitcoach.domain.enums.GoalStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CoachGoalRequest {

    @NotBlank
    private String title;

    private String description;

    private GoalStatus status;

    private LocalDate targetDate;
}
