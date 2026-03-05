package com.fitcoach.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateWorkoutRequest {
    private String name;
    private String notes;
    private List<Long> exerciseIds;
}
