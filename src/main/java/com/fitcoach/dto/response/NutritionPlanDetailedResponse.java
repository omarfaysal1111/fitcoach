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

@Data
@Builder
public class IngredientResponse {
    private String id;
    private String name; 
    // Add any other specific ingredient fields here (e.g., portion size, unit)
}