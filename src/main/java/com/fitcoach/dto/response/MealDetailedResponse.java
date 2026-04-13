package com.fitcoach.dto.response;
import com.fitcoach.dto.response.NutritionPlanDetailedResponse;
import com.fitcoach.dto.response.MealDetailedResponse;
import com.fitcoach.dto.response.IngredientResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MealDetailedResponse {
    private String id;
    private String name;
    private Double calories;
    private Double protein;
    private Double carbs;
    private Double fat;
    private List<IngredientResponse> ingredients;
}