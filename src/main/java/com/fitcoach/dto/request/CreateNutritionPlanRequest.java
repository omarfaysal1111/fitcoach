package com.fitcoach.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateNutritionPlanRequest {
    private String title;
    private String description;
    private List<Long> traineeIds;
    private List<CreateMealRequest> meals;
}
