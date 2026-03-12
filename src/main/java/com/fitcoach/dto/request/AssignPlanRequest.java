package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPlanRequest {

    @NotEmpty(message = "Trainee IDs cannot be empty")
    private List<Long> traineeIds;
}
