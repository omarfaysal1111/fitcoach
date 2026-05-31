package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExerciseResponse {
    private Long id;
    private String name;
    private String description;
    private String videoLink;
    private String targetedMuscle;
}
