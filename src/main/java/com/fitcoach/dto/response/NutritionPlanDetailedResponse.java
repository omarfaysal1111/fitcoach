package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NutritionPlanDetailedResponse {
    private String id;
    private String title;
    private String description;
    private String type;
    private List<MealDetailedResponse> meals;
}